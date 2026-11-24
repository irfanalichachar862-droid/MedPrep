package com.example.ui.screens

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Article
import androidx.compose.material.icons.automirrored.rounded.FactCheck
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.LibraryAdd
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Publish
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.TableView
import androidx.compose.material.icons.rounded.Unpublished
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.AccessKey
import com.example.data.CourseDraftInput
import com.example.data.CourseItem
import com.example.data.CsvMcqParser
import com.example.data.CsvParseResult
import com.example.data.McqQuestion
import com.example.data.MedPrepConstants
import com.example.data.QuestionDraftInput
import com.example.data.SpreadsheetMcqReader
import com.example.data.StudyPost
import com.example.data.StudyPostDraftInput
import com.example.data.TopicLinkResolver
import com.example.data.UiState
import com.example.ui.components.FormulaText
import com.example.ui.components.LiquidGlassScrollableTabBar
import com.example.ui.components.LiquidGlassTabItem
import com.example.ui.theme.LocalMedPrepColors
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.subjectVisualFor

private enum class AdminSection(val labelRes: Int, val icon: ImageVector, val tag: String) {
    OVERVIEW(R.string.admin_tab_overview, Icons.Rounded.Analytics, "admin_tab_overview"),
    COURSES(R.string.admin_tab_courses, Icons.Rounded.School, "admin_tab_courses"),
    MCQS(R.string.admin_tab_mcqs, Icons.AutoMirrored.Rounded.FactCheck, "admin_tab_mcqs"),
    EXCEL(R.string.admin_tab_excel, Icons.Rounded.TableView, "admin_tab_excel"),
    POSTS(R.string.admin_tab_posts, Icons.AutoMirrored.Rounded.Article, "admin_tab_posts"),
    KEYS(R.string.admin_tab_keys, Icons.Rounded.Key, "admin_tab_keys")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(
    studioQuestionsState: UiState<List<McqQuestion>>,
    adminCoursesState: UiState<List<CourseItem>>,
    adminPostsState: UiState<List<StudyPost>>,
    accessKeysState: UiState<List<AccessKey>>,
    isDarkTheme: Boolean,
    isMutating: Boolean,
    snackbarHostState: SnackbarHostState,
    onToggleTheme: () -> Unit,
    onSaveQuestion: (QuestionDraftInput, () -> Unit) -> Unit,
    onToggleStatus: (McqQuestion) -> Unit,
    onDuplicateQuestion: (McqQuestion) -> Unit,
    onPublishAllDrafts: () -> Unit,
    onCleanupLegacySubjects: () -> Unit,
    onDeleteQuestion: (String) -> Unit,
    onImportBatch: (List<QuestionDraftInput>, () -> Unit) -> Unit,
    onLoadStarterPack: () -> Unit,
    onSaveCourse: (CourseDraftInput, () -> Unit) -> Unit,
    onToggleCourseStatus: (CourseItem) -> Unit,
    onDeleteCourse: (String) -> Unit,
    onSavePost: (StudyPostDraftInput, () -> Unit) -> Unit,
    onTogglePostStatus: (StudyPost) -> Unit,
    onDeletePost: (String) -> Unit,
    onPreviewHtmlLesson: (StudyPost) -> Unit,
    onGenerateAccessKey: (String, String?, (String) -> Unit) -> Unit,
    onResetAccessKeyDevice: (String) -> Unit,
    onToggleAccessKeyStatus: (AccessKey) -> Unit,
    onDeleteAccessKey: (String) -> Unit,
    onPostMessage: (String) -> Unit,
    onPreviewStudentApp: () -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val questions = (studioQuestionsState as? UiState.Success)?.data.orEmpty()
    val courses = (adminCoursesState as? UiState.Success)?.data.orEmpty()
    val posts = (adminPostsState as? UiState.Success)?.data.orEmpty()
    val keys = (accessKeysState as? UiState.Success)?.data.orEmpty()

    var activeSection by rememberSaveable { mutableStateOf(AdminSection.OVERVIEW) }
    var editingQuestion by remember { mutableStateOf<McqQuestion?>(null) }
    var showMcqEditor by remember { mutableStateOf(false) }
    var pendingDeleteMcq by remember { mutableStateOf<McqQuestion?>(null) }

    var editingCourse by remember { mutableStateOf<CourseItem?>(null) }
    var showCourseEditor by remember { mutableStateOf(false) }
    var pendingDeleteCourse by remember { mutableStateOf<CourseItem?>(null) }

    var editingPost by remember { mutableStateOf<StudyPost?>(null) }
    var postEditorPreselectedCourseId by remember { mutableStateOf<String?>(null) }
    var postEditorPreselectedSubject by remember { mutableStateOf<String?>(null) }
    var showPostEditor by remember { mutableStateOf(false) }
    var pendingDeletePost by remember { mutableStateOf<StudyPost?>(null) }

    var showGenerateKeyDialog by remember { mutableStateOf(false) }
    var pendingDeleteKey by remember { mutableStateOf<AccessKey?>(null) }

    // Excel / CSV Upload State
    var uploadedFileName by remember { mutableStateOf<String?>(null) }
    var pastedSheetText by remember { mutableStateOf("") }
    var fileParseResult by remember { mutableStateOf<CsvParseResult?>(null) }
    var importAsPublished by remember { mutableStateOf(true) }

    val openSpreadsheetLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val fileName = resolveFileName(context, uri)
                val defaultStatus = if (importAsPublished) "published" else "draft"
                val parsed = context.contentResolver.openInputStream(uri)?.use { stream ->
                    SpreadsheetMcqReader.parseStream(stream, fileName, defaultStatus)
                } ?: CsvParseResult(emptyList(), listOf("Could not open selected file."))

                uploadedFileName = fileName ?: "Spreadsheet"
                fileParseResult = parsed
                pastedSheetText = ""
                activeSection = AdminSection.EXCEL
                onPostMessage("Loaded ${parsed.validQuestions.size} MCQs from $uploadedFileName.")
            } catch (e: Exception) {
                onPostMessage("Failed to read Excel/CSV file: ${e.localizedMessage ?: "Unknown error"}")
            }
        }
    }

    val downloadTemplateLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(CsvMcqParser.TEMPLATE_CSV.toByteArray(Charsets.UTF_8))
                }
                onPostMessage("Template saved with Reason & YouTube columns! Open in Excel and upload.")
            } catch (e: Exception) {
                onPostMessage("Could not save template: ${e.localizedMessage ?: "Error"}")
            }
        }
    }

    val exportBankLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val coreOnly = questions.filter { MedPrepConstants.isCoreSubject(it.subject) }
                val csv = SpreadsheetMcqReader.exportQuestionsToCsv(coreOnly)
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(csv.toByteArray(Charsets.UTF_8))
                }
                onPostMessage("Exported ${coreOnly.size} MCQs to spreadsheet.")
            } catch (e: Exception) {
                onPostMessage("Export failed: ${e.localizedMessage ?: "Error"}")
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // User's uploaded profile photo as the Admin Icon
                        Image(
                            painter = painterResource(R.drawable.admin_profile_photo),
                            contentDescription = "Admin Profile Icon",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                        )
                        Column {
                            Text(
                                text = stringResource(R.string.admin_panel_title),
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = MedPrepConstants.BOOTSTRAP_ADMIN_EMAIL,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = onPreviewStudentApp,
                        modifier = Modifier.testTag("admin_student_preview_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.School,
                            contentDescription = "Preview Student App",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = onToggleTheme,
                        modifier = Modifier.testTag("admin_theme_toggle")
                    ) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                            contentDescription = "Toggle theme"
                        )
                    }
                    IconButton(
                        onClick = onSignOut,
                        modifier = Modifier.testTag("admin_sign_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Logout,
                            contentDescription = "Sign out",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            when (activeSection) {
                AdminSection.COURSES -> FloatingActionButton(
                    onClick = {
                        editingCourse = null
                        showCourseEditor = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("admin_fab_add_course")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Create Course")
                }
                AdminSection.MCQS -> FloatingActionButton(
                    onClick = {
                        editingQuestion = null
                        showMcqEditor = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("admin_fab_add_mcq")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Add MCQ")
                }
                AdminSection.POSTS -> FloatingActionButton(
                    onClick = {
                        editingPost = null
                        postEditorPreselectedCourseId = null
                        postEditorPreselectedSubject = null
                        showPostEditor = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("admin_fab_add_post")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Upload Post")
                }
                AdminSection.KEYS -> FloatingActionButton(
                    onClick = { showGenerateKeyDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("admin_fab_add_key")
                ) {
                    Icon(Icons.Rounded.Key, contentDescription = "Generate Key")
                }
                else -> Unit
            }
        },
        bottomBar = {
            val adminGlassTabs = AdminSection.entries.map { section ->
                LiquidGlassTabItem(
                    id = section.name,
                    label = stringResource(section.labelRes),
                    selectedIcon = section.icon,
                    unselectedIcon = section.icon,
                    testTag = section.tag
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                LiquidGlassScrollableTabBar(
                    items = adminGlassTabs,
                    selectedId = activeSection.name,
                    onSelect = { selectedName ->
                        activeSection = AdminSection.entries.firstOrNull { it.name == selectedName }
                            ?: AdminSection.OVERVIEW
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            when (activeSection) {
                AdminSection.OVERVIEW -> AdminOverviewTab(
                    questions = questions,
                    coursesCount = courses.size,
                    postsCount = posts.size,
                    activeKeysCount = keys.count { it.status == "active" },
                    isDarkTheme = isDarkTheme,
                    isMutating = isMutating,
                    onAddMcq = {
                        editingQuestion = null
                        showMcqEditor = true
                    },
                    onOpenCourses = { activeSection = AdminSection.COURSES },
                    onOpenExcelUpload = { activeSection = AdminSection.EXCEL },
                    onOpenPosts = { activeSection = AdminSection.POSTS },
                    onOpenKeys = { activeSection = AdminSection.KEYS },
                    onPublishAllDrafts = onPublishAllDrafts,
                    onCleanupLegacySubjects = onCleanupLegacySubjects,
                    onLoadStarterPack = onLoadStarterPack,
                    onExportBank = { exportBankLauncher.launch("medprep_question_bank.csv") },
                    onDownloadTemplate = { downloadTemplateLauncher.launch("medprep_excel_template.csv") }
                )

                AdminSection.COURSES -> AdminCoursesManagerTab(
                    adminCoursesState = adminCoursesState,
                    allPosts = posts,
                    isDarkTheme = isDarkTheme,
                    onCreateCourse = {
                        editingCourse = null
                        showCourseEditor = true
                    },
                    onEditCourse = { course ->
                        editingCourse = course
                        showCourseEditor = true
                    },
                    onAddPostForCourse = { course ->
                        editingPost = null
                        postEditorPreselectedCourseId = course.id
                        postEditorPreselectedSubject = course.subject.takeIf { MedPrepConstants.isCoreSubject(it) }
                        showPostEditor = true
                    },
                    onToggleCourseStatus = onToggleCourseStatus,
                    onDeleteCourse = { course -> pendingDeleteCourse = course }
                )

                AdminSection.MCQS -> AdminMcqsManagerTab(
                    studioQuestionsState = studioQuestionsState,
                    posts = posts,
                    isDarkTheme = isDarkTheme,
                    onEdit = { mcq ->
                        editingQuestion = mcq
                        showMcqEditor = true
                    },
                    onToggleStatus = onToggleStatus,
                    onDuplicate = onDuplicateQuestion,
                    onDelete = { mcq -> pendingDeleteMcq = mcq }
                )

                AdminSection.EXCEL -> AdminExcelUploadTab(
                    uploadedFileName = uploadedFileName,
                    pastedText = pastedSheetText,
                    fileParseResult = fileParseResult,
                    importAsPublished = importAsPublished,
                    isImporting = isMutating,
                    onPastedTextChanged = { text ->
                        pastedSheetText = text
                        if (text.isNotBlank()) {
                            uploadedFileName = null
                            fileParseResult = null
                        }
                    },
                    onToggleImportPublished = { checked ->
                        importAsPublished = checked
                        fileParseResult = fileParseResult?.let { prev ->
                            val newStatus = if (checked) "published" else "draft"
                            prev.copy(validQuestions = prev.validQuestions.map { it.copy(status = newStatus) })
                        }
                    },
                    onPickExcelOrCsv = {
                        openSpreadsheetLauncher.launch(
                            arrayOf(
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                "application/vnd.ms-excel",
                                "text/csv",
                                "text/comma-separated-values",
                                "text/plain",
                                "*/*"
                            )
                        )
                    },
                    onDownloadTemplate = { downloadTemplateLauncher.launch("medprep_excel_template.csv") },
                    onPasteSample = {
                        uploadedFileName = null
                        fileParseResult = null
                        pastedSheetText = CsvMcqParser.TEMPLATE_CSV
                    },
                    onConfirmImport = { list ->
                        onImportBatch(list) {
                            uploadedFileName = null
                            fileParseResult = null
                            pastedSheetText = ""
                            activeSection = AdminSection.MCQS
                        }
                    }
                )

                AdminSection.POSTS -> AdminPostsManagerTab(
                    adminPostsState = adminPostsState,
                    allQuestions = questions,
                    isDarkTheme = isDarkTheme,
                    onCreatePost = {
                        editingPost = null
                        postEditorPreselectedCourseId = null
                        postEditorPreselectedSubject = null
                        showPostEditor = true
                    },
                    onEditPost = { post ->
                        editingPost = post
                        postEditorPreselectedCourseId = post.courseId
                        postEditorPreselectedSubject = post.subject
                        showPostEditor = true
                    },
                    onPreviewHtml = onPreviewHtmlLesson,
                    onTogglePostStatus = onTogglePostStatus,
                    onDeletePost = { post -> pendingDeletePost = post },
                    onPostMessage = onPostMessage
                )

                AdminSection.KEYS -> AdminKeysManagerTab(
                    accessKeysState = accessKeysState,
                    onOpenGenerateDialog = { showGenerateKeyDialog = true },
                    onResetDevice = onResetAccessKeyDevice,
                    onToggleStatus = onToggleAccessKeyStatus,
                    onDeleteKey = { key -> pendingDeleteKey = key },
                    onPostMessage = onPostMessage
                )
            }
        }
    }

    if (showMcqEditor) {
        McqEditorBottomSheet(
            initialQuestion = editingQuestion,
            availablePosts = posts,
            isSaving = isMutating,
            onDismiss = { showMcqEditor = false },
            onSave = { draft -> onSaveQuestion(draft) { showMcqEditor = false } }
        )
    }

    if (showCourseEditor) {
        CourseEditorBottomSheet(
            initialCourse = editingCourse,
            isSaving = isMutating,
            onDismiss = { showCourseEditor = false },
            onSave = { draft -> onSaveCourse(draft) { showCourseEditor = false } }
        )
    }

    if (showPostEditor) {
        PostEditorBottomSheet(
            initialPost = editingPost,
            availableCourses = courses,
            preselectedCourseId = postEditorPreselectedCourseId,
            preselectedSubject = postEditorPreselectedSubject,
            isSaving = isMutating,
            onPreviewHtml = { previewPost ->
                showPostEditor = false
                onPreviewHtmlLesson(previewPost)
            },
            onDismiss = { showPostEditor = false },
            onSave = { draft ->
                onSavePost(draft) {
                    showPostEditor = false
                    postEditorPreselectedCourseId = null
                    postEditorPreselectedSubject = null
                }
            }
        )
    }

    if (showGenerateKeyDialog) {
        GenerateAccessKeyDialog(
            isGenerating = isMutating,
            onDismiss = { showGenerateKeyDialog = false },
            onGenerate = { label, customKey ->
                onGenerateAccessKey(label, customKey) { createdCode ->
                    clipboard.setText(AnnotatedString(createdCode))
                    showGenerateKeyDialog = false
                }
            }
        )
    }

    val deleteMcqTarget = pendingDeleteMcq
    if (deleteMcqTarget != null) {
        AlertDialog(
            onDismissRequest = { pendingDeleteMcq = null },
            title = { Text("Delete MCQ?") },
            text = { Text("Permanently remove this ${deleteMcqTarget.subject} MCQ?") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteQuestion(deleteMcqTarget.id)
                    pendingDeleteMcq = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { pendingDeleteMcq = null }) { Text("Cancel") } }
        )
    }

    val deletePostTarget = pendingDeletePost
    if (deletePostTarget != null) {
        AlertDialog(
            onDismissRequest = { pendingDeletePost = null },
            title = { Text("Delete Study Post?") },
            text = { Text("Remove '${deletePostTarget.title}' from Students' feed?") },
            confirmButton = {
                TextButton(onClick = {
                    onDeletePost(deletePostTarget.id)
                    pendingDeletePost = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { pendingDeletePost = null }) { Text("Cancel") } }
        )
    }

    val deleteCourseTarget = pendingDeleteCourse
    if (deleteCourseTarget != null) {
        AlertDialog(
            onDismissRequest = { pendingDeleteCourse = null },
            title = { Text("Delete Course?") },
            text = { Text("Permanently remove '${deleteCourseTarget.title}' from the Courses tab?") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteCourse(deleteCourseTarget.id)
                    pendingDeleteCourse = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { pendingDeleteCourse = null }) { Text("Cancel") } }
        )
    }

    val deleteKeyTarget = pendingDeleteKey
    if (deleteKeyTarget != null) {
        AlertDialog(
            onDismissRequest = { pendingDeleteKey = null },
            title = { Text("Delete Access Key?") },
            text = { Text("Delete key ${deleteKeyTarget.keyCode}? The bound student will immediately lose access.") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteAccessKey(deleteKeyTarget.keyCode)
                    pendingDeleteKey = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { pendingDeleteKey = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun AdminOverviewTab(
    questions: List<McqQuestion>,
    coursesCount: Int,
    postsCount: Int,
    activeKeysCount: Int,
    isDarkTheme: Boolean,
    isMutating: Boolean,
    onAddMcq: () -> Unit,
    onOpenCourses: () -> Unit,
    onOpenExcelUpload: () -> Unit,
    onOpenPosts: () -> Unit,
    onOpenKeys: () -> Unit,
    onPublishAllDrafts: () -> Unit,
    onCleanupLegacySubjects: () -> Unit,
    onLoadStarterPack: () -> Unit,
    onExportBank: () -> Unit,
    onDownloadTemplate: () -> Unit
) {
    val extendedColors = LocalMedPrepColors.current
    val coreQuestions = questions.filter { MedPrepConstants.isCoreSubject(it.subject) }
    val legacyCount = questions.size - coreQuestions.size
    val draftCount = coreQuestions.count { it.status == "draft" }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 720.dp),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AdminKpiTile("MCQS", "${coreQuestions.size}", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer, Modifier.weight(1f))
                AdminKpiTile("COURSES", "$coursesCount", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer, Modifier.weight(1f))
                AdminKpiTile("POSTS", "$postsCount", extendedColors.successContainer, extendedColors.onSuccessContainer, Modifier.weight(1f))
                AdminKpiTile("KEYS", "$activeKeysCount", extendedColors.warningContainer, extendedColors.onWarningContainer, Modifier.weight(1f))
            }
        }

        // One-Tap Old Subjects Cleanup Banner (appears if legacy subjects like Anatomy/Physiology exist)
        if (legacyCount > 0) {
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "$legacyCount Old-Subject MCQs Detected",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = "Delete all non-core subjects from Firestore so only Biology, Chemistry, Physics, English & Logical Reasoning exist.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Button(
                            onClick = onCleanupLegacySubjects,
                            enabled = !isMutating,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("admin_cleanup_legacy_subjects_btn")
                        ) {
                            Icon(Icons.Rounded.CleaningServices, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Delete $legacyCount Old-Subject MCQs")
                        }
                    }
                }
            }
        }

        // Quick Admin Tools Grid
        item {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Admin Command Center", style = MaterialTheme.typography.titleMedium)

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = onOpenExcelUpload,
                            modifier = Modifier.weight(1f).height(48.dp).testTag("admin_overview_excel_btn")
                        ) {
                            Icon(Icons.Rounded.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Excel Upload")
                        }
                        OutlinedButton(
                            onClick = onAddMcq,
                            modifier = Modifier.weight(1f).height(48.dp).testTag("admin_overview_add_btn")
                        ) {
                            Icon(Icons.Rounded.AddCircleOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add MCQ")
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = onOpenCourses,
                            modifier = Modifier.weight(1f).height(46.dp).testTag("admin_overview_courses_btn")
                        ) {
                            Icon(Icons.Rounded.School, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Courses")
                        }
                        OutlinedButton(
                            onClick = onOpenPosts,
                            modifier = Modifier.weight(1f).height(46.dp).testTag("admin_overview_posts_btn")
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.Article, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Upload Post")
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = onOpenKeys,
                            modifier = Modifier.weight(1f).height(46.dp).testTag("admin_overview_keys_btn")
                        ) {
                            Icon(Icons.Rounded.Key, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Student Keys")
                        }
                        OutlinedButton(
                            onClick = onPublishAllDrafts,
                            enabled = !isMutating && draftCount > 0,
                            modifier = Modifier.weight(1f).height(46.dp).testTag("admin_publish_all_btn")
                        ) {
                            Icon(Icons.Rounded.DoneAll, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Publish ($draftCount)")
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = onExportBank,
                            enabled = coreQuestions.isNotEmpty(),
                            modifier = Modifier.weight(1f).height(44.dp).testTag("admin_export_bank_btn")
                        ) {
                            Icon(Icons.Rounded.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Export CSV")
                        }
                        TextButton(onClick = onDownloadTemplate, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Rounded.TableView, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Template")
                        }
                        TextButton(
                            onClick = onLoadStarterPack,
                            enabled = !isMutating,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.LibraryAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Starter 15")
                        }
                    }
                }
            }
        }

        // 5-Subject Inventory Breakdown
        item {
            Text("Subject Inventory (5 Core Subjects Only)", style = MaterialTheme.typography.titleMedium)
        }

        items(MedPrepConstants.DEFAULT_SUBJECTS) { subject ->
            val subQuestions = coreQuestions.filter { it.subject.equals(subject, ignoreCase = true) }
            val pub = subQuestions.count { it.status == "published" }
            val drf = subQuestions.count { it.status == "draft" }
            val visual = subjectVisualFor(subject)
            val badgeBg = if (isDarkTheme) visual.containerDark else visual.containerLight
            val iconTint = if (isDarkTheme) visual.containerLight else visual.accent

            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(badgeBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(visual.icon, contentDescription = subject, tint = iconTint, modifier = Modifier.size(21.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(subject, style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = "$pub published • $drf drafts",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "${subQuestions.size}",
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = MonoFontFamily),
                        color = visual.accent,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminKpiTile(
    label: String,
    value: String,
    container: Color,
    content: Color,
    modifier: Modifier = Modifier
) {
    Surface(color = container, shape = MaterialTheme.shapes.large, modifier = modifier) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = content.copy(alpha = 0.8f))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall.copy(fontFamily = MonoFontFamily),
                color = content,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun AdminMcqsManagerTab(
    studioQuestionsState: UiState<List<McqQuestion>>,
    posts: List<StudyPost>,
    isDarkTheme: Boolean,
    onEdit: (McqQuestion) -> Unit,
    onToggleStatus: (McqQuestion) -> Unit,
    onDuplicate: (McqQuestion) -> Unit,
    onDelete: (McqQuestion) -> Unit
) {
    val extendedColors = LocalMedPrepColors.current
    val questions = (studioQuestionsState as? UiState.Success)?.data
        ?.filter { MedPrepConstants.isCoreSubject(it.subject) }.orEmpty()

    var searchQuery by remember { mutableStateOf("") }
    var subjectFilter by remember { mutableStateOf("All") }
    var statusFilter by remember { mutableStateOf("all") }

    val filtered = remember(questions, searchQuery, subjectFilter, statusFilter) {
        questions.filter { q ->
            val matchesSubject = subjectFilter == "All" || q.subject.equals(subjectFilter, ignoreCase = true)
            val matchesStatus = statusFilter == "all" || q.status == statusFilter
            val matchesQuery = searchQuery.isBlank() ||
                q.stem.contains(searchQuery, ignoreCase = true) ||
                q.topic.contains(searchQuery, ignoreCase = true) ||
                q.subject.contains(searchQuery, ignoreCase = true)
            matchesSubject && matchesStatus && matchesQuery
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 720.dp),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(stringResource(R.string.studio_search_hint)) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth().testTag("admin_search_input")
            )
        }

        item {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                (listOf("All") + MedPrepConstants.DEFAULT_SUBJECTS).forEach { subj ->
                    val visual = if (subj == "All") null else subjectVisualFor(subj)
                    FilterChip(
                        selected = subjectFilter == subj,
                        onClick = { subjectFilter = subj },
                        leadingIcon = visual?.let { v ->
                            { Icon(v.icon, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        },
                        label = { Text(subj) }
                    )
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = statusFilter == "all", onClick = { statusFilter = "all" }, label = { Text("All (${questions.size})") })
                FilterChip(selected = statusFilter == "published", onClick = { statusFilter = "published" }, label = { Text("Published") })
                FilterChip(selected = statusFilter == "draft", onClick = { statusFilter = "draft" }, label = { Text("Drafts") })
            }
        }

        if (studioQuestionsState is UiState.Loading) {
            item {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        } else if (filtered.isEmpty()) {
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No MCQs match this filter. Tap + to add one or switch to Excel Upload.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(20.dp)
                    )
                }
            }
        } else {
            items(filtered, key = { it.id }) { mcq ->
                val isPublished = mcq.status == "published"
                val visual = subjectVisualFor(mcq.subject)
                val badgeBg = if (isDarkTheme) visual.containerDark else visual.containerLight
                val badgeFg = if (isDarkTheme) visual.containerLight else visual.accent

                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    color = if (isPublished) extendedColors.successContainer else extendedColors.warningContainer,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = if (isPublished) "PUBLISHED" else "DRAFT",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isPublished) extendedColors.onSuccessContainer else extendedColors.onWarningContainer,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                    )
                                }
                                Surface(color = badgeBg, shape = RoundedCornerShape(6.dp)) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(visual.icon, contentDescription = null, tint = badgeFg, modifier = Modifier.size(13.dp))
                                        Text(
                                            text = mcq.subject,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = badgeFg,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                if (!mcq.youtubeUrl.isNullOrBlank()) {
                                    Icon(
                                        imageVector = Icons.Rounded.PlayCircle,
                                        contentDescription = "Has YouTube video",
                                        tint = Color(0xFFDC2626),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(6.dp)) {
                                Text(
                                    text = "Key: ${mcq.correctOption}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                )
                            }
                        }

                        val linkedLesson = remember(mcq.id, mcq.linkedPostId, mcq.subject, mcq.topic, posts) {
                            TopicLinkResolver.findLinkedPost(mcq, posts)
                        }

                        FormulaText(
                            text = mcq.stem,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (linkedLesson != null) {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "Linked Topic: ${linkedLesson.title}${if (linkedLesson.hasInteractiveHtml) " • Interactive HTML" else ""}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        if (mcq.explanation.isNotBlank()) {
                            FormulaText(
                                text = "Reason: ${mcq.explanation}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { onToggleStatus(mcq) }) {
                                Icon(
                                    imageVector = if (isPublished) Icons.Rounded.Unpublished else Icons.Rounded.Publish,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(if (isPublished) stringResource(R.string.studio_unpublish) else stringResource(R.string.studio_publish))
                            }
                            Row {
                                IconButton(onClick = { onDuplicate(mcq) }) {
                                    Icon(Icons.Rounded.ContentCopy, contentDescription = "Duplicate MCQ", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick = { onEdit(mcq) }) {
                                    Icon(Icons.Rounded.Edit, contentDescription = "Edit MCQ", tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = { onDelete(mcq) }) {
                                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete MCQ", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminExcelUploadTab(
    uploadedFileName: String?,
    pastedText: String,
    fileParseResult: CsvParseResult?,
    importAsPublished: Boolean,
    isImporting: Boolean,
    onPastedTextChanged: (String) -> Unit,
    onToggleImportPublished: (Boolean) -> Unit,
    onPickExcelOrCsv: () -> Unit,
    onDownloadTemplate: () -> Unit,
    onPasteSample: () -> Unit,
    onConfirmImport: (List<QuestionDraftInput>) -> Unit
) {
    val extendedColors = LocalMedPrepColors.current
    val effectiveResult = remember(fileParseResult, pastedText, importAsPublished) {
        when {
            fileParseResult != null -> fileParseResult
            pastedText.isNotBlank() -> CsvMcqParser.parseCsv(
                pastedText,
                if (importAsPublished) "published" else "draft"
            )
            else -> null
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 720.dp),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.TableView,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text("Excel (.xlsx) & CSV Upload with Reason", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "Include 'reason' & 'youtube_url' columns so students see why the answer is right + watch a video.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Button(
                        onClick = onPickExcelOrCsv,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("admin_pick_excel_button")
                    ) {
                        Icon(Icons.Rounded.FileUpload, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (uploadedFileName != null) "Selected: $uploadedFileName" else "Choose Excel (.xlsx) or CSV File")
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDownloadTemplate,
                            modifier = Modifier.weight(1f).height(46.dp).testTag("admin_download_template_button")
                        ) {
                            Icon(Icons.Rounded.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Template")
                        }
                        OutlinedButton(
                            onClick = onPasteSample,
                            modifier = Modifier.weight(1f).height(46.dp).testTag("admin_load_sample_csv_button")
                        ) {
                            Text("5-Subject Sample")
                        }
                    }
                }
            }
        }

        item {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Or Paste Directly from Excel / Google Sheets", style = MaterialTheme.typography.titleSmall)
                    OutlinedTextField(
                        value = pastedText,
                        onValueChange = onPastedTextChanged,
                        placeholder = {
                            Text("subject,topic,question,option_a,option_b,option_c,option_d,correct,reason,youtube_url,difficulty,status")
                        },
                        minLines = 4,
                        maxLines = 7,
                        modifier = Modifier.fillMaxWidth().testTag("admin_paste_excel_input")
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Import Status: ${if (importAsPublished) "Published" else "Draft"}", style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = "Applies when the sheet has no status column.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(checked = importAsPublished, onCheckedChange = onToggleImportPublished)
                    }
                }
            }
        }

        if (effectiveResult != null) {
            item {
                val ready = effectiveResult.validQuestions
                Surface(
                    color = if (ready.isNotEmpty()) extendedColors.successContainer else MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = if (ready.isNotEmpty()) extendedColors.onSuccessContainer else MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = "${ready.size} valid MCQs ready to upload",
                                style = MaterialTheme.typography.titleMedium,
                                color = if (ready.isNotEmpty()) extendedColors.onSuccessContainer else MaterialTheme.colorScheme.onErrorContainer
                            )
                        }

                        if (ready.isNotEmpty()) {
                            val grouped = ready.groupBy { it.subject }
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                grouped.forEach { (subj, list) ->
                                    Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(8.dp)) {
                                        Text(
                                            text = "$subj: ${list.size}",
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        if (effectiveResult.errors.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                effectiveResult.errors.take(4).forEach { err ->
                                    Text(text = "• $err", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }

                        Button(
                            onClick = { onConfirmImport(ready) },
                            enabled = !isImporting && ready.isNotEmpty(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("admin_confirm_excel_import_button")
                        ) {
                            if (isImporting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Uploading to Firestore…")
                            } else {
                                Text("Upload ${ready.size} MCQs to MedPrep")
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Expected Excel Columns", style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = "subject | topic | question | option_a | option_b | option_c | option_d | correct (A–D) | reason | youtube_url | difficulty | status",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Allowed Subjects ONLY: Biology, Chemistry, Physics, English, Logical Reasoning.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

private fun resolveFileName(context: android.content.Context, uri: Uri): String? {
    return runCatching {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else null
        }
    }.getOrNull() ?: uri.lastPathSegment
}
