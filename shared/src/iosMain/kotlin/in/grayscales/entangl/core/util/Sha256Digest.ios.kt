package `in`.grayscales.entangl.core.util

actual object Sha256Digest {
    actual fun digest(input: ByteArray): ByteArray {
        // Pure Kotlin SHA-256 fallback for iOS / native compilation
        val k = intArrayOf(
            0x428a2f98.toInt(), 0x71374491.toInt(), 0xb5c0fbcf.toInt(), 0xe9b5dba5.toInt(),
            0x3956c25b.toInt(), 0x59f111f1.toInt(), 0x923f82a4.toInt(), 0xab1c5ed5.toInt(),
            0xd807aa98.toInt(), 0x12835b01.toInt(), 0x243185be.toInt(), 0x550c7dc3.toInt(),
            0x72be5d74.toInt(), 0x80deb1fe.toInt(), 0x9bdc06a7.toInt(), 0xc19bf174.toInt(),
            0xe49b69c1.toInt(), 0xefbe4786.toInt(), 0x0fc19dc6.toInt(), 0x240ca1cc.toInt(),
            0x2de92c6f.toInt(), 0x4a7484aa.toInt(), 0x5cb0a9dc.toInt(), 0x76f988da.toInt(),
            0x983e5152.toInt(), 0xa831c66d.toInt(), 0xb00327c8.toInt(), 0xbf597fc7.toInt(),
            0xc6e00bf3.toInt(), 0xd5a79147.toInt(), 0x06ca6351.toInt(), 0x14292967.toInt(),
            0x27b70a85.toInt(), 0x2e1b2138.toInt(), 0x4d2c6dfc.toInt(), 0x53380d13.toInt(),
            0x650a7354.toInt(), 0x766a0abb.toInt(), 0x81c2c92e.toInt(), 0x92722c85.toInt(),
            0xa2bfe8a1.toInt(), 0xa81a664b.toInt(), 0xc24b8b70.toInt(), 0xc76c51a3.toInt(),
            0xd192e819.toInt(), 0xd6990624.toInt(), 0xf40e3585.toInt(), 0x106aa070.toInt(),
            0x19a4c116.toInt(), 0x1e376c08.toInt(), 0x2748774c.toInt(), 0x34b0bcb5.toInt(),
            0x391c0cb3.toInt(), 0x4ed8aa4a.toInt(), 0x5b9cca4f.toInt(), 0x682e6ff3.toInt(),
            0x748f82ee.toInt(), 0x78a5636f.toInt(), 0x84c87814.toInt(), 0x8cc70208.toInt(),
            0x90befffa.toInt(), 0xa4506ceb.toInt(), 0xbef9a3f7.toInt(), 0xc67178f2.toInt()
        )

        var h0 = 0x6a09e667.toInt()
        var h1 = 0xbb67ae85.toInt()
        var h2 = 0x3c6ef372.toInt()
        var h3 = 0xa54ff53a.toInt()
        var h4 = 0x510e527f.toInt()
        var h5 = 0x9b05688c.toInt()
        var h6 = 0x1f83d9ab.toInt()
        var h7 = 0x5be0cd19.toInt()

        val bitLength = input.size.toLong() * 8L
        val padLen = ((56 - ((input.size + 1) % 64)) + 64) % 64
        val totalLen = input.size + 1 + padLen + 8
        val padded = ByteArray(totalLen)
        System.arraycopy(input, 0, padded, 0, input.size)
        padded[input.size] = 0x80.toByte()
        for (i in 0 until 8) {
            padded[totalLen - 8 + i] = ((bitLength ushr ((7 - i) * 8)) and 0xFF).toByte()
        }

        val w = IntArray(64)
        for (chunk in 0 until totalLen step 64) {
            for (i in 0 until 16) {
                w[i] = ((padded[chunk + i * 4].toInt() and 0xFF) shl 24) or
                        ((padded[chunk + i * 4 + 1].toInt() and 0xFF) shl 16) or
                        ((padded[chunk + i * 4 + 2].toInt() and 0xFF) shl 8) or
                        (padded[chunk + i * 4 + 3].toInt() and 0xFF)
            }
            for (i in 16 until 64) {
                val s0 = (w[i - 15] ushr 7 or (w[i - 15] shl 25)) xor
                        (w[i - 15] ushr 18 or (w[i - 15] shl 14)) xor
                        (w[i - 15] ushr 3)
                val s1 = (w[i - 2] ushr 17 or (w[i - 2] shl 15)) xor
                        (w[i - 2] ushr 19 or (w[i - 2] shl 13)) xor
                        (w[i - 2] ushr 10)
                w[i] = w[i - 16] + s0 + w[i - 7] + s1
            }

            var a = h0; var b = h1; var c = h2; var d = h3
            var e = h4; var f = h5; var g = h6; var h = h7

            for (i in 0 until 64) {
                val s1 = (e ushr 6 or (e shl 26)) xor (e ushr 11 or (e shl 21)) xor (e ushr 25 or (e shl 7))
                val ch = (e and f) xor (e.inv() and g)
                val temp1 = h + s1 + ch + k[i] + w[i]
                val s0 = (a ushr 2 or (a shl 30)) xor (a ushr 13 or (a shl 19)) xor (a ushr 22 or (a shl 10))
                val maj = (a and b) xor (a and c) xor (b and c)
                val temp2 = s0 + maj

                h = g; g = f; f = e; e = d + temp1
                d = c; c = b; b = a; a = temp1 + temp2
            }

            h0 += a; h1 += b; h2 += c; h3 += d
            h4 += e; h5 += f; h6 += g; h7 += h
        }

        val result = ByteArray(32)
        val hArray = intArrayOf(h0, h1, h2, h3, h4, h5, h6, h7)
        for (i in 0 until 8) {
            result[i * 4] = ((hArray[i] ushr 24) and 0xFF).toByte()
            result[i * 4 + 1] = ((hArray[i] ushr 16) and 0xFF).toByte()
            result[i * 4 + 2] = ((hArray[i] ushr 8) and 0xFF).toByte()
            result[i * 4 + 3] = (hArray[i] and 0xFF).toByte()
        }
        return result
    }
}
