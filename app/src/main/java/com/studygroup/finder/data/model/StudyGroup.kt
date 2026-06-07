package com.studygroup.finder.data.model

import com.google.firebase.firestore.DocumentId

/**
 * Represents a study group that users can create, join, and collaborate in.
 *
 * @property groupId Unique identifier (Firestore document ID).
 * @property name Display name of the study group.
 * @property subject Academic subject or topic the group focuses on.
 * @property description Detailed description of the group's purpose.
 * @property createdBy User ID of the group creator.
 * @property members List of user IDs who are members of this group.
 * @property maxMembers Maximum number of members allowed.
 * @property isPrivate Whether the group requires approval to join.
 * @property createdAt Group creation timestamp in epoch milliseconds.
 * @property rating Average rating of the group (1.0–5.0).
 */
data class StudyGroup(
    @DocumentId
    val groupId: String = "",
    val name: String = "",
    val subject: String = "",
    val description: String = "",
    val createdBy: String = "",
    val members: List<String> = emptyList(),
    val maxMembers: Int = 10,
    val isPrivate: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val rating: Double = 0.0
) {
    companion object {
        const val COLLECTION = "study_groups"
    }
}
