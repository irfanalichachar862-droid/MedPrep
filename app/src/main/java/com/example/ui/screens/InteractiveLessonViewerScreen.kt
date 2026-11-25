package com.example.ui.screens

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.StudyPost
import com.example.ui.theme.subjectVisualFor

/**
 * Full-screen sandboxed Interactive HTML Topic Viewer for Admin-uploaded HTML lessons.
 * Security hardening:
 * - No native JS bridges (zero addJavascriptInterface)
 * - File/content/universal URL access disabled
 * - Only https:// and http:// external links are delegated safely to the device browser
 * - Loaded against synthetic https://medprep.local/ origin (never file://)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InteractiveLessonViewerScreen(
    post: StudyPost,
    linkedMcqCount: Int,
    isDarkTheme: Boolean,
    isAdminPreview: Boolean = false,
    onPracticeLinkedMcqs: (() -> Unit)? = null,
    onPostMessage: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val visual = subjectVisualFor(post.subject)
    val preparedHtml = remember(post.htmlContent, isDarkTheme) {
        wrapHtmlDocument(post.htmlContent.orEmpty(), isDarkTheme)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("interactive_html_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                title = {
                    Column {
                        Text(
                            text = post.title.ifBlank { "Interactive Topic Lesson" },
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = visual.icon,
                                contentDescription = null,
                                tint = visual.accent,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = buildString {
                                    append(post.subject)
                                    if (post.topic.isNotBlank()) {
                                        append(" • ")
                                        append(post.topic)
                                    }
                                    if (isAdminPreview) {
                                        append(" (Preview)")
                                    }
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                actions = {
                    val webUrl = post.webUrl?.takeIf { it.isNotBlank() }
                    if (webUrl != null) {
                        IconButton(onClick = { openExternalLink(context, webUrl, onPostMessage) }) {
                            Icon(
                                imageVector = Icons.Rounded.Language,
                                contentDescription = "Open Web Link",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    val youtubeUrl = post.youtubeUrl?.takeIf { it.isNotBlank() }
                    if (youtubeUrl != null) {
                        IconButton(onClick = { openExternalLink(context, youtubeUrl, onPostMessage) }) {
                            Icon(
                                imageVector = Icons.Rounded.PlayCircle,
                                contentDescription = "Watch YouTube Video",
                                tint = Color(0xFFDC2626)
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (!isAdminPreview && onPracticeLinkedMcqs != null && linkedMcqCount > 0) {
                Surface(
                    tonalElevation = 3.dp,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Button(
                            onClick = onPracticeLinkedMcqs,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("interactive_lesson_practice_mcqs_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Quiz,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Practice $linkedMcqCount Linked MCQs")
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (!post.htmlFileName.isNullOrBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Code,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "Interactive HTML Lesson • ${post.htmlFileName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            if (post.htmlContent.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "No interactive HTML content is attached to this topic yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(20.dp)
                        )
                    }
                }
            } else {
                var webViewReady by remember(post.id) { mutableStateOf(false) }
                LaunchedEffect(post.id) {
                    withFrameNanos { }
                    withFrameNanos { }
                    webViewReady = true
                }
                if (webViewReady) {
                    SandboxedHtmlWebView(
                        htmlDocument = preparedHtml,
                        isDarkTheme = isDarkTheme,
                        onExternalHttpUrl = { url -> openExternalLink(context, url, onPostMessage) },
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("interactive_html_webview")
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(strokeWidth = 2.5.dp)
                    }
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun SandboxedHtmlWebView(
    htmlDocument: String,
    isDarkTheme: Boolean,
    onExternalHttpUrl: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                setBackgroundColor(if (isDarkTheme) AndroidColor.parseColor("#0F172A") else AndroidColor.WHITE)
                settings.apply {
                    // Required for Admin's interactive topic visualizations, simulations & buttons
                    javaScriptEnabled = true
                    // Strict sandbox: zero local file/content access, zero mixed content, zero native bridges
                    allowFileAccess = false
                    allowContentAccess = false
                    @Suppress("DEPRECATION")
                    allowFileAccessFromFileURLs = false
                    @Suppress("DEPRECATION")
                    allowUniversalAccessFromFileURLs = false
                    domStorageEnabled = false
                    databaseEnabled = false
                    setGeolocationEnabled(false)
                    mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                    builtInZoomControls = true
                    displayZoomControls = false
                    useWideViewPort = true
                    loadWithOverviewMode = true
                }
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        val uri = request?.url ?: return true
                        val scheme = uri.scheme?.lowercase().orEmpty()
                        if (scheme == "https" || scheme == "http") {
                            if (uri.host.equals("medprep.local", ignoreCase = true)) {
                                return false
                            }
                            onExternalHttpUrl(uri.toString())
                        }
                        // Block file://, content://, javascript:, intent:// and any other scheme
                        return true
                    }
                }
                tag = htmlDocument
                loadDataWithBaseURL(
                    "https://medprep.local/",
                    htmlDocument,
                    "text/html",
                    "UTF-8",
                    null
                )
            }
        },
        update = { webView ->
            if (webView.tag != htmlDocument) {
                webView.tag = htmlDocument
                webView.setBackgroundColor(if (isDarkTheme) AndroidColor.parseColor("#0F172A") else AndroidColor.WHITE)
                webView.loadDataWithBaseURL(
                    "https://medprep.local/",
                    htmlDocument,
                    "text/html",
                    "UTF-8",
                    null
                )
            }
        },
        onRelease = { webView ->
            webView.stopLoading()
            webView.loadUrl("about:blank")
            webView.destroy()
        }
    )
}

private fun wrapHtmlDocument(rawHtml: String, isDarkTheme: Boolean): String {
    val trimmed = rawHtml.trim()
    val hasFullDoc = trimmed.contains("<html", ignoreCase = true) ||
        trimmed.contains("<!doctype", ignoreCase = true)
    if (hasFullDoc) {
        return trimmed
    }
    val bg = if (isDarkTheme) "#0F172A" else "#FFFFFF"
    val fg = if (isDarkTheme) "#F1F5F9" else "#0F172A"
    val cardBg = if (isDarkTheme) "#1E293B" else "#F8FAFC"
    val accent = if (isDarkTheme) "#38BDF8" else "#0284C7"
    return """
        <!DOCTYPE html>
        <html lang="en">
        <head>
          <meta charset="UTF-8" />
          <meta name="viewport" content="width=device-width, initial-scale=1.0" />
          <style>
            * { box-sizing: border-box; }
            body {
              margin: 0; padding: 16px;
              font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
              background: $bg; color: $fg; line-height: 1.6;
            }
            h1, h2, h3 { color: $accent; margin-top: 0.6em; }
            pre, code {
              font-family: "JetBrains Mono", "Roboto Mono", monospace;
              background: $cardBg; padding: 10px 12px; border-radius: 10px;
              overflow-x: auto; white-space: pre; display: block;
            }
            button {
              background: $accent; color: #fff; border: none; border-radius: 10px;
              padding: 10px 16px; font-size: 14px; font-weight: 600; cursor: pointer;
            }
            .card {
              background: $cardBg; border-radius: 14px; padding: 14px; margin: 12px 0;
            }
          </style>
        </head>
        <body>
          $trimmed
        </body>
        </html>
    """.trimIndent()
}

object InteractiveHtmlSamples {
    const val CHEMISTRY_ORGANIC_SAMPLE = """<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <style>
    body { font-family: system-ui, sans-serif; margin: 0; padding: 16px; background: #0f172a; color: #f8fafc; line-height: 1.55; }
    .card { background: #1e293b; border: 1px solid #334155; border-radius: 16px; padding: 16px; margin-bottom: 14px; }
    h1 { font-size: 20px; color: #38bdf8; margin: 0 0 8px; }
    .formula { font-family: monospace; background: #0b1220; border: 1px solid #1e3a5f; border-radius: 12px; padding: 14px; white-space: pre; overflow-x: auto; font-size: 15px; color: #a5f3fc; }
    .tabs { display: flex; gap: 8px; flex-wrap: wrap; margin: 12px 0; }
    button { background: #0284c7; color: #fff; border: none; border-radius: 10px; padding: 10px 14px; font-weight: 600; cursor: pointer; }
    button.active { background: #10b981; }
    .highlight { color: #fbbf24; font-weight: 700; }
  </style>
</head>
<body>
  <div class="card">
    <h1>Interactive Organic Chemistry: SN1 vs SN2 &amp; Structures</h1>
    <p>Tap a reaction or functional group below to compare exact chemical structures and MDCAT exam rules.</p>
    <div class="tabs">
      <button id="b1" class="active" onclick="showTab('sn1')">SN1 Carbocation</button>
      <button id="b2" onclick="showTab('sn2')">SN2 Inversion</button>
      <button id="b3" onclick="showTab('ald')">Aldehyde (-CHO)</button>
    </div>
    <div id="diagram" class="formula">      CH₃
       |
  CH₃—C⁺  (Planar sp² Carbocation)
       |
      CH₃

  Step 1 (Slow): R—X  →  R⁺ + X⁻
  Step 2 (Fast): R⁺ + Nu⁻  →  R—Nu (Racemic Mixture)</div>
    <p id="summary"><span class="highlight">MDCAT Key Point:</span> Tertiary (3°) alkyl halides favor SN1 (unimolecular, 2-step, racemization).</p>
  </div>
  <script>
    const DATA = {
      sn1: {
        formula: "      CH₃\n       |\n  CH₃—C⁺  (Planar sp² Carbocation)\n       |\n      CH₃\n\n  Step 1 (Slow): R—X  →  R⁺ + X⁻\n  Step 2 (Fast): R⁺ + Nu⁻  →  R—Nu (Racemic Mixture)",
        summary: "<span class='highlight'>MDCAT Key Point:</span> Tertiary (3°) alkyl halides favor SN1 (unimolecular, 2-step, racemization)."
      },
      sn2: {
        formula: "                  H\n                  |\n  Nu⁻  →  [Nu···C···X]‡  →  Nu—C—H + X⁻\n                /  \\\n               H    R",
        summary: "<span class='highlight'>MDCAT Key Point:</span> Primary (1°) alkyl halides favor SN2 (bimolecular, 1-step, 100% Walden inversion)."
      },
      ald: {
        formula: "       O\n       ||\n  R — C — H    (Terminal Carbonyl Group: -CHO)\n\n  Tollens' Test: R—CHO + 2[Ag(NH₃)₂]⁺ + 3OH⁻  →  R—COO⁻ + 2Ag↓ (Silver Mirror)",
        summary: "<span class='highlight'>MDCAT Key Point:</span> Aldehydes reduce Tollens' &amp; Fehling's reagent, whereas ketones do not."
      }
    };
    function showTab(key) {
      document.getElementById('diagram').textContent = DATA[key].formula;
      document.getElementById('summary').innerHTML = DATA[key].summary;
      ['b1','b2','b3'].forEach(id => document.getElementById(id).classList.remove('active'));
      if (key === 'sn1') document.getElementById('b1').classList.add('active');
      if (key === 'sn2') document.getElementById('b2').classList.add('active');
      if (key === 'ald') document.getElementById('b3').classList.add('active');
    }
  </script>
</body>
</html>"""
}
