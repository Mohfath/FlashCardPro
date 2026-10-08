package com.matt.flashcard

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.res.ResourcesCompat
import java.io.ByteArrayOutputStream

/**
 * Draws the Card as the square image Android Auto shows beside the media controls.
 * The colour follows the Leitner Box (red for Box 1 up to green for Box 5), so progress reads at a glance:
 * flag and language on top, cards left on the right, a huge Prompt, the Answer in the Box colour, and a
 * five-step Box bar at the bottom.
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
        val look = BoxLook.of(ui.box)

        canvas.drawRect(
            0f, 0f, SIZE.toFloat(), SIZE.toFloat(),
            Paint().apply { shader = LinearGradient(0f, 0f, 0f, SIZE.toFloat(), look.tint, NIGHT, Shader.TileMode.CLAMP) },
        )

        val margin = 64f
        val width = SIZE - 2 * margin

        // Top row: flag and language on the left, cards left (or extra practice) on the right.
        val flag = if (ui.revealed) ui.answerFlag.takeIf { it.isNotEmpty() } else ui.promptFlag.takeIf { it.isNotEmpty() }
        var labelX = margin
        if (flag != null) {
            canvas.drawText(flag, margin, margin + 52f, text(Color.WHITE, 60f))
            labelX += 92f
        }
        val languages = if (ui.revealed) "${ui.promptLanguage} → ${ui.answerLanguage}" else ui.promptLanguage
        canvas.drawText(languages, labelX, margin + 46f, text(MUTED, 38f))

        val pill = if (ui.extraPractice) "EXTRA PRACTICE" else "${ui.dueRemaining} LEFT"
        val pillPaint = text(if (ui.extraPractice) AMBER else Color.WHITE, 36f)
        val pillW = pillPaint.measureText(pill) + 56f
        val pillRect = RectF(SIZE - margin - pillW, margin - 4f, SIZE - margin, margin + 68f)
        canvas.drawRoundRect(pillRect, 36f, 36f, Paint().apply { color = PILL })
        canvas.drawText(pill, pillRect.left + 28f, margin + 46f, pillPaint)

        // Prompt (and Answer): as large as fits, each followed by its Latin-letter reading when there is one.
        val promptSize = if (ui.revealed) 92f else 124f
        val promptBlock = fit(ui.promptText, text(Color.WHITE, promptSize), width, maxLines = 3)
        val promptReading = ui.promptRomanization?.let { fit(it, text(MUTED, 46f), width, maxLines = 2) }
        val answerBlock = if (ui.revealed) fit(ui.answerText, text(look.accent, 96f), width, maxLines = 3) else null
        val answerReading = if (ui.revealed) ui.answerRomanization?.let { fit(it, text(MUTED, 46f), width, maxLines = 2) } else null

        val gap = 28f
        val ruleGap = 48f
        var total = promptBlock.height.toFloat()
        promptReading?.let { total += gap + it.height }
        if (answerBlock != null) {
            total += ruleGap * 2 + 5f + answerBlock.height
            answerReading?.let { total += gap + it.height }
        }
        val top = margin + 100f
        val bottom = SIZE - margin - 110f
        var y = top + ((bottom - top) - total).coerceAtLeast(0f) / 2f
        fun draw(layout: StaticLayout) {
            canvas.save(); canvas.translate(margin, y); layout.draw(canvas); canvas.restore()
            y += layout.height
        }
        draw(promptBlock)
        promptReading?.let { y += gap; draw(it) }
        if (answerBlock != null) {
            y += ruleGap
            canvas.drawRoundRect(RectF(margin, y, SIZE - margin, y + 5f), 3f, 3f, Paint().apply { color = look.accent; alpha = 140 })
            y += ruleGap + 5f
            draw(answerBlock)
            answerReading?.let { y += gap; draw(it) }
        }

        // Bottom: five Box steps, filled up to this Card's Box.
        val barTop = SIZE - margin - 22f
        val step = (width - 4 * 14f) / 5f
        for (i in Leitner.MIN_BOX..Leitner.MAX_BOX) {
            val left = margin + (i - 1) * (step + 14f)
            canvas.drawRoundRect(
                RectF(left, barTop, left + step, barTop + 22f), 11f, 11f,
                Paint().apply { color = if (i <= ui.box) look.accent else BAR_OFF },
            )
        }
        canvas.drawText("BOX ${ui.box} OF ${Leitner.MAX_BOX}", margin, barTop - 24f, text(look.accent, 34f))

        return ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }.toByteArray()
    }

    /** Shrinks the text until it fits in [maxLines]; very long sentences end up smaller, not cut off. */
    private fun fit(text: String, paint: TextPaint, width: Float, maxLines: Int): StaticLayout {
        var layout = layout(text, paint, width)
        while (layout.lineCount > maxLines && paint.textSize > 36f) {
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

    /** The colours of one Leitner Box: a bright accent for text and the bar, and a dark tint for the top of the card. */
    private class BoxLook(val accent: Int, val tint: Int) {
        companion object {
            private val looks = listOf(
                BoxLook(Color.parseColor("#F87171"), Color.parseColor("#3A1519")),
                BoxLook(Color.parseColor("#FB923C"), Color.parseColor("#3A200E")),
                BoxLook(Color.parseColor("#FACC15"), Color.parseColor("#332A07")),
                BoxLook(Color.parseColor("#2DD4BF"), Color.parseColor("#08302B")),
                BoxLook(Color.parseColor("#4ADE80"), Color.parseColor("#0A3320")),
            )

            fun of(box: Int) = looks[(box - Leitner.MIN_BOX).coerceIn(0, looks.size - 1)]
        }
    }

    private companion object {
        const val SIZE = 1024
        val NIGHT = Color.parseColor("#0A0C10")
        val MUTED = Color.parseColor("#B6C0CF")
        val PILL = Color.parseColor("#26FFFFFF")
        val BAR_OFF = Color.parseColor("#2A303B")
        val AMBER = Color.parseColor("#FBBF24")
    }
}
