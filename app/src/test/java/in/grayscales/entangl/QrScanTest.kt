package `in`.grayscales.entangl

import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import com.google.zxing.qrcode.QRCodeWriter
import org.junit.Assert.assertNotNull
import org.junit.Test

class QrScanTest {

    @Test
    fun testBlackOnWhiteQrScanning() {
        val testContent = "entangl-test-payload-12345"
        val size = 512
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(testContent, BarcodeFormat.QR_CODE, size, size)

        // Case A: Black modules on white background
        val pixelsNormal = IntArray(size * size)
        for (y in 0 until size) {
            for (x in 0 until size) {
                pixelsNormal[y * size + x] = if (bitMatrix.get(x, y)) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
            }
        }

        val sourceNormal = RGBLuminanceSource(size, size, pixelsNormal)
        val bitmapNormal = BinaryBitmap(HybridBinarizer(sourceNormal))
        val readerNormal = QRCodeReader()
        val resultNormal = readerNormal.decode(bitmapNormal)
        println("Normal (Black on White) result: ${resultNormal.text}")
        assertNotNull(resultNormal)

        // Case B: White modules on black background (inverted)
        val pixelsInverted = IntArray(size * size)
        for (y in 0 until size) {
            for (x in 0 until size) {
                pixelsInverted[y * size + x] = if (bitMatrix.get(x, y)) 0xFFFFFFFF.toInt() else 0xFF000000.toInt()
            }
        }

        val sourceInverted = RGBLuminanceSource(size, size, pixelsInverted)
        val bitmapInverted = BinaryBitmap(HybridBinarizer(sourceInverted))
        val readerInverted = QRCodeReader()
        try {
            val resultInverted = readerInverted.decode(bitmapInverted)
            println("Inverted result: ${resultInverted.text}")
        } catch (e: Exception) {
            println("Inverted (White on Black) FAILED as expected: ${e.message}")
        }

        // Case C: Inverted decoded with .invert()
        val bitmapInvertedFlipped = BinaryBitmap(HybridBinarizer(sourceInverted.invert()))
        val resultInvertedFlipped = readerInverted.decode(bitmapInvertedFlipped)
        println("Inverted with .invert() SUCCESS: ${resultInvertedFlipped.text}")
        assertNotNull(resultInvertedFlipped)
    }

    @Test
    fun testRotatedQrDecoding() {
        val testContent = "entangl-test-rotated-payload"
        val size = 512
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(testContent, BarcodeFormat.QR_CODE, size, size)

        // Rotate 90 degrees clockwise
        val rotatedPixels = IntArray(size * size)
        for (y in 0 until size) {
            for (x in 0 until size) {
                // (x, y) rotated 90 deg clockwise -> (size - 1 - y, x)
                val newX = size - 1 - y
                val newY = x
                rotatedPixels[newY * size + newX] = if (bitMatrix.get(x, y)) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
            }
        }

        val sourceRotated = RGBLuminanceSource(size, size, rotatedPixels)
        val bitmapRotated = BinaryBitmap(HybridBinarizer(sourceRotated))
        val reader = QRCodeReader()
        val resultRotated = reader.decode(bitmapRotated)
        println("Rotated 90 deg result: ${resultRotated.text}")
        assertNotNull(resultRotated)
    }

    @Test
    fun testHandshakePayloadQrEndToEnd() {
        val payload = `in`.grayscales.entangl.domain.model.HandshakePayload(
            v = 1,
            action = "INITIATE",
            uid = "node-abc-123",
            username = "QuantumGhost",
            profileColor = "#00F0FF",
            ephPub = ByteArray(32) { it.toByte() },
            identityPub = ByteArray(32) { (it + 5).toByte() },
            onion = "testonionaddress56characterslongexampleaddress12345678.onion",
            nonce = ByteArray(32) { (it * 2).toByte() },
            timestamp = System.currentTimeMillis(),
            signature = ByteArray(64) { 0x44 }
        )

        val qrString = payload.toQrString()
        println("Generated QR string length: ${qrString.length}")
        println("Generated QR string: $qrString")

        val size = 640
        val hints = mapOf(
            com.google.zxing.EncodeHintType.ERROR_CORRECTION to com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.L,
            com.google.zxing.EncodeHintType.MARGIN to 2,
            com.google.zxing.EncodeHintType.CHARACTER_SET to "UTF-8"
        )
        val bitMatrix = QRCodeWriter().encode(qrString, BarcodeFormat.QR_CODE, size, size, hints)
        val pixels = IntArray(size * size)
        for (y in 0 until size) {
            for (x in 0 until size) {
                pixels[y * size + x] = if (bitMatrix.get(x, y)) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
            }
        }

        val source = RGBLuminanceSource(size, size, pixels)
        val bitmap = BinaryBitmap(HybridBinarizer(source))
        val decodedText = QRCodeReader().decode(bitmap).text
        println("Decoded QR text: $decodedText")

        val parsedPayload = `in`.grayscales.entangl.domain.model.HandshakePayload.fromQrString(decodedText)
        org.junit.Assert.assertEquals(payload, parsedPayload)
        org.junit.Assert.assertEquals("#00F0FF", parsedPayload.profileColor)
        println("Successfully decoded and verified HandshakePayload!")
    }
}
