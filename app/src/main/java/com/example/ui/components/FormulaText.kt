package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MonoFontFamily

/**
 * Detects whether a pasted string contains multi-line structural chemistry diagrams,
 * aligned reaction equations, or explicit ASCII/Unicode chemical structures whose exact
 * spacing and alignment must be preserved verbatim.
 */
fun isStructuralChemicalText(text: String): Boolean {
    if (!text.contains('\n')) return false
    val lines = text.lines()
    val hasIndentedLine = lines.any { line ->
        line.startsWith("  ") || line.startsWith("\t")
    }
    val hasBondChars = text.any { ch ->
        ch == '|' || ch == '│' || ch == '─' || ch == '═' || ch == '║' ||
            ch == '┌' || ch == '┐' || ch == '└' || ch == '┘' || ch == '╱' || ch == '╲'
    } || text.contains(" /") || text.contains("\\ ") ||
        text.contains("---") || text.contains("===") || text.contains("≡") ||
        text.contains("⇌") || text.contains("→")
    return hasIndentedLine || hasBondChars
}

/**
 * Renders text while strictly preserving pasted Chemistry formulas, Unicode subscripts/superscripts,
 * and multi-line structural diagrams without collapsing spaces or misaligning chemical bonds.
 */
@Composable
fun FormulaText(
    text: String,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip
) {
    val isStructural = remember(text) { isStructuralChemicalText(text) }

    if (isStructural && maxLines == Int.MAX_VALUE) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
            modifier = modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(
                    text = text,
                    style = style.copy(
                        fontFamily = MonoFontFamily,
                        lineHeight = (style.fontSize.value * 1.45f).sp
                    ),
                    color = color,
                    softWrap = false
                )
            }
        }
    } else if (isStructural) {
        Text(
            text = text,
            style = style.copy(fontFamily = MonoFontFamily),
            color = color,
            maxLines = maxLines,
            overflow = overflow,
            modifier = modifier
        )
    } else {
        Text(
            text = text,
            style = style,
            color = color,
            maxLines = maxLines,
            overflow = overflow,
            modifier = modifier
        )
    }
}

/**
 * Compact chemistry symbol & bond quick-insert bar for Admin MCQ authoring so formulas
 * like H₂SO₄, CH₃–CH₂–OH, SO₄²⁻, and reaction equilibria (⇌, →, Δ) can be inserted verbatim.
 */
@Composable
fun ChemistryFormulaToolbar(
    onInsertSymbol: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val groups = remember {
        listOf(
            "→", "⇌", "⇄", "Δ", "°", "↑", "↓",
            "₀", "₁", "₂", "₃", "₄", "₅", "₆", "₇", "₈", "₉",
            "⁺", "⁻", "²⁺", "³⁺", "²⁻", "⁰", "¹", "²", "³",
            "—", "═", "≡", "•", "·", "|", "CH₃", "CH₂", "H₂O", "OH", "COOH"
        )
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "Chemistry Formula Symbols (tap to append to active field):",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            groups.forEach { symbol ->
                Surface(
                    onClick = { onInsertSymbol(symbol) },
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f))
                ) {
                    Text(
                        text = symbol,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = MonoFontFamily,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                    )
                }
            }
        }
    }
}
