package com.studygroup.finder.core.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Reusable utility functions for formatting text and dates in the UI.
 */
object UiUtils {

    /**
     * Convert an epoch-millis timestamp into a human-readable relative
     * string like "Just now", "5 min ago", "2 hours ago", "Yesterday".
     */
    fun formatTimestamp(epochMs: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - epochMs

        val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
        val hours = TimeUnit.MILLISECONDS.toHours(diff)
        val days = TimeUnit.MILLISECONDS.toDays(diff)

        return when {
            diff < 0 -> "Just now"
            minutes < 1 -> "Just now"
            minutes < 60 -> "$minutes min ago"
            hours < 24 -> "$hours ${if (hours == 1L) "hour" else "hours"} ago"
            days == 1L -> "Yesterday"
            days < 7 -> "$days days ago"
            days < 30 -> "${days / 7} ${if (days / 7 == 1L) "week" else "weeks"} ago"
            else -> "${days / 30} ${if (days / 30 == 1L) "month" else "months"} ago"
        }
    }

    /**
     * Format date-time epoch millisecond into a string format like "Mon, 12 Jun at 3:00 PM".
     */
    fun formatDateTime(epochMs: Long): String {
        val date = Date(epochMs)
        val sdf = SimpleDateFormat("EEE, dd MMM 'at' h:mm a", Locale.getDefault())
        return sdf.format(date)
    }
}
