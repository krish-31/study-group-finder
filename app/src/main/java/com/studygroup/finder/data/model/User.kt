package com.studygroup.finder.data.model

import com.google.firebase.firestore.DocumentId

/**
 * Represents a registered user in the Study Group Finder app.
 *
 * @property userId Unique identifier (Firestore document ID).
 * @property name Display name of the user.
 * @property email Email address used for authentication.
 * @property profilePicUrl URL to the user's profile picture.
 * @property bio Short biography or description.
 * @property subjects List of academic subjects the user is interested in.
 * @property joinedGroups List of group IDs the user has joined.
 * @property createdAt Account creation timestamp in epoch milliseconds.
 * @property isApproved Whether the user is approved by an admin (default true).
 */
data class User(
    @DocumentId
    val userId: String = "",
    val name: String = "",
    val email: String = "",
    val profilePicUrl: String = "",
    val bio: String = "",
    val subjects: List<String> = emptyList(),
    val joinedGroups: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val isApproved: Boolean = true
) {
    companion object {
        const val COLLECTION = "users"
    }
}
