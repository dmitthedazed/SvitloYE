package com.occaecat.ztoeschedule.presentation.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.google.zxing.qrcode.encoder.Encoder

object QrCodeGenerator {
    fun generate(content: String, sizePx: Int, marginPx: Int = 1): Bitmap? {
        if (content.isBlank() || sizePx <= 0) return null
        return runCatching {
            val hints = mapOf(EncodeHintType.MARGIN to marginPx)
            val matrix = QRCodeWriter().encode(
                content,
                BarcodeFormat.QR_CODE,
                sizePx,
                sizePx,
                hints
            )
            val bitmap = createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            for (x in 0 until sizePx) {
                for (y in 0 until sizePx) {
                    bitmap[x, y] = if (matrix[x, y]) Color.BLACK else Color.WHITE
                }
            }
            bitmap
        }.getOrNull()
    }

    /** Rounded-dot QR with rounded "eye" markers, drawn from the ZXing module matrix. */
    fun generateStyled(content: String, sizePx: Int, fgColor: Int, bgColor: Int, quietModules: Int = 2): Bitmap? {
        if (content.isBlank() || sizePx <= 0) return null
        return runCatching {
            val hints = mapOf(
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.Q,
                EncodeHintType.MARGIN to 0
            )
            val m = Encoder.encode(content, ErrorCorrectionLevel.Q, hints).matrix
            val n = m.width
            val cell = sizePx.toFloat() / (n + quietModules * 2)
            val off = quietModules * cell
            val bitmap = createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(bgColor)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = fgColor }

            fun inEye(x: Int, y: Int) =
                (x < 7 && y < 7) || (x >= n - 7 && y < 7) || (x < 7 && y >= n - 7)

            val r = cell * 0.46f
            for (y in 0 until n) for (x in 0 until n) {
                if (m.get(x, y).toInt() == 1 && !inEye(x, y)) {
                    val cx = off + x * cell + cell / 2
                    val cy = off + y * cell + cell / 2
                    canvas.drawCircle(cx, cy, r, paint)
                }
            }
            for ((ex, ey) in listOf(0 to 0, n - 7 to 0, 0 to n - 7)) {
                val left = off + ex * cell
                val top = off + ey * cell
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = cell
                canvas.drawRoundRect(
                    RectF(left + cell / 2, top + cell / 2, left + 6.5f * cell, top + 6.5f * cell),
                    cell * 2f, cell * 2f, paint
                )
                paint.style = Paint.Style.FILL
                canvas.drawRoundRect(
                    RectF(left + 2 * cell, top + 2 * cell, left + 5 * cell, top + 5 * cell),
                    cell * 1.1f, cell * 1.1f, paint
                )
            }
            bitmap
        }.getOrNull()
    }
}
