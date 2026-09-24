package `in`.grayscales.entangl.domain.model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.cbor.Cbor
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Optical QR payload scanned by the New Device to establish a direct, encrypted local P2P link.
 */
@OptIn(ExperimentalSerializationApi::class, ExperimentalEncodingApi::class)
@Serializable
data class TransferQrPayload(
    val ip: String,
    val port: Int,
    val ephPub: String,
    val authToken: String,
    val senderUid: String
) {
    fun toCbor(): ByteArray = Cbor.encodeToByteArray(serializer(), this)

    fun toQrString(): String = "entangl-transfer://" + Base64.UrlSafe.encode(toCbor())

    companion object {
        fun fromQrString(qrText: String): TransferQrPayload? {
            return try {
                val clean = qrText.trim().removePrefix("entangl-transfer://")
                val bytes = try {
                    Base64.UrlSafe.decode(clean)
                } catch (_: Exception) {
                    Base64.decode(clean)
                }
                Cbor.decodeFromByteArray(serializer(), bytes)
            } catch (_: Exception) {
                null
            }
        }
    }
}
