package `in`.grayscales.entangl.ui.qr

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.createBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * High-performance QR Code generator producing high-contrast Bitmaps
 * styled for OLED screens and sub-second camera optical lock.
 */
object QrCodeGenerator {

    /**
     * Generates an [ImageBitmap] representing the given [content].
     *
     * @param content Encoded payload string (e.g. Base64Url CBOR handshake).
     * @param sizePx Pixel dimension for width and height.
     * @param foregroundColor ARGB int for dark modules (default: pure white for OLED contrast).
     * @param backgroundColor ARGB int for quiet zone and light modules (default: void #050508).
     */
    fun generate(
        content: String,
        sizePx: Int = 640,
        foregroundColor: Int = android.graphics.Color.BLACK,
        backgroundColor: Int = android.graphics.Color.WHITE
    ): ImageBitmap {
        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.L,
            EncodeHintType.MARGIN to 2,
            EncodeHintType.CHARACTER_SET to "UTF-8"
        )

        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
        val width = bitMatrix.width
        val height = bitMatrix.height

        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            val offset = y * width
            for (x in 0 until width) {
                pixels[offset + x] = if (bitMatrix.get(x, y)) foregroundColor else backgroundColor
            }
        }

        val bitmap = createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap.asImageBitmap()
    }
}
