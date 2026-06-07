package com.studygroup.finder.data.repository.impl

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.studygroup.finder.data.model.StudySession
import com.studygroup.finder.data.repository.SessionRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase-backed implementation of [SessionRepository].
 */
@Singleton
class SessionRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : SessionRepository {

    private val sessionsCollection = firestore.collection(StudySession.COLLECTION)

    override suspend fun createSession(session: StudySession): Result<Unit> {
        return try {
            sessionsCollection.add(session).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getSessionsForGroup(groupId: String): Flow<List<StudySession>> = callbackFlow {
        val listenerRegistration: ListenerRegistration = sessionsCollection
            .whereEqualTo("groupId", groupId)
            .orderBy("dateTime", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val sessions = snapshot?.toObjects(StudySession::class.java).orEmpty()
                trySend(sessions)
            }

        awaitClose { listenerRegistration.remove() }
    }

    override suspend fun updateSessionStatus(sessionId: String, status: String): Result<Unit> {
        return try {
            sessionsCollection.document(sessionId)
                .update("status", status)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
