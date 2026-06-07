package com.studygroup.finder.ui.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.studygroup.finder.data.model.Notification
import java.util.concurrent.TimeUnit

/**
 * A single notification item in the list.
 *
 * - Icon varies by notification type (join_request, session, announcement).
 * - Title is bold when unread.
 * - Includes a relative timestamp ("2 hours ago", "Yesterday", etc.).
 * - Unread items get a subtle blue-tinted background.
 *
 * @param notification  the [Notification] to display.
 * @param onClick       called when the item is tapped.
 * @param modifier      optional outer modifier.
 */
@Composable
fun NotificationItem(
    notification: Notification,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val icon: ImageVector
    val iconTint: Color
    val iconBg: Color

    when (notification.type) {
        Notification.TYPE_JOIN_REQUEST -> {
            icon = Icons.Default.PersonAdd
            iconTint = Color(0xFF3B82F6)
            iconBg = Color(0xFF3B82F6).copy(alpha = 0.12f)
        }
        Notification.TYPE_SESSION -> {
            icon = Icons.Default.CalendarToday
            iconTint = Color(0xFF22C55E)
            iconBg = Color(0xFF22C55E).copy(alpha = 0.12f)
        }
        else -> { // announcement
            icon = Icons.Default.Campaign
            iconTint = Color(0xFFF59E0B)
            iconBg = Color(0xFFF59E0B).copy(alpha = 0.12f)
        }
    }

    val relativeTime = formatRelativeTime(notification.createdAt)

    val cardBackground = if (!notification.isRead)
        MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
    else
        MaterialTheme.colorScheme.surface

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (!notification.isRead) 1.dp else 0.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Type icon
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = notification.type,
                    modifier = Modifier.size(22.dp),
                    tint = iconTint
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Title
                Text(
                    text = notification.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (!notification.isRead) FontWeight.Bold
                        else FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Message body
                Text(
                    text = notification.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )

                // Timestamp
                Text(
                    text = relativeTime,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Unread indicator dot
            if (!notification.isRead) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .align(Alignment.CenterVertically)
                )
            }
        }
    }
}

// ── Relative time formatting ────────────────────────────

/**
 * Convert an epoch-millis timestamp into a human-readable relative
 * string like "Just now", "5 min ago", "2 hours ago", "Yesterday".
 */
private fun formatRelativeTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
    val hours = TimeUnit.MILLISECONDS.toHours(diff)
    val days = TimeUnit.MILLISECONDS.toDays(diff)

    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "$minutes min ago"
        hours < 24 -> "$hours ${if (hours == 1L) "hour" else "hours"} ago"
        days == 1L -> "Yesterday"
        days < 7 -> "$days days ago"
        days < 30 -> "${days / 7} ${if (days / 7 == 1L) "week" else "weeks"} ago"
        else -> "${days / 30} ${if (days / 30 == 1L) "month" else "months"} ago"
    }
}
