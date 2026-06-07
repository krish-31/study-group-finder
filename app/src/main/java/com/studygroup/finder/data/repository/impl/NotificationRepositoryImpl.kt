package com.studygroup.finder.data.repository.impl

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.studygroup.finder.data.model.Notification
import com.studygroup.finder.data.repository.NotificationRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase-backed implementation of [NotificationRepository].
 */
@Singleton
class NotificationRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : NotificationRepository {

    private val notificationsCollection = firestore.collection(Notification.COLLECTION)

    override suspend fun createNotification(notification: Notification): Result<Unit> {
        return try {
            notificationsCollection.add(notification).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getNotificationsForUser(userId: String): Flow<List<Notification>> =
        callbackFlow {
            val listenerRegistration: ListenerRegistration = notificationsCollection
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }
                    val notifications =
                        snapshot?.toObjects(Notification::class.java).orEmpty()
                    trySend(notifications)
                }

            awaitClose { listenerRegistration.remove() }
        }

    override suspend fun markAsRead(notificationId: String): Result<Unit> {
        return try {
            notificationsCollection.document(notificationId)
                .update("isRead", true)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
