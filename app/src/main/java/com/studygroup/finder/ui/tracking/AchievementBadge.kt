package com.studygroup.finder.ui.tracking

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Data class representing an achievement badge's visual definition.
 *
 * @property title           display title (e.g. "First Group").
 * @property icon            vector icon rendered in the badge circle.
 * @property gradientColors  the two gradient colors for the unlocked background.
 * @property isUnlocked      whether the user has earned this achievement.
 */
data class AchievementDef(
    val title: String,
    val icon: ImageVector,
    val gradientColors: List<Color>,
    val isUnlocked: Boolean
)

/** Pre-defined achievement badges used in the Activity Tracking screen. */
val predefinedAchievements = listOf(
    AchievementDef(
        title = "First Group",
        icon = Icons.Default.Groups,
        gradientColors = listOf(Color(0xFF1565C0), Color(0xFF42A5F5)),
        isUnlocked = false
    ),
    AchievementDef(
        title = "5 Sessions",
        icon = Icons.Default.MenuBook,
        gradientColors = listOf(Color(0xFF00897B), Color(0xFF4DB6AC)),
        isUnlocked = false
    ),
    AchievementDef(
        title = "Top Contributor",
        icon = Icons.Default.Star,
        gradientColors = listOf(Color(0xFFFF8F00), Color(0xFFFFCA28)),
        isUnlocked = false
    ),
    AchievementDef(
        title = "Study Streak",
        icon = Icons.Default.LocalFireDepartment,
        gradientColors = listOf(Color(0xFFD84315), Color(0xFFFF7043)),
        isUnlocked = false
    ),
    AchievementDef(
        title = "Scholar",
        icon = Icons.Default.School,
        gradientColors = listOf(Color(0xFF6A1B9A), Color(0xFFAB47BC)),
        isUnlocked = false
    ),
    AchievementDef(
        title = "Elite Member",
        icon = Icons.Default.WorkspacePremium,
        gradientColors = listOf(Color(0xFFC6A700), Color(0xFFFFD54F)),
        isUnlocked = false
    )
)

/**
 * Composable showing an achievement badge with:
 * - Circular icon with gradient background (when unlocked)
 * - Greyed-out, locked visual state (when locked)
 * - Achievement title below the icon
 * - Subtle pulse animation on unlocked badges
 *
 * @param achievement the [AchievementDef] to render.
 * @param modifier    optional [Modifier].
 */
@Composable
fun AchievementBadge(
    achievement: AchievementDef,
    modifier: Modifier = Modifier
) {
    // Subtle pulse animation for unlocked badges
    val infiniteTransition = rememberInfiniteTransition(label = "badge_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (achievement.isUnlocked) 1.06f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Column(
        modifier = modifier.width(80.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .scale(pulseScale)
                .clip(CircleShape)
                .background(
                    if (achievement.isUnlocked) {
                        Brush.linearGradient(achievement.gradientColors)
                    } else {
                        Brush.linearGradient(
                            listOf(
                                Color(0xFF9E9E9E),
                                Color(0xFFBDBDBD)
                            )
                        )
                    }
                )
                .alpha(if (achievement.isUnlocked) 1f else 0.5f),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (achievement.isUnlocked) {
                    achievement.icon
                } else {
                    Icons.Default.Lock
                },
                contentDescription = achievement.title,
                modifier = Modifier.size(30.dp),
                tint = Color.White
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = achievement.title,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (achievement.isUnlocked) FontWeight.Bold else FontWeight.Normal
            ),
            color = if (achievement.isUnlocked) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            },
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
