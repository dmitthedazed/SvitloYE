package com.occaecat.ztoeschedule.presentation.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.core.graphics.PathParser
import androidx.core.graphics.createBitmap
import androidx.core.content.FileProvider
import com.occaecat.ztoeschedule.R
import com.occaecat.ztoeschedule.data.model.ScheduleStatus
import com.occaecat.ztoeschedule.domain.GroupedSchedule
import com.occaecat.ztoeschedule.domain.ScheduleMapper
import java.io.File
import java.io.FileOutputStream

object ScheduleImageGenerator {

    // Logo palette (launcher icon: blue field, white "Є", yellow bolt)
    private const val BLUE = 0xFF1A5FD0.toInt()
    private const val BLUE_DEEP = 0xFF0A2E7A.toInt()
    private const val BOLT = 0xFFFFB800.toInt()
    private const val BG = 0xFFF8F9FF.toInt()
    private const val CARD = 0xFFE7ECF8.toInt()
    private const val ON_SURFACE = 0xFF191B23.toInt()
    private const val ON_VARIANT = 0xFF44474F.toInt()
    private const val TRACK = 0xFFC3CCE4.toInt()
    private const val OUTAGE = 0xFFBA1A1A.toInt()
    private const val PROBABLE = 0xFFD78078.toInt()
    private const val OUTAGE_CONTAINER = 0xFFFFDAD6.toInt()
    private const val ON_OUTAGE_CONTAINER = 0xFF93000A.toInt()
    private const val PROBABLE_CONTAINER = 0xFFFCE0D7.toInt()
    private const val ON_PROBABLE_CONTAINER = 0xFF7A3A30.toInt()
    private const val CHERGA_CONTAINER = 0xFFD9E2FF.toInt()
    private const val ON_CHERGA_CONTAINER = 0xFF0B2E7A.toInt()

    private const val ICON_OUTAGE = "M3.27 3L2 4.27l5 5V13h3v9l3.58-6.14L17.73 20 19 18.73 3.27 3zM17 10h-4l4-8H7v2.18l8.46 8.46L17 10z"
    private const val ICON_PROBABLE = "M12 5.99L19.53 19H4.47L12 5.99M12 2L1 21h22L12 2zm1 14h-2v2h2v-2zm0-6h-2v4h2v-4z"

    private const val W = 360f          // design width in dp, rendered at 3x
    private const val SCALE = 3f
    private const val PAD = 16f

