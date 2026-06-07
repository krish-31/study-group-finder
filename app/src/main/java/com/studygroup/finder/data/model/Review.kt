package com.studygroup.finder.data.model

import com.google.firebase.firestore.DocumentId

/**
 * Represents a user review and rating for a study group.
 *
 * @property reviewId Unique identifier (Firestore document ID).
 * @property groupId ID of the group being reviewed.
 * @property reviewerId User ID of the person who left the review.
 * @property rating Numeric rating from 1.0 to 5.0.
 * @property comment Text content of the review.
 * @property createdAt Review creation timestamp in epoch milliseconds.
 */
data class Review(
    @DocumentId
    val reviewId: String = "",
    val groupId: String = "",
    val reviewerId: String = "",
    val rating: Float = 0.0f,
    val comment: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val COLLECTION = "reviews"

        const val MIN_RATING = 1.0f
        const val MAX_RATING = 5.0f
    }
}
