package com.studygroup.finder.data.repository.impl

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.studygroup.finder.data.model.JoinRequest
import com.studygroup.finder.data.repository.JoinRequestRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase-backed implementation of [JoinRequestRepository].
 */
@Singleton
class JoinRequestRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : JoinRequestRepository {

    private val requestsCollection = firestore.collection(JoinRequest.COLLECTION)

    override suspend fun sendJoinRequest(request: JoinRequest): Result<Unit> {
        return try {
            requestsCollection.add(request).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun respondToRequest(requestId: String, status: String): Result<Unit> {
        return try {
            requestsCollection.document(requestId)
                .update("status", status)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getPendingRequestsForGroup(groupId: String): Flow<List<JoinRequest>> =
        callbackFlow {
            val listenerRegistration: ListenerRegistration = requestsCollection
                .whereEqualTo("groupId", groupId)
                .whereEqualTo("status", JoinRequest.STATUS_PENDING)
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }
                    val requests = snapshot?.toObjects(JoinRequest::class.java).orEmpty()
                    trySend(requests)
                }

            awaitClose { listenerRegistration.remove() }
        }

    override fun getRequestsByUser(userId: String): Flow<List<JoinRequest>> = callbackFlow {
        val listenerRegistration: ListenerRegistration = requestsCollection
            .whereEqualTo("senderId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val requests = snapshot?.toObjects(JoinRequest::class.java).orEmpty()
                trySend(requests)
            }

        awaitClose { listenerRegistration.remove() }
    }
}
