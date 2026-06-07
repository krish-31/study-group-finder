package com.studygroup.finder.data.model

import com.google.firebase.firestore.DocumentId

/**
 * Represents a scheduled study session within a group.
 *
 * @property sessionId Unique identifier (Firestore document ID).
 * @property groupId ID of the group hosting this session.
 * @property title Short title or topic for the session.
 * @property dateTime Scheduled start time in epoch milliseconds.
 * @property durationMinutes Expected duration of the session in minutes.
 * @property location Physical location or "Online" for virtual sessions.
 * @property status Current status: "upcoming", "active", or "completed".
 */
data class StudySession(
    @DocumentId
    val sessionId: String = "",
    val groupId: String = "",
    val title: String = "",
    val dateTime: Long = System.currentTimeMillis(),
    val durationMinutes: Int = 60,
    val location: String = "Online",
    val status: String = STATUS_UPCOMING
) {
    companion object {
        const val COLLECTION = "study_sessions"

        const val STATUS_UPCOMING = "upcoming"
        const val STATUS_ACTIVE = "active"
        const val STATUS_COMPLETED = "completed"
    }
}
