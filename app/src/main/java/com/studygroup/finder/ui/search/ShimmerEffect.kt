package com.studygroup.finder.ui.search

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A reusable shimmer-effect composable that draws an animated
 * gradient highlight sweeping across a rounded rectangle.
 *
 * Use this to build placeholder skeletons while data is loading.
 *
 * @param modifier  outer modifier (usually sets width/height).
 * @param height    the height of the shimmer rectangle.
 * @param cornerRadius corner rounding.
 */
@Composable
fun ShimmerEffect(
    modifier: Modifier = Modifier,
    height: Dp = 20.dp,
    cornerRadius: Dp = 8.dp
) {
    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    )

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim - 300f, translateAnim - 300f),
        end = Offset(translateAnim, translateAnim)
    )

    Canvas(
        modifier = modifier.height(height)
    ) {
        val cornerRadiusPx = cornerRadius.toPx()
        drawRoundRect(
            brush = brush,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                cornerRadiusPx, cornerRadiusPx
            )
        )
    }
}

/**
 * A shimmer placeholder that mimics the layout of [GroupCard].
 *
 * Displays animated skeleton shapes for the title, subject chip,
 * member count, and action button, giving users a visual cue that
 * content is loading.
 */
@Composable
fun ShimmerGroupCard(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Title placeholder
            ShimmerEffect(
                modifier = Modifier.fillMaxWidth(0.6f),
                height = 20.dp,
                cornerRadius = 6.dp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Subject chip placeholder
            ShimmerEffect(
                modifier = Modifier.width(90.dp),
                height = 24.dp,
                cornerRadius = 8.dp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Stats row placeholder
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ShimmerEffect(
                    modifier = Modifier.width(64.dp),
                    height = 14.dp,
                    cornerRadius = 4.dp
                )
                ShimmerEffect(
                    modifier = Modifier.width(48.dp),
                    height = 14.dp,
                    cornerRadius = 4.dp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Button placeholder
            ShimmerEffect(
                modifier = Modifier.fillMaxWidth(),
                height = 40.dp,
                cornerRadius = 12.dp
            )
        }
    }
}
