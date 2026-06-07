package com.studygroup.finder.data.model

import com.google.firebase.firestore.DocumentId

/**
 * Represents an in-app notification delivered to a user.
 *
 * @property notificationId Unique identifier (Firestore document ID).
 * @property userId ID of the user this notification is for.
 * @property title Short notification title.
 * @property message Detailed notification body text.
 * @property type Category of notification: "join_request", "session", or "announcement".
 * @property isRead Whether the user has viewed this notification.
 * @property createdAt Notification creation timestamp in epoch milliseconds.
 */
data class Notification(
    @DocumentId
    val notificationId: String = "",
    val userId: String = "",
    val title: String = "",
    val message: String = "",
    val type: String = TYPE_ANNOUNCEMENT,
    val isRead: Boolean = false,
    val groupId: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val COLLECTION = "notifications"

        const val TYPE_JOIN_REQUEST = "join_request"
        const val TYPE_SESSION = "session"
        const val TYPE_ANNOUNCEMENT = "announcement"
    }
}
