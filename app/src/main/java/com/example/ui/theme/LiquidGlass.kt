package com.example.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun LiquidGlassBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val ext = LocalMontanuColors.current
    val bgColor = MaterialTheme.colorScheme.background
    val glowPrimary = ext.crimsonGlow
    val glowSecondary = if (ext.isDark) Color(0x1A38BDF8) else Color(0x14B5122A)

    Box(
        modifier = modifier
            .background(bgColor)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(glowPrimary, Color.Transparent),
                        center = Offset(size.width * 0.85f, size.height * 0.08f),
                        radius = size.width * 0.85f
                    ),
                    radius = size.width * 0.85f,
                    center = Offset(size.width * 0.85f, size.height * 0.08f)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(glowSecondary, Color.Transparent),
                        center = Offset(size.width * 0.1f, size.height * 0.78f),
                        radius = size.width * 0.75f
                    ),
                    radius = size.width * 0.75f,
                    center = Offset(size.width * 0.1f, size.height * 0.78f)
                )
            },
        content = content
    )
}

@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    highlightBorder: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val ext = LocalMontanuColors.current
    val shape: Shape = RoundedCornerShape(cornerRadius)
    val fillBrush = Brush.linearGradient(
        colors = listOf(ext.glassFillStart, ext.glassFillEnd),
        start = Offset.Zero,
        end = Offset(600f, 600f)
    )
    val borderBrush = if (highlightBorder) {
        Brush.linearGradient(
            colors = listOf(MaterialTheme.colorScheme.primary, ext.glassBorderTop, ext.glassBorderBottom)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(ext.glassBorderTop, ext.glassBorderBottom)
        )
    }

    val clickableMod = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(brush = fillBrush, shape = shape)
            .border(width = 1.dp, brush = borderBrush, shape = shape)
            .then(clickableMod)
            .padding(contentPadding),
        content = content
    )
}
