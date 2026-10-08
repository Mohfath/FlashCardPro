package com.matt.flashcard

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.res.ResourcesCompat
import java.io.ByteArrayOutputStream

/**
 * Draws the Card as the square image Android Auto shows beside the media controls.
 * Layout: pure black, huge white Prompt, cobalt Answer.
 */
class CardArtwork(context: Context) {
    private val base = ResourcesCompat.getFont(context, R.font.plus_jakarta_sans) ?: Typeface.DEFAULT
    private val bold = Typeface.create(base, Typeface.BOLD)

    private fun text(color: Int, sizePx: Float, face: Typeface = bold) =
        TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            textSize = sizePx
            typeface = face
        }

    fun render(ui: ReviewUi): ByteArray {
        val bitmap = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.BLACK)

        val margin = 72f
        val width = SIZE - 2 * margin

        // Language label (left) and Box badge (right).
        val label = text(MUTED, 38f)
        canvas.drawText(if (ui.revealed) "${ui.promptLanguage} → ${ui.answerLanguage}" else ui.promptLanguage, margin, margin + 40f, label)
        val badge = "BOX ${ui.box}"
        val badgePaint = text(Color.WHITE, 38f)
        val badgeW = badgePaint.measureText(badge) + 48f
        canvas.drawRoundRect(RectF(SIZE - margin - badgeW, margin - 8f, SIZE - margin, margin + 64f), 36f, 36f, Paint().apply { color = COBALT })
        canvas.drawText(badge, SIZE - margin - badgeW + 24f, margin + 40f, badgePaint)

        // Prompt: as large as fits in at most three lines.
        val promptSize = if (ui.revealed) 84f else 120f
        val prompt = fit(ui.promptText, text(Color.WHITE, promptSize), width, maxLines = 3)
        val answerBlock = if (ui.revealed) fit(ui.answerText, text(COBALT_LIGHT, 92f), width, maxLines = 3) else null

        val gap = 56f
        val total = prompt.height + (answerBlock?.let { gap * 2 + 4 + it.height } ?: 0f)
        var y = (SIZE - total) / 2f + 40f
        canvas.save(); canvas.translate(margin, y); prompt.draw(canvas); canvas.restore()
        y += prompt.height
        if (answerBlock != null) {
            y += gap
            canvas.drawRect(margin, y, SIZE - margin, y + 4f, Paint().apply { color = RULE })
            y += gap + 4
            canvas.save(); canvas.translate(margin, y); answerBlock.draw(canvas); canvas.restore()
        }

        if (ui.pendingDelete) {
            canvas.drawText("Press Delete again to remove this Card", margin, SIZE - margin, text(DANGER, 40f))
        } else if (ui.extraPractice) {
            canvas.drawText("EXTRA PRACTICE", margin, SIZE - margin, text(AMBER, 40f))
        }

        return ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }.toByteArray()
    }

    /** Shrinks the text until it fits in [maxLines]; very long sentences end up smaller, not cut off. */
    private fun fit(text: String, paint: TextPaint, width: Float, maxLines: Int): StaticLayout {
        var layout = layout(text, paint, width)
        while (layout.lineCount > maxLines && paint.textSize > 40f) {
            paint.textSize -= 6f
            layout = layout(text, paint, width)
        }
        return layout
    }

    private fun layout(text: String, paint: TextPaint, width: Float) =
        StaticLayout.Builder.obtain(text, 0, text.length, paint, width.toInt())
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, 1.1f)
            .build()

    private companion object {
        const val SIZE = 1024
        val COBALT = Color.parseColor("#3B82F6")
        val COBALT_LIGHT = Color.parseColor("#60A5FA")
        val MUTED = Color.parseColor("#94A3B8")
        val RULE = Color.parseColor("#1E2430")
        val AMBER = Color.parseColor("#F59E0B")
        val DANGER = Color.parseColor("#F87171")
    }
}
