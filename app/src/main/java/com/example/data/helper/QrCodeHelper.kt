package com.example.data.helper

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix

object QrCodeHelper {

    /**
     * Generates a QR Code payload string for a room code
     * Format: "CODE|TIMESTAMP"
     */
    fun generatePayload(roomCode: String): String {
        val timestamp = System.currentTimeMillis()
        return "$roomCode|$timestamp"
    }

    /**
     * Generates a QR Code Bitmap from text payload
     */
    fun generateQrCodeBitmap(payload: String, width: Int = 512, height: Int = 512): Bitmap? {
        return try {
            val bitMatrix: BitMatrix = MultiFormatWriter().encode(
                payload,
                BarcodeFormat.QR_CODE,
                width,
                height
            )
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bmp.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
                }
            }
            bmp
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
