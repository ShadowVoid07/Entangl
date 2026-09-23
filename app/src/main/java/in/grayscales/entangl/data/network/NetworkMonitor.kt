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
import kotlinx.coroutines.launch

class NetworkMonitor(
    private val context: Context,
    private val networkTransport: NetworkTransport,
    private val nodeIdentityManager: NodeIdentityManager,
    private val messageRepository: MessageRepositoryImpl
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            Log.i("NetworkMonitor", "Network is available, restarting transport listeners and retrying messages")
            val localUid = nodeIdentityManager.localUid
            if (localUid.isNotBlank()) {
                networkTransport.restartListening(localUid)
            }
            scope.launch {
                try {
                    messageRepository.retryFailedMessages()
                } catch (e: Exception) {
                    Log.e("NetworkMonitor", "Failed to retry messages: ${e.message}")
                }
            }
        }

        override fun onLost(network: Network) {
            super.onLost(network)
            Log.i("NetworkMonitor", "Network lost, stopping transport listeners")
            networkTransport.stopListening()
        }
    }

    fun startMonitoring() {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, networkCallback)
    }

    fun stopMonitoring() {
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (e: Exception) {
            Log.e("NetworkMonitor", "Error unregistering callback: ${e.message}")
        }
    }
}
