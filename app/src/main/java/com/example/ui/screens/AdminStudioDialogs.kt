package com.example.ui.screens

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.CourseItem
import com.example.data.CsvMcqParser
import com.example.data.FormulaTextPreserver
import com.example.data.McqQuestion
import com.example.data.MedPrepConstants
import com.example.data.QuestionDraftInput
import com.example.data.StudyPost
import com.example.data.StudyPostDraftInput
import com.example.ui.components.ChemistryFormulaToolbar
import com.example.ui.components.isStructuralChemicalText
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.subjectVisualFor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun McqEditorBottomSheet(
    initialQuestion: McqQuestion?,
    availablePosts: List<StudyPost> = emptyList(),
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (QuestionDraftInput) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val initialValidSubject = initialQuestion?.subject?.takeIf { MedPrepConstants.isCoreSubject(it) }
        ?: MedPrepConstants.DEFAULT_SUBJECTS.first()

    var subject by remember { mutableStateOf(initialValidSubject) }
    var topic by remember { mutableStateOf(initialQuestion?.topic ?: "") }
    var linkedPostId by remember { mutableStateOf(initialQuestion?.linkedPostId.orEmpty()) }
    var stem by remember { mutableStateOf(initialQuestion?.stem ?: "") }
    var optionA by remember { mutableStateOf(initialQuestion?.optionA ?: "") }
    var optionB by remember { mutableStateOf(initialQuestion?.optionB ?: "") }
    var optionC by remember { mutableStateOf(initialQuestion?.optionC ?: "") }
    var optionD by remember { mutableStateOf(initialQuestion?.optionD ?: "") }
    var correctOption by remember { mutableStateOf(initialQuestion?.correctOption ?: "A") }
    var explanation by remember { mutableStateOf(initialQuestion?.explanation ?: "") }
    var youtubeUrl by remember { mutableStateOf(initialQuestion?.youtubeUrl ?: "") }
    var difficulty by remember { mutableStateOf(initialQuestion?.difficulty ?: "medium") }
    var publishImmediately by remember { mutableStateOf((initialQuestion?.status ?: "published") == "published") }
    var validationError by remember { mutableStateOf<String?>(null) }

    // Exact Formula Mode preserves exact monospace alignment & reveals the Chemistry symbol quick-insert bar
    var exactFormulaMode by remember {
        mutableStateOf(
            initialValidSubject.equals("Chemistry", ignoreCase = true) ||
                isStructuralChemicalText(initialQuestion?.stem.orEmpty()) ||
                isStructuralChemicalText(initialQuestion?.optionA.orEmpty())
        )
    }
    var activeField by remember { mutableStateOf("stem") }

    val matchingSubjectPosts = remember(availablePosts, subject) {
        availablePosts.filter { it.subject.equals(subject, ignoreCase = true) }
    }

    val formulaFieldStyle = if (exactFormulaMode) {
        MaterialTheme.typography.bodyMedium.copy(fontFamily = MonoFontFamily)
    } else {
        MaterialTheme.typography.bodyMedium
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (initialQuestion == null) "Create MCQ" else "Edit MCQ",
                style = MaterialTheme.typography.headlineSmall
            )

            Text("Select Subject (5 Core Subjects) *", style = MaterialTheme.typography.titleSmall)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MedPrepConstants.DEFAULT_SUBJECTS.forEach { subj ->
                    val visual = subjectVisualFor(subj)
                    FilterChip(
                        selected = subject.equals(subj, ignoreCase = true),
                        onClick = {
                            subject = subj
                            if (subj.equals("Chemistry", ignoreCase = true)) {
                                exactFormulaMode = true
                            }
                        },
                        leadingIcon = {
                            Icon(visual.icon, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        label = { Text(subj) }
                    )
                }
            }

            // Link MCQ to a Study Post / Interactive HTML Topic Explanation
            if (matchingSubjectPosts.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Link,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Link to Interactive Topic Explanation (Optional)",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = linkedPostId.isBlank(),
                                onClick = { linkedPostId = "" },
                                label = { Text("Auto by Topic") }
                            )
                            matchingSubjectPosts.forEach { post ->
                                FilterChip(
                                    selected = linkedPostId == post.id,
                                    onClick = {
                                        linkedPostId = post.id
                                        if (topic.isBlank() && post.topic.isNotBlank()) {
                                            topic = post.topic
                                        }
                                    },
                                    leadingIcon = if (post.hasInteractiveHtml) {
                                        { Icon(Icons.Rounded.Code, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                    } else null,
                                    label = {
                                        Text(
                                            text = post.topic.ifBlank { post.title },
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }

            OutlinedTextField(
                value = topic,
                onValueChange = { topic = it.take(120) },
                label = { Text("Topic / Chapter (links automatically with matching Study Posts)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Exact Chemistry Formula Preservation Toggle + Quick Symbol Bar
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Science,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text(
                                    text = "Exact Chemistry Formula Mode",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = "Preserves exact pasted spaces, line breaks & structural bonds.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = exactFormulaMode,
                            onCheckedChange = { exactFormulaMode = it }
                        )
                    }

                    if (exactFormulaMode) {
                        ChemistryFormulaToolbar(
                            onInsertSymbol = { sym ->
                                when (activeField) {
                                    "stem" -> stem = (stem + sym).take(2000)
                                    "optionA" -> optionA = (optionA + sym).take(1000)
                                    "optionB" -> optionB = (optionB + sym).take(1000)
                                    "optionC" -> optionC = (optionC + sym).take(1000)
                                    "optionD" -> optionD = (optionD + sym).take(1000)
                                    "explanation" -> explanation = (explanation + sym).take(3000)
                                }
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = stem,
                onValueChange = { stem = it.take(2000) },
                label = { Text("Question Stem * (multiline / exact formula paste supported)") },
                textStyle = formulaFieldStyle,
                minLines = 3,
                maxLines = 8,
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { if (it.isFocused) activeField = "stem" }
                    .testTag("editor_stem_input")
            )

            OutlinedTextField(
                value = optionA,
                onValueChange = { optionA = it.take(1000) },
                label = { Text("Option A * (exact formula structure preserved)") },
                textStyle = formulaFieldStyle,
                singleLine = false,
                minLines = 1,
                maxLines = 6,
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { if (it.isFocused) activeField = "optionA" }
                    .testTag("editor_option_a_input")
            )
            OutlinedTextField(
                value = optionB,
                onValueChange = { optionB = it.take(1000) },
                label = { Text("Option B * (exact formula structure preserved)") },
                textStyle = formulaFieldStyle,
                singleLine = false,
                minLines = 1,
                maxLines = 6,
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { if (it.isFocused) activeField = "optionB" }
                    .testTag("editor_option_b_input")
            )
            OutlinedTextField(
                value = optionC,
                onValueChange = { optionC = it.take(1000) },
                label = { Text("Option C * (exact formula structure preserved)") },
                textStyle = formulaFieldStyle,
                singleLine = false,
                minLines = 1,
                maxLines = 6,
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { if (it.isFocused) activeField = "optionC" }
                    .testTag("editor_option_c_input")
            )
            OutlinedTextField(
                value = optionD,
                onValueChange = { optionD = it.take(1000) },
                label = { Text("Option D * (exact formula structure preserved)") },
                textStyle = formulaFieldStyle,
                singleLine = false,
                minLines = 1,
                maxLines = 6,
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { if (it.isFocused) activeField = "optionD" }
                    .testTag("editor_option_d_input")
            )

            Text("Correct Answer *", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("A", "B", "C", "D").forEach { letter ->
                    FilterChip(
                        selected = correctOption == letter,
                        onClick = { correctOption = letter },
                        label = { Text("Option $letter") }
                    )
                }
            }

            OutlinedTextField(
                value = explanation,
                onValueChange = { explanation = it.take(3000) },
                label = { Text("Reason (Why Correct Answer Is Right)") },
                placeholder = { Text("Shown immediately after student selects an option…") },
                textStyle = formulaFieldStyle,
                minLines = 2,
                maxLines = 8,
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { if (it.isFocused) activeField = "explanation" }
                    .testTag("editor_reason_input")
            )

            OutlinedTextField(
                value = youtubeUrl,
                onValueChange = { youtubeUrl = it.take(500) },
                label = { Text("Suggested YouTube Video Link (Optional)") },
                placeholder = { Text("https://www.youtube.com/watch?v=...") },
                leadingIcon = { Icon(Icons.Rounded.PlayCircle, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("editor_youtube_input")
            )

            Text("Difficulty", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MedPrepConstants.DIFFICULTIES.forEach { diff ->
                    FilterChip(
                        selected = difficulty == diff,
                        onClick = { difficulty = diff },
                        label = { Text(diff.replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Publish Immediately", style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = if (publishImmediately) "Live in Student App & Offline Sync" else "Saved as Draft in Admin Panel",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = publishImmediately,
                    onCheckedChange = { publishImmediately = it }
                )
            }

            if (validationError != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = validationError ?: "",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            Button(
                onClick = {
                    val preservedStem = FormulaTextPreserver.preserveExactStructure(stem, 2000)
                    val preservedA = FormulaTextPreserver.preserveExactStructure(optionA, 1000)
                    val preservedB = FormulaTextPreserver.preserveExactStructure(optionB, 1000)
                    val preservedC = FormulaTextPreserver.preserveExactStructure(optionC, 1000)
                    val preservedD = FormulaTextPreserver.preserveExactStructure(optionD, 1000)
                    val preservedReason = FormulaTextPreserver.preserveExactStructure(explanation, 3000)
                    val cleanYt = if (youtubeUrl.isBlank()) null else CsvMcqParser.sanitizeOptionalHttpUrl(youtubeUrl)
                    when {
                        !MedPrepConstants.isCoreSubject(subject) -> validationError = "Please select one of the 5 core subjects."
                        preservedStem.trim().length < 5 -> validationError = "Question stem must be at least 5 characters."
                        preservedA.isBlank() || preservedB.isBlank() || preservedC.isBlank() || preservedD.isBlank() ->
                            validationError = "All four options (A, B, C, D) are required."
                        youtubeUrl.isNotBlank() && cleanYt == null ->
                            validationError = "YouTube link must be a valid https:// URL."
                        else -> {
                            validationError = null
                            onSave(
                                QuestionDraftInput(
                                    existingId = initialQuestion?.id,
                                    subject = subject.trim(),
                                    topic = topic.trim(),
                                    stem = preservedStem,
                                    optionA = preservedA,
                                    optionB = preservedB,
                                    optionC = preservedC,
                                    optionD = preservedD,
                                    correctOption = correctOption,
                                    explanation = preservedReason,
                                    youtubeUrl = cleanYt,
                                    linkedPostId = linkedPostId.takeIf { it.isNotBlank() },
                                    difficulty = difficulty,
                                    status = if (publishImmediately) "published" else "draft"
                                )
                            )
                        }
                    }
                },
                enabled = !isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("editor_save_mcq_button")
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(if (initialQuestion == null) "Save Question" else "Update Question")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PostEditorBottomSheet(
    initialPost: StudyPost?,
    availableCourses: List<CourseItem> = emptyList(),
    preselectedCourseId: String? = null,
    preselectedSubject: String? = null,
    isSaving: Boolean,
    onPreviewHtml: ((StudyPost) -> Unit)? = null,
    onDismiss: () -> Unit,
    onSave: (StudyPostDraftInput) -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by remember { mutableStateOf(initialPost?.title ?: "") }
    var body by remember { mutableStateOf(initialPost?.body ?: "") }
    var subject by remember {
        mutableStateOf(
            initialPost?.subject?.takeIf { MedPrepConstants.isCoreSubject(it) }
                ?: preselectedSubject?.takeIf { MedPrepConstants.isCoreSubject(it) }
                ?: MedPrepConstants.DEFAULT_SUBJECTS.first()
        )
    }
    var selectedCourseId by remember { mutableStateOf(initialPost?.courseId ?: preselectedCourseId ?: "") }
    var topic by remember { mutableStateOf(initialPost?.topic ?: "") }
    var webUrl by remember { mutableStateOf(initialPost?.webUrl ?: "") }
    var youtubeUrl by remember { mutableStateOf(initialPost?.youtubeUrl ?: "") }
    var htmlContent by remember { mutableStateOf(initialPost?.htmlContent ?: "") }
    var htmlFileName by remember { mutableStateOf(initialPost?.htmlFileName ?: "") }
    var showHtmlCodeEditor by remember { mutableStateOf(false) }
    var publishImmediately by remember { mutableStateOf((initialPost?.status ?: "published") == "published") }
    var validationError by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    val pickHtmlLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val appContext = context.applicationContext
            scope.launch(Dispatchers.IO) {
                val result = runCatching {
                    val rawBytes = appContext.contentResolver.openInputStream(uri)?.use { stream ->
                        stream.readBytes()
                    }
                    val resolvedName = resolveDialogFileName(appContext, uri) ?: "interactive_lesson.html"
                    rawBytes to resolvedName
                }
                withContext(Dispatchers.Main) {
                    result.onSuccess { (rawBytes, resolvedName) ->
                        if (rawBytes == null || rawBytes.isEmpty()) {
                            validationError = "Selected HTML file is empty."
                        } else {
                            val decoded = rawBytes.toString(Charsets.UTF_8)
                            if (decoded.length > MedPrepConstants.MAX_HTML_CONTENT_CHARS) {
                                validationError = "HTML file is too large (${decoded.length / 1024} KB). Max size is 200 KB."
                            } else {
                                htmlContent = decoded
                                htmlFileName = resolvedName
                                if (title.isBlank()) {
                                    title = htmlFileName.substringBeforeLast('.').replace('_', ' ').replace('-', ' ')
                                        .replaceFirstChar { it.uppercase() }
                                }
                                validationError = null
                            }
                        }
                    }.onFailure { e ->
                        validationError = "Could not read HTML file: ${e.localizedMessage ?: "Error"}"
                    }
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (initialPost == null) "Upload Topic Lesson / Study Post" else "Edit Study Post",
                style = MaterialTheme.typography.headlineSmall
            )

            Text("Subject *", style = MaterialTheme.typography.titleSmall)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MedPrepConstants.DEFAULT_SUBJECTS.forEach { subj ->
                    val visual = subjectVisualFor(subj)
                    FilterChip(
                        selected = subject.equals(subj, ignoreCase = true),
                        onClick = { subject = subj },
                        leadingIcon = { Icon(visual.icon, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text(subj) }
                    )
                }
            }

            if (availableCourses.isNotEmpty()) {
                Text("Attach to Course (Optional)", style = MaterialTheme.typography.titleSmall)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedCourseId.isBlank(),
                        onClick = { selectedCourseId = "" },
                        label = { Text("None") }
                    )
                    availableCourses.forEach { course ->
                        FilterChip(
                            selected = selectedCourseId == course.id,
                            onClick = {
                                selectedCourseId = course.id
                                if (MedPrepConstants.isCoreSubject(course.subject)) {
                                    subject = course.subject
                                }
                            },
                            label = {
                                Text(
                                    text = course.title,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it.take(180) },
                label = { Text("Lesson / Post Title *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("post_editor_title_input")
            )

            OutlinedTextField(
                value = topic,
                onValueChange = { topic = it.take(120) },
                label = { Text("Topic / Chapter (Auto-links with MCQs having the same Topic)") },
                placeholder = { Text("e.g. Organic Chemistry, Cell Biology, Mechanics") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("post_editor_topic_input")
            )

            // Interactive HTML Upload & Editor Card
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Code,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Interactive HTML Topic Explanation",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "Upload an .html file or paste HTML code so students can explore interactive topic diagrams right inside the app.",
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
                            onClick = {
                                pickHtmlLauncher.launch(arrayOf("text/html", "application/xhtml+xml", "text/plain", "*/*"))
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("post_upload_html_file_button")
                        ) {
                            Icon(Icons.Rounded.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Upload .HTML File")
                        }
                        OutlinedButton(
                            onClick = { showHtmlCodeEditor = !showHtmlCodeEditor },
                            modifier = Modifier.height(46.dp)
                        ) {
                            Text(if (showHtmlCodeEditor) "Hide Code" else "Paste HTML")
                        }
                    }

                    if (htmlContent.isNotBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = htmlFileName.ifBlank { "interactive_lesson.html" },
                                        style = MaterialTheme.typography.labelLarge,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${(htmlContent.length / 1024f).let { String.format("%.1f", it) }} KB ready",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Row {
                                    if (onPreviewHtml != null) {
                                        IconButton(
                                            onClick = {
                                                onPreviewHtml(
                                                    StudyPost(
                                                        id = initialPost?.id.orEmpty(),
                                                        title = title.ifBlank { "HTML Preview" },
                                                        body = body,
                                                        subject = subject,
                                                        topic = topic,
                                                        webUrl = webUrl.takeIf { it.isNotBlank() },
                                                        youtubeUrl = youtubeUrl.takeIf { it.isNotBlank() },
                                                        htmlContent = htmlContent,
                                                        htmlFileName = htmlFileName.ifBlank { "preview.html" }
                                                    )
                                                )
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Visibility,
                                                contentDescription = "Preview Interactive HTML",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = {
                                            htmlContent = ""
                                            htmlFileName = ""
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.DeleteOutline,
                                            contentDescription = "Remove HTML",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        TextButton(
                            onClick = {
                                htmlContent = InteractiveHtmlSamples.CHEMISTRY_ORGANIC_SAMPLE
                                htmlFileName = "organic_chemistry_interactive.html"
                                if (subject != "Chemistry") subject = "Chemistry"
                                if (topic.isBlank()) topic = "Organic Chemistry"
                                if (title.isBlank()) title = "Interactive Organic Chemistry: SN1 vs SN2 & Aldehydes"
                            },
                            modifier = Modifier.testTag("post_load_sample_html_button")
                        ) {
                            Icon(Icons.Rounded.Science, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Load Sample Interactive Chemistry HTML")
                        }
                    }

                    if (showHtmlCodeEditor) {
                        OutlinedTextField(
                            value = htmlContent,
                            onValueChange = {
                                htmlContent = it.take(MedPrepConstants.MAX_HTML_CONTENT_CHARS)
                                if (htmlFileName.isBlank() && it.isNotBlank()) {
                                    htmlFileName = "custom_interactive.html"
                                }
                            },
                            label = { Text("Interactive HTML Code (max 200 KB)") },
                            placeholder = { Text("<!DOCTYPE html><html>...</html>") },
                            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = MonoFontFamily),
                            minLines = 5,
                            maxLines = 10,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("post_editor_html_code_input")
                        )
                    }
                }
            }

            OutlinedTextField(
                value = body,
                onValueChange = { body = it.take(3000) },
                label = { Text("Notes / Chemistry Formulas / Summary") },
                minLines = 3,
                maxLines = 7,
                modifier = Modifier.fillMaxWidth().testTag("post_editor_body_input")
            )

            OutlinedTextField(
                value = webUrl,
                onValueChange = { webUrl = it.take(500) },
                label = { Text("External Web Page Link (Optional)") },
                placeholder = { Text("https://example.com/study-notes") },
                leadingIcon = { Icon(Icons.Rounded.Language, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("post_editor_web_url_input")
            )

            OutlinedTextField(
                value = youtubeUrl,
                onValueChange = { youtubeUrl = it.take(500) },
                label = { Text("YouTube Video Link (Optional)") },
                placeholder = { Text("https://www.youtube.com/watch?v=...") },
                leadingIcon = { Icon(Icons.Rounded.PlayCircle, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("post_editor_youtube_url_input")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Publish to Student Feed", style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = if (publishImmediately) "Visible in Students' Posts & linked MCQs" else "Saved as Draft",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = publishImmediately, onCheckedChange = { publishImmediately = it })
            }

            if (validationError != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = validationError ?: "",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            Button(
                onClick = {
                    val cleanWeb = if (webUrl.isBlank()) null else CsvMcqParser.sanitizeOptionalHttpUrl(webUrl)
                    val cleanYt = if (youtubeUrl.isBlank()) null else CsvMcqParser.sanitizeOptionalHttpUrl(youtubeUrl)
                    when {
                        title.trim().length < 3 -> validationError = "Title must be at least 3 characters."
                        webUrl.isNotBlank() && cleanWeb == null -> validationError = "Web Page Link must be a valid https:// URL."
                        youtubeUrl.isNotBlank() && cleanYt == null -> validationError = "YouTube Link must be a valid https:// URL."
                        htmlContent.length > MedPrepConstants.MAX_HTML_CONTENT_CHARS ->
                            validationError = "Interactive HTML exceeds 200 KB limit."
                        else -> {
                            validationError = null
                            onSave(
                                StudyPostDraftInput(
                                    existingId = initialPost?.id,
                                    title = title.trim(),
                                    body = FormulaTextPreserver.preserveExactStructure(body, 3000),
                                    subject = subject,
                                    topic = topic.trim(),
                                    courseId = selectedCourseId.takeIf { it.isNotBlank() },
                                    webUrl = cleanWeb,
                                    youtubeUrl = cleanYt,
                                    htmlContent = htmlContent.takeIf { it.isNotBlank() },
                                    htmlFileName = htmlFileName.trim().takeIf { htmlContent.isNotBlank() && it.isNotEmpty() },
                                    status = if (publishImmediately) "published" else "draft"
                                )
                            )
                        }
                    }
                },
                enabled = !isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("post_editor_save_button")
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(if (initialPost == null) "Upload Topic Post" else "Update Topic Post")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
fun GenerateAccessKeyDialog(
    isGenerating: Boolean,
    onDismiss: () -> Unit,
    onGenerate: (label: String, customKeyCode: String?) -> Unit
) {
    var label by remember { mutableStateOf("") }
    var customKey by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Student-Only Access Key") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Only students use Access Keys to log in (locked to 1 student & 1 device). Enter a specific custom key or leave blank to auto-generate.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = customKey,
                    onValueChange = {
                        customKey = it.uppercase().filter { ch -> ch.isLetterOrDigit() || ch == '-' || ch == '_' }.take(40)
                        localError = null
                    },
                    label = { Text("Specific Key Code (Optional)") },
                    placeholder = { Text("e.g. ALI-MDCAT-2026 (or auto)") },
                    leadingIcon = { Icon(Icons.Rounded.Key, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("generate_key_custom_code_input")
                )
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it.take(100) },
                    label = { Text("Student Name / Batch (Optional)") },
                    placeholder = { Text("e.g. Ali Raza - MDCAT 2026") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("generate_key_label_input")
                )
                if (localError != null) {
                    Text(
                        text = localError ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmedKey = customKey.trim()
                    if (trimmedKey.isNotEmpty() && trimmedKey.length < 6) {
                        localError = "Specific key must be at least 6 characters (or leave blank to auto-generate)."
                    } else {
                        onGenerate(label, trimmedKey.takeIf { it.isNotEmpty() })
                    }
                },
                enabled = !isGenerating,
                modifier = Modifier.testTag("confirm_generate_key_button")
            ) {
                Text(if (isGenerating) "Creating…" else if (customKey.isNotBlank()) "Create Specific Key" else "Generate Key")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun resolveDialogFileName(context: android.content.Context, uri: Uri): String? {
    return runCatching {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else null
        }
    }.getOrNull() ?: uri.lastPathSegment
}
