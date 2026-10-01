package `in`.grayscales.entangl.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import `in`.grayscales.entangl.core.identity.NodeIdentityManager
import `in`.grayscales.entangl.data.repository.MessageRepositoryImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class NetworkMonitor(
    private val context: Context,
    private val networkTransport: NetworkTransport,
    private val nodeIdentityManager: NodeIdentityManager,
    private val messageRepository: MessageRepositoryImpl
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _quality = MutableStateFlow(NetworkQuality.offline())
    /** Live connectivity for header indicators. Emits only on real change. */
    val quality: StateFlow<NetworkQuality> = _quality.asStateFlow()

    @Volatile
    private var decayStarted = false

    private fun refreshQuality() {
        val caps = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        val hasInternet = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        val transportName = when {
            caps == null -> "OFFLINE"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WIFI"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "CELLULAR"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ETHERNET"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
            else -> "LINK"
        }
        val relayHost = try {
            java.net.URL(networkTransport.activeRelay).host.ifBlank { networkTransport.activeRelay }
        } catch (_: Exception) {
            networkTransport.activeRelay
        }
        val lastSuccess = networkTransport.lastRelaySuccessAt
        val now = System.currentTimeMillis()
        _quality.value = NetworkQuality(
            hasInternet = hasInternet,
            transportName = if (hasInternet) transportName else "OFFLINE",
            relayHost = relayHost,
            relayReachable = lastSuccess > 0L && (now - lastSuccess) <= NetworkQuality.RELAY_FRESH_MS,
            listenerMode = when {
                !networkTransport.isRunning -> "IDLE"
                networkTransport.isForeground -> "STREAMING"
                else -> "POLLING"
            },
            lastRelaySuccessAgeSec = if (lastSuccess > 0L) (now - lastSuccess) / 1000L else null
        )
    }

    /** Force relay re-listen (settings Renew action). */
    fun restartTransport() {
        val localUid = nodeIdentityManager.localUid
        if (localUid.isNotBlank()) {
            networkTransport.restartListening(localUid)
        }
        refreshQuality()
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            Log.i("NetworkMonitor", "Network is available (callback)")
            val localUid = nodeIdentityManager.localUid
            if (localUid.isNotBlank() && !networkTransport.isRunning) {
                Log.i("NetworkMonitor", "Network transport is idle; starting listener for $localUid")
                networkTransport.startListening(localUid)
            }
            refreshQuality()
            scope.launch {
                try {
                    messageRepository.retryFailedMessages()
                } catch (e: Exception) {
                    Log.e("NetworkMonitor", "Failed to retry messages: ${e.message}")
                }
                refreshQuality()
            }
        }

        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            super.onCapabilitiesChanged(network, capabilities)
            refreshQuality()
        }

        override fun onLost(network: Network) {
            super.onLost(network)
            Log.i("NetworkMonitor", "Network lost, stopping transport listeners")
            networkTransport.stopListening()
            refreshQuality()
        }
    }

    fun startMonitoring() {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, networkCallback)
        refreshQuality()
        // Decay loop: relay freshness ages out so bars fall honestly when quiet.
        if (!decayStarted) {
            decayStarted = true
            scope.launch {
                while (isActive) {
                    delay(30_000L)
                    refreshQuality()
                }
            }
        }
    }

    fun stopMonitoring() {
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (e: Exception) {
            Log.e("NetworkMonitor", "Error unregistering callback: ${e.message}")
        }
    }
}
