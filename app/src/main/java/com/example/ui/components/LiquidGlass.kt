package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalMedPrepColors

data class LiquidGlassTabItem(
    val id: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

/**
 * Translucent Liquid Glass container with layered frosted gradient fill and luminous specular rim border.
 */
@Composable
fun LiquidGlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    content: @Composable () -> Unit
) {
    val ext = LocalMedPrepColors.current
    val baseBg = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f)
    Box(
        modifier = modifier
            .clip(shape)
            .background(baseBg)
            .background(
                Brush.verticalGradient(
                    colors = listOf(ext.glassFillTop, ext.glassFillBottom)
                )
            )
            .border(
                width = 1.25.dp,
                brush = Brush.linearGradient(
                    colors = listOf(ext.glassBorderHighlight, ext.glassBorderSubtle)
                ),
                shape = shape
            )
    ) {
        content()
    }
}

/**
 * Floating Liquid Glass bottom navigation capsule with animated frosted active pill
 * and safe navigationBarsPadding() for edge-to-edge gesture/3-button bars.
 */
@Composable
fun LiquidGlassBottomNavBar(
    items: List<LiquidGlassTabItem>,
    selectedId: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalMedPrepColors.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        LiquidGlassSurface(
            shape = RoundedCornerShape(30.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { item ->
                    val selected = item.id == selectedId
                    val contentColor by animateColorAsState(
                        targetValue = if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "glass_nav_color"
                    )
                    val pillShape = RoundedCornerShape(24.dp)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 52.dp)
                            .clip(pillShape)
                            .then(
                                if (selected) {
                                    Modifier
                                        .background(
                                            Brush.linearGradient(
                                                colors = listOf(
                                                    ext.glassActivePillStart,
                                                    ext.glassActivePillEnd
                                                )
                                            )
                                        )
                                        .border(
                                            width = 1.dp,
                                            brush = Brush.verticalGradient(
                                                colors = listOf(
                                                    ext.glassBorderHighlight,
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                                )
                                            ),
                                            shape = pillShape
                                        )
                                } else {
                                    Modifier
                                }
                            )
                            .clickable { onSelect(item.id) }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                            .testTag(item.testTag),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.label,
                                tint = contentColor,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = item.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = contentColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Scrollable Liquid Glass pill tab bar for multi-tab hubs (e.g., 8-tab Three-Dots Hub).
 */
@Composable
fun LiquidGlassScrollableTabBar(
    items: List<LiquidGlassTabItem>,
    selectedId: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalMedPrepColors.current
    LiquidGlassSurface(
        shape = RoundedCornerShape(26.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val selected = item.id == selectedId
                val contentColor by animateColorAsState(
                    targetValue = if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "glass_tab_color"
                )
                val pillShape = RoundedCornerShape(20.dp)

                Row(
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .clip(pillShape)
                        .then(
                            if (selected) {
                                Modifier
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(
                                                ext.glassActivePillStart,
                                                ext.glassActivePillEnd
                                            )
                                        )
                                    )
                                    .border(
                                        width = 1.dp,
                                        brush = Brush.verticalGradient(
                                            colors = listOf(
                                                ext.glassBorderHighlight,
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
                                            )
                                        ),
                                        shape = pillShape
                                    )
                            } else {
                                Modifier.background(Color.Transparent)
                            }
                        )
                        .clickable { onSelect(item.id) }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .testTag(item.testTag),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Icon(
                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label,
                        tint = contentColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = contentColor,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