    fun generateAndShare(
        context: Context,
        street: String,
        city: String,
        cherga: String,
        updated: String,
        schedules: List<GroupedSchedule>
    ): Uri? {
        return try {
            saveBitmapToCache(context, drawScheduleBitmap(context, street, city, cherga, updated, schedules))
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun paint(block: Paint.() -> Unit) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.DEFAULT
        fontFeatureSettings = "tnum"
        block()
    }

    private fun parseMin(t: String): Int {
        val parts = t.trim().split(":")
        return (parts.getOrNull(0)?.toIntOrNull() ?: 0) * 60 + (parts.getOrNull(1)?.toIntOrNull() ?: 0)
    }

    private fun wrap(text: String, p: Paint, maxW: Float): List<String> {
        val lines = mutableListOf<String>()
        var line = ""
        text.split(" ").forEach { w ->
            if (line.isEmpty() || p.measureText("$line $w") <= maxW) line = "$line $w".trim()
            else { lines += line; line = w }
        }
        if (line.isNotEmpty()) lines += line
        return lines
    }

    private fun roundedPath(r: RectF, tl: Float, tr: Float, br: Float, bl: Float) = Path().apply {
        addRoundRect(r, floatArrayOf(tl, tl, tr, tr, br, br, bl, bl), Path.Direction.CW)
    }

    private fun drawIcon(canvas: Canvas, data: String, x: Float, y: Float, size: Float, color: Int) {
        val path = PathParser.createPathFromPathData(data)
        canvas.save()
        canvas.translate(x, y)
        canvas.scale(size / 24f, size / 24f)
        canvas.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color })
        canvas.restore()
    }

    private fun drawLogo(context: Context, canvas: Canvas, x: Float, y: Float, size: Float, letter: Int, accent: Int) {
        listOf(R.drawable.ic_logo_letter to letter, R.drawable.ic_logo_accent to accent).forEach { (res, tint) ->
            ContextCompat.getDrawable(context, res)?.mutate()?.let { d ->
                d.setTint(tint)
                d.setBounds(x.toInt(), y.toInt(), (x + size).toInt(), (y + size).toInt())
                d.draw(canvas)
            }
        }
    }

    private fun drawScheduleBitmap(
        context: Context,
        street: String,
        city: String,
        cherga: String,
        updated: String,
        schedules: List<GroupedSchedule>
    ): Bitmap {
        val bold = Typeface.DEFAULT_BOLD
        val streetP = paint { color = ON_SURFACE; textSize = 16f; typeface = bold; letterSpacing = 0.01f }
        val cityP = paint { color = ON_VARIANT; textSize = 12f }
        val chergaLabelP = paint { color = ON_CHERGA_CONTAINER; textSize = 11f; textAlign = Paint.Align.CENTER }
        val chergaP = paint { color = ON_CHERGA_CONTAINER; textSize = 20f; typeface = bold; textAlign = Paint.Align.CENTER }
        val dayP = paint { color = ON_SURFACE; textSize = 20f; typeface = bold }
        val totalP = paint { color = OUTAGE; textSize = 14f; typeface = bold; textAlign = Paint.Align.RIGHT }
        val segP = paint { color = Color.WHITE; textSize = 11f; typeface = bold; textAlign = Paint.Align.CENTER }
        val tickP = paint { color = ON_VARIANT; textSize = 11f }
        val chipP = paint { textSize = 15f; typeface = bold }
        val legendP = paint { color = ON_VARIANT; textSize = 12f }

        val groups = schedules.groupBy { it.date }
        val groupList = groups.entries.toList()

        // ---- measure ----
        val textW = W - PAD * 2 - 16f * 2 - 52f - 14f - 70f - 14f
        val streetLines = wrap(street.ifBlank { city }, streetP, textW)
        val headTextH = streetLines.size * 24f + if (city.isNotBlank() && street.isNotBlank()) 16f else 0f
        val headH = 32f + maxOf(52f, headTextH)

        val chipPadX = 12f
        val chipRows = groupList.map { (_, items) ->
            val chips = items.filter { it.status != ScheduleStatus.Available }
            var rows = if (chips.isEmpty()) 0 else 1
            var x = 0f
            val avail = W - PAD * 2 - 32f
            chips.forEach { c ->
                val w = 10f + 18f + 6f + chipP.measureText(c.span) + chipPadX
                if (x > 0f && x + w > avail) { rows++; x = 0f }
                x += w + 6f
            }
            rows
        }
        val dayHeights = chipRows.map { rows ->
            18f + 28f + 14f + 44f + 6f + 16f + (if (rows > 0) 14f + rows * 36f + (rows - 1) * 6f else 0f) + 16f
        }
        val height = PAD + headH + groupList.size * 2f + dayHeights.sum() + 16f + 16f + 8f + 20f + 20f

        val bitmap = createBitmap((W * SCALE).toInt(), (height * SCALE).toInt(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.scale(SCALE, SCALE)
        canvas.drawColor(BG)
        val fill = Paint(Paint.ANTI_ALIAS_FLAG)

        var y = PAD
        // ---- header card ----
        fill.color = CARD
        canvas.drawPath(roundedPath(RectF(PAD, y, W - PAD, y + headH), 28f, 28f, 6f, 6f), fill)
        val badge = RectF(PAD + 16f, y + (headH - 52f) / 2f, PAD + 16f + 52f, y + (headH + 52f) / 2f)
        fill.shader = LinearGradient(badge.left, badge.top, badge.right, badge.bottom, BLUE, BLUE_DEEP, Shader.TileMode.CLAMP)
        canvas.drawRoundRect(badge, 18f, 18f, fill)
        fill.shader = null
        drawLogo(context, canvas, badge.left + 8f, badge.top + 8f, 36f, Color.WHITE, BOLT)
        val tx = badge.right + 14f
        var ty = y + (headH - headTextH) / 2f
        streetLines.forEach { ty += 17f; canvas.drawText(it, tx, ty, streetP); ty += 7f }
        if (city.isNotBlank() && street.isNotBlank()) canvas.drawText(city, tx, ty + 12f, cityP)
        val chipR = RectF(W - PAD - 16f - 70f, y + (headH - 56f) / 2f, W - PAD - 16f, y + (headH + 56f) / 2f)
        fill.color = CHERGA_CONTAINER
        canvas.drawRoundRect(chipR, 18f, 18f, fill)
        canvas.drawText("черга", chipR.centerX(), chipR.top + 18f, chergaLabelP)
        canvas.drawText(cherga, chipR.centerX(), chipR.top + 42f, chergaP)
        y += headH + 2f

        // ---- day cards ----
        val inner = W - PAD * 2 - 32f
        groupList.forEachIndexed { gi, (date, items) ->
            val h = dayHeights[gi]
            val last = gi == groupList.lastIndex
            fill.color = CARD
            canvas.drawPath(roundedPath(RectF(PAD, y, W - PAD, y + h), 6f, 6f, if (last) 28f else 6f, if (last) 28f else 6f), fill)
            val x0 = PAD + 16f
            var cy = y + 18f

            val outMin = items.filter { it.status == ScheduleStatus.Outage }.sumOf { it.durationHours * 60 + it.durationMinutes }
            canvas.drawText(date, x0, cy + 20f, dayP)
            if (outMin > 0) {
                totalP.color = OUTAGE
                canvas.drawText("−" + ScheduleMapper.formatDuration(outMin / 60, outMin % 60), x0 + inner, cy + 18f, totalP)
            } else {
                totalP.color = BLUE
                canvas.drawText("без відключень", x0 + inner, cy + 18f, totalP)
            }
            cy += 28f + 14f

            // timeline: light track + coloured outage/probable segments
            val barTop = cy
            val barRect = RectF(x0, barTop, x0 + inner, barTop + 44f)
            fill.color = TRACK
            canvas.drawRoundRect(barRect, 14f, 14f, fill)
            items.filter { it.status != ScheduleStatus.Available }.forEach { it0 ->
                val st = parseMin(it0.startTime)
                var en = parseMin(it0.endTime)
                if (en <= st) en = 1440
                val l = x0 + inner * st / 1440f
                val r = x0 + inner * en / 1440f
                val rl = if (st == 0) 14f else 4f
                val rr = if (en >= 1440) 14f else 4f
                fill.color = if (it0.status == ScheduleStatus.Outage) OUTAGE else PROBABLE
                canvas.drawPath(roundedPath(RectF(l + 1f, barTop, r - 1f, barTop + 44f), rl, rr, rr, rl), fill)
                if (en - st >= 180) {
                    segP.color = if (it0.status == ScheduleStatus.Outage) Color.WHITE else ON_SURFACE
                    canvas.drawText(it0.startTime, (l + r) / 2f, barTop + 26f, segP)
                }
            }
            cy += 44f + 6f
            listOf(0, 6, 12, 18, 24).forEach { hr ->
                val label = "%02d".format(hr)
                val cx = x0 + inner * hr / 24f
                val px = when (hr) { 0 -> cx; 24 -> cx - tickP.measureText(label); else -> cx - tickP.measureText(label) / 2f }
                canvas.drawText(label, px, cy + 11f, tickP)
            }
            cy += 16f

            // interval chips
            val chips = items.filter { it.status != ScheduleStatus.Available }
            if (chips.isNotEmpty()) {
                cy += 14f
                var cx = x0
                var rowTop = cy
                chips.forEach { c ->
                    val w = 10f + 18f + 6f + chipP.measureText(c.span) + chipPadX
                    if (cx > x0 && cx + w > x0 + inner) { cx = x0; rowTop += 42f }
                    val outage = c.status == ScheduleStatus.Outage
                    fill.color = if (outage) OUTAGE_CONTAINER else PROBABLE_CONTAINER
                    canvas.drawRoundRect(RectF(cx, rowTop, cx + w, rowTop + 36f), 12f, 12f, fill)
                    val fg = if (outage) ON_OUTAGE_CONTAINER else ON_PROBABLE_CONTAINER
                    drawIcon(canvas, if (outage) ICON_OUTAGE else ICON_PROBABLE, cx + 10f, rowTop + 9f, 18f, fg)
                    chipP.color = fg
                    canvas.drawText(c.span, cx + 34f, rowTop + 24f, chipP)
                    cx += w + 6f
                }
            }
            y += h + 2f
        }

        // ---- legend + footer ----
        y += 14f
        val legend = listOf("Відключення" to OUTAGE, "Можливе" to PROBABLE, "Світло є" to TRACK)
        val itemW = legend.map { 14f + 6f + legendP.measureText(it.first) }
        var lx = (W - (itemW.sum() + 16f * (legend.size - 1))) / 2f
        legend.forEachIndexed { i, (label, c) ->
            fill.color = c
            canvas.drawRoundRect(RectF(lx, y + 4f, lx + 14f, y + 12f), 4f, 4f, fill)
            canvas.drawText(label, lx + 20f, y + 12f, legendP)
            lx += itemW[i] + 16f
        }
        y += 28f
        val footer = if (updated.isNotBlank()) "СвітлоЄ? · дані від $updated" else "СвітлоЄ?"
        val fw = 16f + 6f + legendP.measureText(footer)
        val fx = (W - fw) / 2f
        drawLogo(context, canvas, fx, y - 1f, 16f, BLUE, BOLT)
        canvas.drawText(footer, fx + 22f, y + 12f, legendP)

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
