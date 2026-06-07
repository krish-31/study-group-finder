package com.studygroup.finder.data.repository.impl

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.studygroup.finder.data.model.Review
import com.studygroup.finder.data.repository.ReviewRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase-backed implementation of [ReviewRepository].
 */
@Singleton
class ReviewRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : ReviewRepository {

    private val reviewsCollection = firestore.collection(Review.COLLECTION)

    override suspend fun submitReview(review: Review): Result<Unit> {
        return try {
            reviewsCollection.add(review).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getReviewsForGroup(groupId: String): Flow<List<Review>> = callbackFlow {
        val listenerRegistration: ListenerRegistration = reviewsCollection
            .whereEqualTo("groupId", groupId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val reviews = snapshot?.toObjects(Review::class.java).orEmpty()
                trySend(reviews)
            }

        awaitClose { listenerRegistration.remove() }
    }
}
