package `in`.grayscales.entangl

import `in`.grayscales.entangl.data.network.NetworkQuality
import org.junit.Assert.assertEquals
import org.junit.Test

class NetworkQualityTest {

    @Test
    fun testOfflineLevelZero() {
        val quality = NetworkQuality.offline()
        assertEquals(0, quality.level)
        assertEquals("OFFLINE", quality.label)
    }

    @Test
    fun testLevelCountsInternetListenerRelay() {
        assertEquals(
            1,
            NetworkQuality(hasInternet = true, listenerMode = "IDLE").level
        )
        assertEquals(
            2,
            NetworkQuality(hasInternet = true, listenerMode = "STREAMING").level
        )
        assertEquals(
            3,
            NetworkQuality(hasInternet = true, listenerMode = "STREAMING", relayReachable = true).level
        )
    }

    @Test
    fun testNoInternetCapsLevel() {
        // Relay/old listener state must never light bars without internet.
        assertEquals(
            0,
            NetworkQuality(hasInternet = false, listenerMode = "STREAMING", relayReachable = true).level
        )
    }

    @Test
    fun testLabels() {
        assertEquals("OFFLINE", NetworkQuality(hasInternet = false).label)
        assertEquals("ONLINE", NetworkQuality(hasInternet = true, listenerMode = "IDLE").label)
        assertEquals("LINKING", NetworkQuality(hasInternet = true, listenerMode = "STREAMING").label)
        assertEquals(
            "RELAY LINKED",
            NetworkQuality(hasInternet = true, listenerMode = "STREAMING", relayReachable = true).label
        )
    }
}
