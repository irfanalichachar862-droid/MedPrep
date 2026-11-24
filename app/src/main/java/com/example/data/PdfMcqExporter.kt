package com.example.data

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.ui.components.isStructuralChemicalText
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Generates clean multi-page A4 PDF documents for downloaded MCQs using Android's native PdfDocument API.
 * Preserves exact monospace alignment for structural Chemistry formulas and includes Answer Key + Reason.
 */
object PdfMcqExporter {
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN_X = 36f
    private const val MARGIN_TOP = 38f
    private const val MARGIN_BOTTOM = 44f
    private const val CONTENT_WIDTH = PAGE_WIDTH - (MARGIN_X * 2)

    fun writeMcqsPdf(
        outputStream: OutputStream,
        title: String,
        subtitle: String,
        questions: List<McqQuestion>
    ) {
        val document = PdfDocument()
        try {
            var pageNumber = 1
            var page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
            var canvas = page.canvas
            var y = MARGIN_TOP

            val headerBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#0D6E6E")
            }
            val headerTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 18f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            val headerSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#CCF0EC")
                textSize = 10.5f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            }
            val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#0D6E6E")
                textSize = 9.5f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            val stemPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#112024")
                textSize = 11.5f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#1E293B")
                textSize = 10.5f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            }
            val monoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#112024")
                textSize = 9.5f
                typeface = Typeface.MONOSPACE
            }
            val reasonBoxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#EEF7F6")
            }
            val reasonTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#084C52")
                textSize = 10f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#CBD9DA")
                strokeWidth = 0.8f
            }
            val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#64748B")
                textSize = 9f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            }

            fun drawFooter(c: Canvas, num: Int) {
                c.drawLine(MARGIN_X, PAGE_HEIGHT - 30f, PAGE_WIDTH - MARGIN_X, PAGE_HEIGHT - 30f, dividerPaint)
                c.drawText("MedPrep Interactive Study Bank • Offline PDF", MARGIN_X, PAGE_HEIGHT - 16f, footerPaint)
                val pageLabel = "Page $num"
                val w = footerPaint.measureText(pageLabel)
                c.drawText(pageLabel, PAGE_WIDTH - MARGIN_X - w, PAGE_HEIGHT - 16f, footerPaint)
            }

            fun ensureSpace(neededHeight: Float) {
                if (y + neededHeight > PAGE_HEIGHT - MARGIN_BOTTOM) {
                    drawFooter(canvas, pageNumber)
                    document.finishPage(page)
                    pageNumber += 1
                    page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
                    canvas = page.canvas
                    y = MARGIN_TOP
                }
            }

            // Cover Header Banner on Page 1
            val bannerRect = RectF(MARGIN_X, y, PAGE_WIDTH - MARGIN_X, y + 64f)
            canvas.drawRoundRect(bannerRect, 12f, 12f, headerBgPaint)
            canvas.drawText(title.take(54), MARGIN_X + 16f, y + 28f, headerTitlePaint)
            val dateStr = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault()).format(Date())
            canvas.drawText("$subtitle • ${questions.size} MCQs • $dateStr", MARGIN_X + 16f, y + 48f, headerSubPaint)
            y += 80f

            questions.forEachIndexed { idx, q ->
                val tagLine = buildString {
                    append("Q${idx + 1}  •  ${q.subject}")
                    if (q.topic.isNotBlank()) append("  •  ${q.topic}")
                    append("  [${q.difficulty.uppercase()}]")
                }

                val stemIsChem = isStructuralChemicalText(q.stem)
                val stemLines = wrapParagraphs(
                    text = q.stem,
                    paint = if (stemIsChem) monoPaint else stemPaint,
                    maxWidth = CONTENT_WIDTH,
                    preserveVerbatim = stemIsChem
                )

                val optionBlocks = listOf(
                    "A" to q.optionA,
                    "B" to q.optionB,
                    "C" to q.optionC,
                    "D" to q.optionD
                ).map { (letter, text) ->
                    val isChem = isStructuralChemicalText(text)
                    val prefix = if (isChem) "$letter)\n$text" else "$letter) $text"
                    val lines = wrapParagraphs(
                        text = prefix,
                        paint = if (isChem) monoPaint else bodyPaint,
                        maxWidth = CONTENT_WIDTH - 12f,
                        preserveVerbatim = isChem
                    )
                    Triple(letter, isChem, lines)
                }

                val reasonText = q.explanation.ifBlank { "Correct answer: Option ${q.correctOption}." }
                val reasonIsChem = isStructuralChemicalText(reasonText)
                val reasonLines = wrapParagraphs(
                    text = reasonText,
                    paint = if (reasonIsChem) monoPaint else bodyPaint,
                    maxWidth = CONTENT_WIDTH - 20f,
                    preserveVerbatim = reasonIsChem
                )

                // Estimate block height so we start a new page cleanly if near the bottom
                val estimatedHeight = 20f +
                    (stemLines.size * 15f) +
                    optionBlocks.sumOf { it.third.size * 14 + 4 }.toFloat() +
                    (reasonLines.size * 13.5f + 34f) +
                    16f

                if (estimatedHeight < PAGE_HEIGHT - MARGIN_TOP - MARGIN_BOTTOM) {
                    ensureSpace(estimatedHeight)
                } else {
                    ensureSpace(90f)
                }

                // Tag line
                canvas.drawText(tagLine, MARGIN_X, y, badgePaint)
                y += 15f

                // Stem lines
                val activeStemPaint = if (stemIsChem) monoPaint else stemPaint
                stemLines.forEach { line ->
                    ensureSpace(16f)
                    canvas.drawText(line, MARGIN_X, y, activeStemPaint)
                    y += 15f
                }
                y += 5f

                // Options A–D
                optionBlocks.forEach { (_, isChem, lines) ->
                    val optPaint = if (isChem) monoPaint else bodyPaint
                    lines.forEach { line ->
                        ensureSpace(15f)
                        canvas.drawText(line, MARGIN_X + 10f, y, optPaint)
                        y += 14f
                    }
                    y += 2f
                }
                y += 4f

                // Answer Key & Reason box
                val reasonBoxHeight = 22f + (reasonLines.size * 13.5f) + 8f
                ensureSpace(reasonBoxHeight + 12f)
                val boxRect = RectF(MARGIN_X, y, PAGE_WIDTH - MARGIN_X, y + reasonBoxHeight)
                canvas.drawRoundRect(boxRect, 8f, 8f, reasonBoxPaint)
                y += 15f
                canvas.drawText("Answer: Option ${q.correctOption.uppercase()}  —  Reason:", MARGIN_X + 10f, y, reasonTitlePaint)
                y += 14f
                val activeReasonPaint = if (reasonIsChem) monoPaint else bodyPaint
                reasonLines.forEach { line ->
                    canvas.drawText(line, MARGIN_X + 10f, y, activeReasonPaint)
                    y += 13.5f
                }
                y += 14f

                // Subtle separator between MCQs
                if (idx < questions.lastIndex) {
                    ensureSpace(12f)
                    canvas.drawLine(MARGIN_X, y, PAGE_WIDTH - MARGIN_X, y, dividerPaint)
                    y += 14f
                }
            }

            drawFooter(canvas, pageNumber)
            document.finishPage(page)
            document.writeTo(outputStream)
        } finally {
            document.close()
        }
    }

    private fun wrapParagraphs(
        text: String,
        paint: Paint,
        maxWidth: Float,
        preserveVerbatim: Boolean
    ): List<String> {
        val result = mutableListOf<String>()
        val rawLines = text.replace("\r\n", "\n").replace("\r", "\n").split("\n")

        for (rawLine in rawLines) {
            if (rawLine.isEmpty()) {
                result.add("")
                continue
            }
            if (preserveVerbatim) {
                // Keep leading indentation intact; only split if line exceeds printable width
                if (paint.measureText(rawLine) <= maxWidth) {
                    result.add(rawLine)
                } else {
                    var start = 0
                    while (start < rawLine.length) {
                        val count = paint.breakText(rawLine, start, rawLine.length, true, maxWidth, null)
                            .coerceAtLeast(1)
                        result.add(rawLine.substring(start, start + count))
                        start += count
                    }
                }
            } else {
                val words = rawLine.trim().split(Regex("\\s+"))
                var current = StringBuilder()
                for (word in words) {
                    val candidate = if (current.isEmpty()) word else "$current $word"
                    if (paint.measureText(candidate) <= maxWidth) {
                        current = StringBuilder(candidate)
                    } else {
                        if (current.isNotEmpty()) {
                            result.add(current.toString())
                        }
                        if (paint.measureText(word) <= maxWidth) {
                            current = StringBuilder(word)
                        } else {
                            // Break very long token/URL cleanly
                            var start = 0
                            while (start < word.length) {
                                val count = paint.breakText(word, start, word.length, true, maxWidth, null)
                                    .coerceAtLeast(1)
                                val slice = word.substring(start, start + count)
                                if (start + count < word.length) {
                                    result.add(slice)
                                } else {
                                    current = StringBuilder(slice)
                                }
                                start += count
                            }
                        }
                    }
                }
                if (current.isNotEmpty()) {
                    result.add(current.toString())
                }
            }
        }
        return if (result.isEmpty()) listOf("") else result
    }
}
