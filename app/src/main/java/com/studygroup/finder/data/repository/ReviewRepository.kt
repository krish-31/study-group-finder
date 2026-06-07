package com.studygroup.finder.data.repository

import com.studygroup.finder.data.model.Review
import kotlinx.coroutines.flow.Flow

/**
 * Contract for group review operations.
 */
interface ReviewRepository {

    suspend fun submitReview(review: Review): Result<Unit>

    fun getReviewsForGroup(groupId: String): Flow<List<Review>>
}
