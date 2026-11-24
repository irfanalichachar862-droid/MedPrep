package com.example.data

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.zip.ZipInputStream

/**
 * Pure-Kotlin reader for Microsoft Excel (.xlsx) workbooks and CSV/TSV files.
 * Extracts xl/sharedStrings.xml and the first worksheet XML directly from the .xlsx ZIP archive
 * with zero third-party binary overhead.
 */
object SpreadsheetMcqReader {

    fun parseStream(
        inputStream: InputStream,
        fileName: String?,
        defaultStatus: String = "published"
    ): CsvParseResult {
        val bytes = inputStream.readBytes()
        if (bytes.isEmpty()) {
            return CsvParseResult(emptyList(), listOf("Selected file is empty."))
        }

        // ZIP magic bytes (PK\x03\x04) identify .xlsx workbooks reliably regardless of filename
        val isZip = bytes.size >= 4 &&
            bytes[0] == 0x50.toByte() &&
            bytes[1] == 0x4B.toByte() &&
            bytes[2] == 0x03.toByte() &&
            bytes[3] == 0x04.toByte()

        val looksXlsx = isZip || (fileName?.endsWith(".xlsx", ignoreCase = true) == true)
        return if (looksXlsx) {
            parseXlsxBytes(bytes, defaultStatus)
        } else {
            val text = bytes.toString(Charsets.UTF_8)
            CsvMcqParser.parseCsv(text, defaultStatus)
        }
    }

