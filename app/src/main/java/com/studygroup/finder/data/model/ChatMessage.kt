package com.studygroup.finder.data.model

import com.google.firebase.firestore.DocumentId

/**
 * Represents a single chat message within a study group conversation.
 *
 * @property messageId Unique identifier (Firestore document ID).
 * @property groupId ID of the group this message belongs to.
 * @property senderId User ID of the message author.
 * @property senderName Display name of the sender (denormalized for quick display).
 * @property content Text content of the message.
 * @property timestamp Message creation timestamp in epoch milliseconds.
 */
data class ChatMessage(
    @DocumentId
    val messageId: String = "",
    val groupId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        const val COLLECTION = "chat_messages"
    }
}
