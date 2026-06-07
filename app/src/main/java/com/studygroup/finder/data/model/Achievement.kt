package com.studygroup.finder.data.model

import com.google.firebase.firestore.DocumentId

/**
 * Represents a gamification achievement earned by a user.
 *
 * @property achievementId Unique identifier (Firestore document ID).
 * @property userId ID of the user who earned this achievement.
 * @property title Short title of the achievement (e.g., "First Session").
 * @property description Detailed description of what the user did to earn it.
 * @property earnedAt Timestamp when the achievement was earned, in epoch milliseconds.
 */
data class Achievement(
    @DocumentId
    val achievementId: String = "",
    val userId: String = "",
    val title: String = "",
    val description: String = "",
    val earnedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val COLLECTION = "achievements"
    }
}
