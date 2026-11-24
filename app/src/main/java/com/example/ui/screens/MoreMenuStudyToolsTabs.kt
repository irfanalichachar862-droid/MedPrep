package com.example.ui.screens

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BookmarkAdded
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PriorityHigh
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.DifficultQuestionDraft
import com.example.data.DifficultQuestionItem
import com.example.data.DownloadedMcqPack
import com.example.data.McqQuestion
import com.example.notifications.MedPrepNotificationHelper
import com.example.data.MedPrepBackupPayload
import com.example.data.MedPrepConstants
import com.example.data.OfflinePackStorage
import com.example.data.PdfMcqExporter
import com.example.data.StudyNote
import com.example.data.StudyNoteDraft
import com.example.data.UserProfile
import com.example.ui.components.ChemistryFormulaToolbar
import com.example.ui.components.FormulaText
import com.example.ui.components.LiquidGlassSurface
import com.example.ui.theme.AppAppearanceConfig
import com.example.ui.theme.AppFunctionalSettings
import com.example.ui.theme.LocalMedPrepColors
import com.example.ui.theme.MonoFontFamily
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// --- TAB 5: HIGHLIGHTS & NOTES + DOWNLOADED INTERACTIVE MCQS & PDF EXPORT ---

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HighlightsNotesTabContent(
    publishedQuestions: List<McqQuestion>,
    studyNotes: List<StudyNote>,
    downloadedPacks: List<DownloadedMcqPack>,
    isMutating: Boolean,
    onStartPackQuiz: (DownloadedMcqPack) -> Unit,
    onSaveOfflinePack: (String, String, List<McqQuestion>) -> Unit,
    onDeleteOfflinePack: (String) -> Unit,
    onSaveNote: (StudyNoteDraft, () -> Unit) -> Unit,
    onDeleteNote: (String) -> Unit,
    onPostMessage: (String) -> Unit
) {
    val context = LocalContext.current
    var selectedSubject by remember { mutableStateOf("All Subjects") }
    var pendingPdfQuestions by remember { mutableStateOf<Pair<String, List<McqQuestion>>?>(null) }
    var readingPack by remember { mutableStateOf<DownloadedMcqPack?>(null) }
    var showAddNoteDialog by remember { mutableStateOf(false) }

    val filteredQuestions = remember(publishedQuestions, selectedSubject) {
        if (selectedSubject == "All Subjects") publishedQuestions
        else publishedQuestions.filter { it.subject.equals(selectedSubject, ignoreCase = true) }
    }

    val pdfExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri: Uri? ->
        val payload = pendingPdfQuestions
        if (uri != null && payload != null) {
            try {
                val (packTitle, list) = payload
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    PdfMcqExporter.writeMcqsPdf(
                        outputStream = out,
                        title = "MedPrep — $packTitle",
                        subtitle = "5-Subject Interactive MCQ Bank",
                        questions = list
                    )
                }
                // Also save as interactive offline pack inside the app!
                onSaveOfflinePack(packTitle, selectedSubject, list)
                onPostMessage("Saved PDF + Interactive Offline Pack (${list.size} MCQs)!")
            } catch (e: Exception) {
                onPostMessage("PDF export failed: ${e.localizedMessage ?: "Error"}")
            }
        }
        pendingPdfQuestions = null
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Download MCQs as PDF & Interactive In-App Offline Packs
        item {
            LiquidGlassSurface(shape = MaterialTheme.shapes.extraLarge) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column {
                            Text("Download MCQs (PDF + Interactive App Mode)", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Export clean multi-page PDF & run downloaded MCQs interactively inside the app",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        (listOf("All Subjects") + MedPrepConstants.DEFAULT_SUBJECTS).forEach { subj ->
                            FilterChip(
                                selected = selectedSubject == subj,
                                onClick = { selectedSubject = subj },
                                label = { Text(subj) }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val title = "$selectedSubject MCQs"
                                pendingPdfQuestions = title to filteredQuestions
                                val safeName = selectedSubject.replace(" ", "_")
                                pdfExportLauncher.launch("MedPrep_${safeName}_MCQs.pdf")
                            },
                            enabled = filteredQuestions.isNotEmpty(),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("notes_download_pdf_button")
                        ) {
                            Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("PDF (${filteredQuestions.size})")
                        }
                        OutlinedButton(
                            onClick = {
                                onSaveOfflinePack("$selectedSubject Interactive Pack", selectedSubject, filteredQuestions)
                            },
                            enabled = filteredQuestions.isNotEmpty(),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("notes_save_offline_pack_button")
                        ) {
                            Icon(Icons.Rounded.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save In-App")
                        }
                    }
                }
            }
        }

        // 2. Saved Offline Interactive Packs (Run Interactively Inside the App!)
        item {
            Text(
                text = "Downloaded Interactive Packs (${downloadedPacks.size})",
                style = MaterialTheme.typography.titleMedium
            )
        }

        if (downloadedPacks.isEmpty()) {
            item {
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No downloaded packs yet. Tap PDF or Save In-App above to download MCQs and run them interactively offline anytime.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        } else {
            items(downloadedPacks, key = { it.id }) { pack ->
                val dateLabel = remember(pack.downloadedAtMillis) {
                    SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(pack.downloadedAtMillis))
                }
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(pack.title, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = "${pack.questions.size} MCQs • ${pack.subject} • Saved $dateLabel",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { onDeleteOfflinePack(pack.id) }) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = "Delete pack",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { onStartPackQuiz(pack) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("pack_interactive_play_${pack.id}")
                            ) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(17.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Interactive Quiz")
                            }
                            OutlinedButton(
                                onClick = { readingPack = pack },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("pack_read_sheet_${pack.id}")
                            ) {
                                Icon(Icons.Rounded.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Read MCQs")
                            }
                        }
                    }
                }
            }
        }

        // 3. Personal Highlights & Study Notes
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "My Highlights & Notes (${studyNotes.size})",
                    style = MaterialTheme.typography.titleMedium
                )
                Button(
                    onClick = { showAddNoteDialog = true },
                    modifier = Modifier.testTag("add_highlight_note_button")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Note")
                }
            }
        }

        if (studyNotes.isEmpty()) {
            item {
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Save high-yield highlights, mnemonics, or chemical formulas here or right inside any quiz session.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        } else {
            items(studyNotes, key = { it.id }) { note ->
                val accent = noteHighlightColor(note.highlightColor)
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.12f)),
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = accent.copy(alpha = 0.22f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = note.subject,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            IconButton(
                                onClick = { onDeleteNote(note.id) },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = "Delete note",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Text(note.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        FormulaText(text = note.content, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(20.dp)) }
    }

    // Interactive In-App Reader Sheet for Downloaded MCQs
    val activePack = readingPack
    if (activePack != null) {
        ModalBottomSheet(onDismissRequest = { readingPack = null }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(activePack.title, style = MaterialTheme.typography.titleLarge)
                        Text(
                            "${activePack.questions.size} MCQs • Tap any question to reveal Answer & Reason",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = {
                            val pack = activePack
                            readingPack = null
                            onStartPackQuiz(pack)
                        }
                    ) {
                        Text("Quiz Mode")
                    }
                }

                LazyColumn(
                    modifier = Modifier.height(460.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(activePack.questions.indices.toList(), key = { idx -> "${activePack.id}_$idx" }) { idx ->
                        InteractiveReaderMcqCard(index = idx + 1, question = activePack.questions[idx])
                    }
                }
            }
        }
    }

    if (showAddNoteDialog) {
        AddNoteDialog(
            isMutating = isMutating,
            onDismiss = { showAddNoteDialog = false },
            onSave = { draft ->
                onSaveNote(draft) { showAddNoteDialog = false }
            }
        )
    }
}

