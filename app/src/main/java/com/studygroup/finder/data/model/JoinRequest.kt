package com.studygroup.finder.data.model

import com.google.firebase.firestore.DocumentId

/**
 * Represents a request from a user to join a private study group.
 *
 * @property requestId Unique identifier (Firestore document ID).
 * @property groupId ID of the group being requested to join.
 * @property senderId User ID of the person requesting to join.
 * @property senderName Display name of the requester (denormalized for quick display).
 * @property status Current status: "pending", "accepted", or "rejected".
 * @property createdAt Request creation timestamp in epoch milliseconds.
 */
data class JoinRequest(
    @DocumentId
    val requestId: String = "",
    val groupId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val status: String = STATUS_PENDING,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val COLLECTION = "join_requests"

        const val STATUS_PENDING = "pending"
        const val STATUS_ACCEPTED = "accepted"
        const val STATUS_REJECTED = "rejected"
    }
}