    fun parseXlsxBytes(bytes: ByteArray, defaultStatus: String = "published"): CsvParseResult {
        return try {
            var sharedStringsBytes: ByteArray? = null
            var sheetBytes: ByteArray? = null
            var firstWorksheetName: String? = null

            ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val name = entry.name.lowercase()
                    when {
                        name == "xl/sharedstrings.xml" -> {
                            sharedStringsBytes = zis.readBytes()
                        }
                        name == "xl/worksheets/sheet1.xml" -> {
                            sheetBytes = zis.readBytes()
                            firstWorksheetName = name
                        }
                        name.startsWith("xl/worksheets/sheet") && name.endsWith(".xml") && sheetBytes == null -> {
                            sheetBytes = zis.readBytes()
                            firstWorksheetName = name
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }

            val worksheet = sheetBytes
                ?: return CsvParseResult(
                    emptyList(),
                    listOf("Could not find a worksheet inside this .xlsx Excel file. Save as .xlsx or .csv and retry.")
                )

            val sharedStrings = sharedStringsBytes?.let { parseSharedStrings(it) }.orEmpty()
            val rows = parseWorksheetRows(worksheet, sharedStrings)
            if (rows.isEmpty()) {
                return CsvParseResult(
                    emptyList(),
                    listOf("Worksheet (${firstWorksheetName ?: "sheet1"}) contains no data rows.")
                )
            }

            CsvMcqParser.parseRows(rows, defaultStatus)
        } catch (e: Exception) {
            CsvParseResult(
                emptyList(),
                listOf("Could not read Excel file: ${e.localizedMessage ?: "Invalid .xlsx format"}")
            )
        }
    }

    private fun parseSharedStrings(xmlBytes: ByteArray): List<String> {
        val strings = mutableListOf<String>()
        val parser = Xml.newPullParser()
        parser.setInput(ByteArrayInputStream(xmlBytes), "UTF-8")

        var inSi = false
        var inText = false
        val buffer = StringBuilder()

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "si" -> {
                            inSi = true
                            buffer.setLength(0)
                        }
                        "t" -> if (inSi) inText = true
                    }
                }
                XmlPullParser.TEXT -> {
                    if (inSi && inText) buffer.append(parser.text.orEmpty())
                }
                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        "t" -> inText = false
                        "si" -> {
                            strings.add(buffer.toString())
                            inSi = false
                        }
                    }
                }
            }
            event = parser.next()
        }
        return strings
    }

    private fun parseWorksheetRows(xmlBytes: ByteArray, sharedStrings: List<String>): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val parser = Xml.newPullParser()
        parser.setInput(ByteArrayInputStream(xmlBytes), "UTF-8")

        var currentRow = mutableListOf<String>()
        var cellType = ""
        var cellColIndex = -1
        var inValue = false
        var inInlineText = false
        val valueBuf = StringBuilder()

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "row" -> currentRow = mutableListOf()
                        "c" -> {
                            cellType = parser.getAttributeValue(null, "t").orEmpty()
                            val ref = parser.getAttributeValue(null, "r").orEmpty()
                            cellColIndex = columnIndexFromRef(ref).takeIf { it >= 0 } ?: currentRow.size
                            valueBuf.setLength(0)
                        }
                        "v" -> inValue = true
                        "t" -> if (cellType == "inlineStr") inInlineText = true
                    }
                }
                XmlPullParser.TEXT -> {
                    if (inValue || inInlineText) {
                        valueBuf.append(parser.text.orEmpty())
                    }
                }
                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        "v" -> inValue = false
                        "t" -> inInlineText = false
                        "c" -> {
                            val raw = valueBuf.toString()
                            val resolved = when (cellType) {
                                "s" -> raw.toIntOrNull()?.let { sharedStrings.getOrNull(it) }.orEmpty()
                                else -> raw
                            }
                            if (cellColIndex >= 0) {
                                while (currentRow.size <= cellColIndex) {
                                    currentRow.add("")
                                }
                                currentRow[cellColIndex] = resolved
                            }
                        }
                        "row" -> {
                            if (currentRow.any { it.isNotBlank() }) {
                                rows.add(currentRow.toList())
                            }
                        }
                    }
                }
            }
            event = parser.next()
        }
        return rows
    }

    private fun columnIndexFromRef(ref: String): Int {
        if (ref.isBlank()) return -1
        var index = 0
        var seenLetters = false
        for (ch in ref) {
            if (ch in 'A'..'Z') {
                index = index * 26 + (ch - 'A' + 1)
                seenLetters = true
            } else if (ch in 'a'..'z') {
                index = index * 26 + (ch - 'a' + 1)
                seenLetters = true
            } else {
                break
            }
        }
        return if (seenLetters) index - 1 else -1
    }

    fun exportQuestionsToCsv(questions: List<McqQuestion>): String {
        val sb = StringBuilder()
        sb.appendLine("subject,topic,question,option_a,option_b,option_c,option_d,correct,reason,youtube_url,difficulty,status")
        questions.forEach { q ->
            val fields = listOf(
                q.subject,
                q.topic,
                q.stem,
                q.optionA,
                q.optionB,
                q.optionC,
                q.optionD,
                q.correctOption,
                q.explanation,
                q.youtubeUrl.orEmpty(),
                q.difficulty,
                q.status
            )
            sb.appendLine(fields.joinToString(",") { escapeCsv(it) })
        }
        return sb.toString()
    }

    fun exportQuestionsToStudyText(questions: List<McqQuestion>, heading: String = "MedPrep Offline MCQ Bank"): String {
        val sb = StringBuilder()
        sb.appendLine("====================================================")
        sb.appendLine(heading)
        sb.appendLine("Total MCQs: ${questions.size}")
        sb.appendLine("Subjects: Biology | Chemistry | Physics | English | Logical Reasoning")
        sb.appendLine("====================================================")
        sb.appendLine()

        questions.forEachIndexed { idx, q ->
            sb.appendLine("Q${idx + 1}. [${q.subject}${if (q.topic.isNotBlank()) " • ${q.topic}" else ""}] (${q.difficulty.uppercase()})")
            sb.appendLine(q.stem)
            sb.appendLine("   A) ${q.optionA}")
            sb.appendLine("   B) ${q.optionB}")
            sb.appendLine("   C) ${q.optionC}")
            sb.appendLine("   D) ${q.optionD}")
            sb.appendLine("   Correct Answer: ${q.correctOption}")
            if (q.explanation.isNotBlank()) {
                sb.appendLine("   Reason: ${q.explanation}")
            }
            if (!q.youtubeUrl.isNullOrBlank()) {
                sb.appendLine("   YouTube Video: ${q.youtubeUrl}")
            }
            sb.appendLine("----------------------------------------------------")
        }
        return sb.toString()
    }

    private fun escapeCsv(value: String): String {
        val needsQuotes = value.contains(',') || value.contains('"') || value.contains('\n') || value.contains('\r')
        return if (needsQuotes) "\"${value.replace("\"", "\"\"")}\"" else value
    }
}
