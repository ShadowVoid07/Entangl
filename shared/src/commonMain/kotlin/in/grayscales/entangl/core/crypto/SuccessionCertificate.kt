package `in`.grayscales.entangl.core.crypto

import `in`.grayscales.entangl.core.util.SecureRandom
import `in`.grayscales.entangl.core.util.currentTimeMillis
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.cbor.Cbor
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * A cryptographic proof that an old identity key has securely delegated its authority
 * to a new identity key. This allows offline contacts to seamlessly transition trust
 * to the new device when they come back online.
 */
@OptIn(ExperimentalSerializationApi::class, ExperimentalEncodingApi::class)
@Serializable
data class SuccessionCertificate(
    val oldIdentityPubKey: String,
    val newIdentityPubKey: String,
    val timestampMs: Long,
    val nonce: String,
    val signature: String // Signature of (oldPubKey + newPubKey + timestamp + nonce) signed by oldPrivateKey
) {
    companion object {
        fun create(
            oldIdentityPubKey: ByteArray,
            newIdentityPubKey: ByteArray,
            keyPairGenerator: KeyPairGenerator
        ): SuccessionCertificate {
            val timestamp = currentTimeMillis()
            val nonceBytes = SecureRandom.nextBytes(16)
            
            val payloadToSign = oldIdentityPubKey + newIdentityPubKey + 
                                timestamp.toString().encodeToByteArray() + nonceBytes
                                
            val signatureBytes = keyPairGenerator.sign(payloadToSign)
            
            return SuccessionCertificate(
                oldIdentityPubKey = Base64.encode(oldIdentityPubKey),
                newIdentityPubKey = Base64.encode(newIdentityPubKey),
                timestampMs = timestamp,
                nonce = Base64.encode(nonceBytes),
                signature = Base64.encode(signatureBytes)
            )
        }

        fun verify(certificate: SuccessionCertificate, keyPairGenerator: KeyPairGenerator): Boolean {
            try {
                val oldPub = Base64.decode(certificate.oldIdentityPubKey)
                val newPub = Base64.decode(certificate.newIdentityPubKey)
                val nonce = Base64.decode(certificate.nonce)
                val sig = Base64.decode(certificate.signature)
                
                val payload = oldPub + newPub + certificate.timestampMs.toString().encodeToByteArray() + nonce
                
                return keyPairGenerator.verify(oldPub, payload, sig)
            } catch (e: Exception) {
                return false
            }
        }
        
        fun toByteArray(cert: SuccessionCertificate): ByteArray {
            return Cbor.encodeToByteArray(serializer(), cert)
        }
        
        fun fromByteArray(bytes: ByteArray): SuccessionCertificate? {
            return try {
                Cbor.decodeFromByteArray(serializer(), bytes)
            } catch (e: Exception) {
                null
            }
        }

        fun toEncodedString(cert: SuccessionCertificate): String {
            return Base64.encode(toByteArray(cert))
        }

        fun fromEncodedString(str: String): SuccessionCertificate? {
            return try {
                fromByteArray(Base64.decode(str))
            } catch (e: Exception) {
                null
            }
        }
    }
}