@Composable
private fun InteractiveReaderMcqCard(index: Int, question: McqQuestion) {
    var revealed by remember(question.id) { mutableStateOf(false) }
    val ext = LocalMedPrepColors.current

    Card(
        onClick = { revealed = !revealed },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Q$index • ${question.subject}${if (question.topic.isNotBlank()) " • ${question.topic}" else ""}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    imageVector = Icons.Rounded.Visibility,
                    contentDescription = "Reveal answer",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
            FormulaText(text = question.stem, style = MaterialTheme.typography.titleSmall)
            listOf("A" to question.optionA, "B" to question.optionB, "C" to question.optionC, "D" to question.optionD).forEach { (l, txt) ->
                val isAns = revealed && l.equals(question.correctOption, ignoreCase = true)
                Text(
                    text = "$l) $txt",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (isAns) FontWeight.Bold else FontWeight.Normal,
                    color = if (isAns) ext.success else MaterialTheme.colorScheme.onSurface
                )
            }
            AnimatedVisibility(visible = revealed) {
                Surface(
                    color = ext.successContainer.copy(alpha = 0.55f),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Correct Answer: Option ${question.correctOption}",
                            style = MaterialTheme.typography.labelMedium,
                            color = ext.onSuccessContainer,
                            fontWeight = FontWeight.Bold
                        )
                        FormulaText(
                            text = question.explanation.ifBlank { "No reason provided." },
                            style = MaterialTheme.typography.bodySmall,
                            color = ext.onSuccessContainer
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddNoteDialog(
    initialSubject: String = "Biology",
    initialTitle: String = "",
    initialContent: String = "",
    linkedQuestionId: String? = null,
    isMutating: Boolean,
    onDismiss: () -> Unit,
    onSave: (StudyNoteDraft) -> Unit
) {
    var subject by remember { mutableStateOf(initialSubject) }
    var title by remember { mutableStateOf(initialTitle) }
    var content by remember { mutableStateOf(initialContent) }
    var colorKey by remember { mutableStateOf("amber") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save Highlight / Study Note") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MedPrepConstants.DEFAULT_SUBJECTS.forEach { subj ->
                        FilterChip(
                            selected = subject == subj,
                            onClick = { subject = subj },
                            label = { Text(subj) }
                        )
                    }
                }
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(160) },
                    label = { Text("Note / Highlight Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it.take(4000) },
                    label = { Text("Key Concept, Formula, or Mnemonic") },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf("teal", "amber", "rose", "indigo", "emerald").forEach { key ->
                        val c = noteHighlightColor(key)
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(c)
                                .clickable { colorKey = key },
                            contentAlignment = Alignment.Center
                        ) {
                            if (colorKey == key) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        StudyNoteDraft(
                            title = title.trim(),
                            content = content,
                            subject = subject,
                            questionId = linkedQuestionId,
                            highlightColor = colorKey
                        )
                    )
                },
                enabled = !isMutating && title.trim().isNotEmpty() && content.trim().isNotEmpty()
            ) {
                Text("Save Note")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun noteHighlightColor(key: String): Color = when (key.lowercase()) {
    "amber" -> Color(0xFFD97706)
    "rose" -> Color(0xFFBE185D)
    "indigo" -> Color(0xFF4F46E5)
    "emerald" -> Color(0xFF059669)
    else -> Color(0xFF0D6E6E)
}

// --- TAB 6: DIFFICULT QUESTIONS (ADD DIFFICULT EXAM QUESTIONS + PRACTICE + PDF) ---

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DifficultQuestionsTabContent(
    publishedQuestions: List<McqQuestion>,
    personalDifficult: List<DifficultQuestionItem>,
    isMutating: Boolean,
    onPracticeDifficult: (List<McqQuestion>) -> Unit,
    onSaveDifficultQuestion: (DifficultQuestionDraft, () -> Unit) -> Unit,
    onDeleteDifficultQuestion: (String) -> Unit,
    onPostMessage: (String) -> Unit
) {
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }

    val bankHardQuestions = remember(publishedQuestions) {
        publishedQuestions.filter { it.difficulty.equals("hard", ignoreCase = true) }
    }
    val combinedDifficult = remember(personalDifficult, bankHardQuestions) {
        val personalAsMcq = personalDifficult.map { it.toMcqQuestion() }
        val existingIds = personalAsMcq.map { it.id }.toSet()
        personalAsMcq + bankHardQuestions.filterNot { it.id in existingIds }
    }

    val pdfDifficultLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri: Uri? ->
        if (uri != null && combinedDifficult.isNotEmpty()) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    PdfMcqExporter.writeMcqsPdf(
                        outputStream = out,
                        title = "MedPrep — Difficult Exam Questions",
                        subtitle = "High-Difficulty Revision Vault",
                        questions = combinedDifficult
                    )
                }
                onPostMessage("Exported ${combinedDifficult.size} Difficult Questions to PDF!")
            } catch (e: Exception) {
                onPostMessage("PDF export failed: ${e.localizedMessage ?: "Error"}")
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            LiquidGlassSurface(shape = MaterialTheme.shapes.extraLarge) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.PriorityHigh, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Column {
                            Text("Difficult Exam Questions Vault", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${personalDifficult.size} student-added/marked + ${bankHardQuestions.size} hard exam bank MCQs",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onPracticeDifficult(combinedDifficult) },
                            enabled = combinedDifficult.isNotEmpty(),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("difficult_practice_button")
                        ) {
                            Icon(Icons.Rounded.Shuffle, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Practice (${combinedDifficult.size})")
                        }
                        OutlinedButton(
                            onClick = { showAddDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("difficult_add_question_button")
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Difficult Q")
                        }
                    }

                    OutlinedButton(
                        onClick = { pdfDifficultLauncher.launch("MedPrep_Difficult_Questions.pdf") },
                        enabled = combinedDifficult.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    ) {
                        Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Download Difficult Questions PDF")
                    }
                }
            }
        }

        if (personalDifficult.isNotEmpty()) {
            item {
                Text("My Saved Difficult Questions (${personalDifficult.size})", style = MaterialTheme.typography.titleMedium)
            }
            items(personalDifficult, key = { it.id }) { item ->
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${item.subject}${if (item.topic.isNotBlank()) " • ${item.topic}" else ""}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { onDeleteDifficultQuestion(item.id) },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                        FormulaText(text = item.stem, style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = "Answer: Option ${item.correctOption} — ${item.explanation.ifBlank { "Review carefully before exams." }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (bankHardQuestions.isNotEmpty()) {
            item {
                Text("High-Difficulty Bank Questions (${bankHardQuestions.size})", style = MaterialTheme.typography.titleMedium)
            }
            items(bankHardQuestions, key = { it.id }) { q ->
                InteractiveReaderMcqCard(index = bankHardQuestions.indexOf(q) + 1, question = q)
            }
        }

        item { Spacer(modifier = Modifier.height(20.dp)) }
    }

    if (showAddDialog) {
        AddDifficultQuestionDialog(
            isMutating = isMutating,
            onDismiss = { showAddDialog = false },
            onSave = { draft ->
                onSaveDifficultQuestion(draft) { showAddDialog = false }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddDifficultQuestionDialog(
    isMutating: Boolean,
    onDismiss: () -> Unit,
    onSave: (DifficultQuestionDraft) -> Unit
) {
    var subject by remember { mutableStateOf("Chemistry") }
    var topic by remember { mutableStateOf("") }
    var stem by remember { mutableStateOf("") }
    var optA by remember { mutableStateOf("") }
    var optB by remember { mutableStateOf("") }
    var optC by remember { mutableStateOf("") }
    var optD by remember { mutableStateOf("") }
    var correct by remember { mutableStateOf("A") }
    var explanation by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Difficult Exam Question") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MedPrepConstants.DEFAULT_SUBJECTS.forEach { subj ->
                        FilterChip(
                            selected = subject == subj,
                            onClick = { subject = subj },
                            label = { Text(subj) }
                        )
                    }
                }
                OutlinedTextField(
                    value = topic,
                    onValueChange = { topic = it.take(120) },
                    label = { Text("Topic (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (subject.equals("Chemistry", ignoreCase = true)) {
                    ChemistryFormulaToolbar(onInsertSymbol = { sym -> stem += sym })
                }
                OutlinedTextField(
                    value = stem,
                    onValueChange = { stem = it.take(2000) },
                    label = { Text("Question Stem (exact chemistry formulas preserved)") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(value = optA, onValueChange = { optA = it.take(1000) }, label = { Text("Option A") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = optB, onValueChange = { optB = it.take(1000) }, label = { Text("Option B") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = optC, onValueChange = { optC = it.take(1000) }, label = { Text("Option C") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = optD, onValueChange = { optD = it.take(1000) }, label = { Text("Option D") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("A", "B", "C", "D").forEach { letter ->
                        FilterChip(
                            selected = correct == letter,
                            onClick = { correct = letter },
                            label = { Text("Key: $letter") }
                        )
                    }
                }
                OutlinedTextField(
                    value = explanation,
                    onValueChange = { explanation = it.take(3000) },
                    label = { Text("Reason / Exam Trick") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        DifficultQuestionDraft(
                            subject = subject,
                            topic = topic,
                            stem = stem,
                            optionA = optA,
                            optionB = optB,
                            optionC = optC,
                            optionD = optD,
                            correctOption = correct,
                            explanation = explanation
                        )
                    )
                },
                enabled = !isMutating && stem.trim().length >= 5 &&
                    optA.isNotBlank() && optB.isNotBlank() && optC.isNotBlank() && optD.isNotBlank()
            ) {
                Text("Add to Vault")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// --- TAB 7: SETTINGS & BACKUP (ALL APP SETTINGS + EXPORT/IMPORT BACKUP + BOTTOM ADMIN LOGIN) ---

@Composable
fun SettingsBackupTabContent(
    profile: UserProfile?,
    appearance: AppAppearanceConfig,
    settings: AppFunctionalSettings,
    studyNotes: List<StudyNote>,
    difficultQuestions: List<DifficultQuestionItem>,
    downloadedPacks: List<DownloadedMcqPack>,
    isAdminUser: Boolean,
    isSyncing: Boolean,
    isMutating: Boolean,
    onUpdateSettings: (AppFunctionalSettings) -> Unit,
    onSyncNow: () -> Unit,
    onImportBackup: (MedPrepBackupPayload) -> Unit,
    onOpenAdminPanel: (() -> Unit)?,
    onSignOut: () -> Unit,
    onPostMessage: (String) -> Unit
) {
    val context = LocalContext.current

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val updated = settings.copy(notificationsEnabled = true)
            onUpdateSettings(updated)
            MedPrepNotificationHelper.syncSchedule(context, updated)
            onPostMessage("Study notifications enabled!")
        } else {
            onPostMessage("Notification permission denied. You can enable it anytime in Android Settings.")
        }
    }

    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val json = OfflinePackStorage.serializeBackup(
                    profile = profile,
                    appearance = appearance,
                    settings = settings,
                    notes = studyNotes,
                    difficultQuestions = difficultQuestions,
                    offlinePacks = downloadedPacks
                )
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(json.toByteArray(Charsets.UTF_8))
                }
                onPostMessage("Full MedPrep Backup exported successfully!")
            } catch (e: Exception) {
                onPostMessage("Export failed: ${e.localizedMessage ?: "Error"}")
            }
        }
    }

    val importBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val raw = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                if (raw.isNullOrBlank()) {
                    onPostMessage("Selected backup file is empty.")
                } else {
                    OfflinePackStorage.parseBackup(raw)
                        .onSuccess { payload -> onImportBackup(payload) }
                        .onFailure { err -> onPostMessage(err.localizedMessage ?: "Invalid backup file.") }
                }
            } catch (e: Exception) {
                onPostMessage("Import failed: ${e.localizedMessage ?: "Error"}")
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Quiz & Study Behavior Settings
        item {
            LiquidGlassSurface(shape = MaterialTheme.shapes.extraLarge) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Quiz & App Functional Settings", style = MaterialTheme.typography.titleMedium)

                    SettingToggleRow(
                        title = "Randomize A–D Options",
                        subtitle = "Shuffle option positions on every quiz & retest",
                        checked = settings.shuffleOptions,
                        tag = "setting_shuffle_options",
                        onCheckedChange = { onUpdateSettings(settings.copy(shuffleOptions = it)) }
                    )

                    SettingToggleRow(
                        title = "Instant Reason After Answer",
                        subtitle = "Reveal why the correct option is right immediately in Practice",
                        checked = settings.showInstantReason,
                        tag = "setting_instant_reason",
                        onCheckedChange = { onUpdateSettings(settings.copy(showInstantReason = it)) }
                    )

                    SettingToggleRow(
                        title = "Exact Chemistry Formula Mode",
                        subtitle = "Keep chemical structure spacing & alignment verbatim",
                        checked = settings.monospaceChemistry,
                        tag = "setting_mono_chem",
                        onCheckedChange = { onUpdateSettings(settings.copy(monospaceChemistry = it)) }
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Timed Exam Pace: ${settings.examSecondsPerQuestion}s per MCQ",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(45, 60, 90, 120).forEach { secs ->
                                val selected = settings.examSecondsPerQuestion == secs
                                Surface(
                                    onClick = { onUpdateSettings(settings.copy(examSecondsPerQuestion = secs)) },
                                    shape = MaterialTheme.shapes.small,
                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.weight(1f).height(42.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "${secs}s",
                                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoFontFamily),
                                            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = onSyncNow,
                        enabled = !isSyncing,
                        modifier = Modifier.fillMaxWidth().height(46.dp)
                    ) {
                        Icon(Icons.Rounded.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isSyncing) "Syncing Cloud Bank…" else "Sync Cloud Bank & Offline Cache Now")
                    }
                }
            }
        }

        // 1B. Notifications & Study Reminders
        item {
            LiquidGlassSurface(shape = MaterialTheme.shapes.extraLarge) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (settings.notificationsEnabled) Icons.Rounded.NotificationsActive else Icons.Rounded.Notifications,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text("Notifications & Study Reminders", style = MaterialTheme.typography.titleMedium)
                    }

                    SettingToggleRow(
                        title = "Enable Study Notifications",
                        subtitle = "Allow daily practice reminders, new course alerts & exam streaks",
                        checked = settings.notificationsEnabled,
                        tag = "setting_notifications_enabled",
                        onCheckedChange = { enable ->
                            if (enable) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                    !MedPrepNotificationHelper.hasNotificationPermission(context)
                                ) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    val updated = settings.copy(notificationsEnabled = true)
                                    onUpdateSettings(updated)
                                    MedPrepNotificationHelper.syncSchedule(context, updated)
                                    onPostMessage("Notifications enabled (${MedPrepNotificationHelper.formatReminderHour(updated.reminderHour)} daily).")
                                }
                            } else {
                                val updated = settings.copy(notificationsEnabled = false)
                                onUpdateSettings(updated)
                                MedPrepNotificationHelper.syncSchedule(context, updated)
                                onPostMessage("Notifications turned off.")
                            }
                        }
                    )

                    AnimatedVisibility(visible = settings.notificationsEnabled) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            SettingToggleRow(
                                title = "Daily Study Reminder",
                                subtitle = "Remind me every day at ${MedPrepNotificationHelper.formatReminderHour(settings.reminderHour)}",
                                checked = settings.dailyStudyReminder,
                                tag = "setting_notif_daily",
                                onCheckedChange = { checked ->
                                    val updated = settings.copy(dailyStudyReminder = checked)
                                    onUpdateSettings(updated)
                                    MedPrepNotificationHelper.syncSchedule(context, updated)
                                }
                            )

                            SettingToggleRow(
                                title = "New Courses & Posts Alerts",
                                subtitle = "Notify when Admin publishes new Courses, Posts, or HTML Lessons",
                                checked = settings.newCoursePostAlerts,
                                tag = "setting_notif_courses",
                                onCheckedChange = { checked ->
                                    onUpdateSettings(settings.copy(newCoursePostAlerts = checked))
                                }
                            )

                            SettingToggleRow(
                                title = "Exam & Goal Streak Alerts",
                                subtitle = "Keep your daily MCQ target & exam countdown on track",
                                checked = settings.examStreakAlerts,
                                tag = "setting_notif_exam",
                                onCheckedChange = { checked ->
                                    onUpdateSettings(settings.copy(examStreakAlerts = checked))
                                }
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Daily Reminder Time",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf(7, 14, 20, 22).forEach { hour ->
                                        val selected = settings.reminderHour == hour
                                        Surface(
                                            onClick = {
                                                val updated = settings.copy(reminderHour = hour)
                                                onUpdateSettings(updated)
                                                MedPrepNotificationHelper.syncSchedule(context, updated)
                                                onPostMessage("Daily reminder set for ${MedPrepNotificationHelper.formatReminderHour(hour)}")
                                            },
                                            shape = MaterialTheme.shapes.small,
                                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(44.dp)
                                                .testTag("setting_notif_hour_$hour")
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = MedPrepNotificationHelper.formatReminderHour(hour),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    val sent = MedPrepNotificationHelper.showNotification(
                                        context = context,
                                        title = "MedPrep Study Reminder",
                                        body = "Notifications are active! Time to practice Biology, Chemistry, Physics, English & Logical Reasoning."
                                    )
                                    onPostMessage(
                                        if (sent) "Test notification sent!"
                                        else "Grant notification permission in Android Settings to view alerts."
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("setting_send_test_notification")
                            ) {
                                Icon(Icons.Rounded.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Send Test Notification")
                            }
                        }
                    }
                }
            }
        }

        // 2. Import & Export Backup
        item {
            LiquidGlassSurface(shape = MaterialTheme.shapes.extraLarge) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Backup & Restore", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "Export or import your profile, appearance theme, settings, ${studyNotes.size} notes, ${difficultQuestions.size} difficult questions, and ${downloadedPacks.size} offline packs as a portable .json file.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { exportBackupLauncher.launch("MedPrep_Backup.json") },
                            enabled = !isMutating,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("settings_export_backup_button")
                        ) {
                            Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export Backup")
                        }
                        OutlinedButton(
                            onClick = { importBackupLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
                            enabled = !isMutating,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("settings_import_backup_button")
                        ) {
                            Icon(Icons.Rounded.FileUpload, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Import Backup")
                        }
                    }
                }
            }
        }

        // 3. Account & Discreet Admin Login / Portal Access (Strictly inside Settings & Backup, NEVER on top of screen)
        item {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Account & Admin Access", style = MaterialTheme.typography.titleMedium)

                    if (isAdminUser && onOpenAdminPanel != null) {
                        Surface(
                            onClick = onOpenAdminPanel,
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_open_admin_portal_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Image(
                                    painter = painterResource(R.drawable.admin_profile_photo),
                                    contentDescription = "Admin Portal",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(40.dp).clip(CircleShape)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Open Admin Portal",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Text(
                                        text = "Manage Excel MCQs, HTML lessons & Student Keys",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                    )
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                onPostMessage("Sign in with the verified Admin Google account (${MedPrepConstants.BOOTSTRAP_ADMIN_EMAIL}).")
                                onSignOut()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("settings_admin_login_button")
                        ) {
                            Icon(Icons.Rounded.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Admin Account Login")
                        }
                    }

                    OutlinedButton(
                        onClick = onSignOut,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("settings_sign_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Logout,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sign Out", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    tag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(tag)
        )
    }
}

// --- TAB 8: APP GUIDE (HOW TO USE MEDPREP) ---

private data class GuideStep(
    val number: String,
    val title: String,
    val description: String,
    val icon: ImageVector
)

@Composable
fun AppGuideTabContent() {
    val steps = remember {
        listOf(
            GuideStep(
                "01",
                "Student-Only 1-Device Access Key",
                "Sign in with Google and enter your unique Access Key provided by Admin. Your key locks securely to 1 device so your study progress stays protected.",
                Icons.Rounded.Key
            ),
            GuideStep(
                "02",
                "Master 5 Core Medical Subjects",
                "Practice Biology, Chemistry, Physics, English, and Logical Reasoning from the Study dashboard. Tap any subject card or start Quick Practice / Timed Exam.",
                Icons.Rounded.Science
            ),
            GuideStep(
                "03",
                "Shuffled A–D Options, Reasons & Retest",
                "Every attempt and Retest automatically randomizes both question order and A–D option positions. Select an answer to view the Reason and suggested YouTube video.",
                Icons.Rounded.Shuffle
            ),
            GuideStep(
                "04",
                "Interactive HTML Lessons Linked to MCQs",
                "Open the Posts tab (or tap 'Explain' inside any MCQ) to explore interactive HTML lessons uploaded by Admin, then tap 'Practice Linked MCQs' to test yourself immediately.",
                Icons.Rounded.Code
            ),
            GuideStep(
                "05",
                "Download MCQs as PDF + Interactive Offline Mode",
                "Download any subject or the full bank as a formatted multi-page PDF with exact Chemistry formulas. Each download also saves an Interactive Pack so you can run quizzes or read MCQs offline right inside MedPrep.",
                Icons.Rounded.PictureAsPdf
            ),
            GuideStep(
                "06",
                "Progress Tracker & Cover-in-N-Days Schedule",
                "Open Progress in the three-dots (⋮) menu to track accuracy per subject and generate a custom daily schedule to complete all remaining MCQs in 7, 15, 30, or any number of days.",
                Icons.Rounded.CalendarMonth
            ),
            GuideStep(
                "07",
                "Highlights, Notes & Difficult Exam Vault",
                "Save personal notes/highlights during quizzes or in Highlights & Notes. Flag tough MCQs or add your own Difficult Questions for focused pre-exam drills.",
                Icons.Rounded.BookmarkAdded
            ),
            GuideStep(
                "08",
                "Liquid Glass Themes, Plans & Full Backup",
                "Customize Dark/Light mode, 5 color palettes, font style, and text size under Appearance. Use Settings & Backup to export or import a full .json backup anytime.",
                Icons.Rounded.AutoAwesome
            )
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            LiquidGlassSurface(shape = MaterialTheme.shapes.extraLarge) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "How to Use MedPrep",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Complete step-by-step guide to every study tool inside the app.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        items(steps, key = { it.number }) { step ->
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = step.icon,
                            contentDescription = step.title,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = step.number,
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = step.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = step.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}
