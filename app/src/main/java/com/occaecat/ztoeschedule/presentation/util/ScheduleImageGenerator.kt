package com.occaecat.ztoeschedule.presentation.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import androidx.core.graphics.createBitmap
import androidx.core.content.FileProvider
import com.occaecat.ztoeschedule.domain.GroupedSchedule
import java.io.File
import java.io.FileOutputStream

object ScheduleImageGenerator {

    fun generateAndShare(
        context: Context,
        address: String,
        cherga: String,
        schedules: List<GroupedSchedule>
    ): Uri? {
        return try {
            val bitmap = drawScheduleBitmap(context, address, cherga, schedules)
            saveBitmapToCache(context, bitmap)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun drawScheduleBitmap(
        context: Context,
        address: String,
        cherga: String,
        schedules: List<GroupedSchedule>
    ): Bitmap {
        val width = 1080
        // Approximate height calculation: Header + Address + (Items * ItemHeight) + Footer
        val groupedByDate = schedules.groupBy { it.date }
        val itemCount = groupedByDate.values.sumOf { it.size } + groupedByDate.size // Items + Date Headers
        val itemHeight = 100
        val headerHeight = 350
        val footerHeight = 150
        val height = headerHeight + (itemCount * itemHeight) + footerHeight

        val bitmap = createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Colors
        val bgPaint = Paint().apply { color = Color.WHITE }
        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 40f
            isAntiAlias = true
            typeface = Typeface.DEFAULT
        }
        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 56f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val secondaryTextPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 36f
            isAntiAlias = true
        }
        val greenPaint = Paint().apply { color = androidx.core.content.ContextCompat.getColor(context, com.occaecat.ztoeschedule.R.color.widget_power_on) }
        val redPaint = Paint().apply { color = androidx.core.content.ContextCompat.getColor(context, com.occaecat.ztoeschedule.R.color.widget_power_off) }
        val yellowPaint = Paint().apply { color = android.graphics.Color.YELLOW }

        // Background
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Header
        var currentY = 100f
        canvas.drawText("Графік відключень", 60f, currentY, titlePaint)
        currentY += 80f
        
        // Address splitting if too long
        val addressWords = address.split(" ")
        var line = ""
        addressWords.forEach { word ->
            if (textPaint.measureText("$line $word") < width - 120) {
                line = "$line $word".trim()
            } else {
                canvas.drawText(line, 60f, currentY, textPaint)
                currentY += 50f
                line = word
            }
        }
        if (line.isNotEmpty()) canvas.drawText(line, 60f, currentY, textPaint)
        
        currentY += 70f
        canvas.drawText("Черга: $cherga", 60f, currentY, titlePaint)
        currentY += 100f

        // Draw List
        groupedByDate.forEach { (date, items) ->
            // Date Header
            canvas.drawText("🗓 $date", 60f, currentY, titlePaint)
            currentY += 80f

            items.forEach { item ->
                // Status Indicator
                val color = when (item.status) {
                    com.occaecat.ztoeschedule.data.model.ScheduleStatus.Available -> greenPaint
                    com.occaecat.ztoeschedule.data.model.ScheduleStatus.Probable -> yellowPaint
                    else -> redPaint
                }
                
                val rect = RectF(60f, currentY - 40f, 80f, currentY + 10f)
                canvas.drawRoundRect(rect, 8f, 8f, color)

                // Time Span
                canvas.drawText(item.span, 110f, currentY, textPaint)
                
                // Status Text
                val statusText = item.displayText
                val textWidth = textPaint.measureText(statusText)
                canvas.drawText(statusText, width - textWidth - 60f, currentY, secondaryTextPaint)

                currentY += itemHeight.toFloat()
            }
            currentY += 20f
        }

        // Footer
        val footerPaint = Paint().apply {
            color = Color.LTGRAY
            textSize = 30f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("СвітлоЄ? Житомир", width / 2f, height - 60f, footerPaint)

        return bitmap
    }

    private fun saveBitmapToCache(context: Context, bitmap: Bitmap): Uri {
        val imagesFolder = File(context.cacheDir, "images")
        imagesFolder.mkdirs()
        val file = File(imagesFolder, "schedule_share.png")
        val stream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, 90, stream)
        stream.flush()
        stream.close()
        
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }
}
