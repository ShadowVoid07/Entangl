package `in`.grayscales.entangl.data.network

import `in`.grayscales.entangl.data.network.NetworkQuality.Companion.RELAY_FRESH_MS


/**
 * Live connectivity snapshot driving the Settings header indicators.
 * Every field is measured locally — no self-reported or simulated values:
 * internet presence from ConnectivityManager, listener state from
 * [NetworkTransport], relay reachability from its last confirmed contact.
 */
data class NetworkQuality(
    /** Validated upstream internet on the active network. Drives the green dot. */
    val hasInternet: Boolean = false,
    /** Human transport label, e.g. WIFI / CELLULAR / ETHERNET / OFFLINE. */
    val transportName: String = "OFFLINE",
    /** Currently selected relay host, empty when unknown. */
    val relayHost: String = "",
    /** A relay confirmed contact within [RELAY_FRESH_MS]. */
    val relayReachable: Boolean = false,
    /** Listener mode: STREAMING (foreground), POLLING (background), IDLE. */
    val listenerMode: String = "IDLE",
    /** Age of last confirmed relay contact, seconds. Null when never. */
    val lastRelaySuccessAgeSec: Long? = null
) {
    /** 0..3 bars: internet + listener + confirmed relay. Zero without internet. */
    val level: Int
        get() = if (!hasInternet) {
            0
        } else {
            (1 + (if (listenerMode != "IDLE") 1 else 0) + (if (relayReachable) 1 else 0))
                .coerceIn(0, 3)
        }

    val label: String
        get() = when {
            !hasInternet -> "OFFLINE"
            !relayReachable -> if (listenerMode == "IDLE") "ONLINE" else "LINKING"
            else -> "RELAY LINKED"
        }

    companion object {
        const val RELAY_FRESH_MS = 5 * 60 * 1000L

        fun offline(): NetworkQuality = NetworkQuality()
    }
}
