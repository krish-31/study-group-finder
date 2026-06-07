package com.studygroup.finder.data.repository.impl

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.studygroup.finder.data.model.ChatMessage
import com.studygroup.finder.data.repository.ChatRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase-backed implementation of [ChatRepository].
 */
@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : ChatRepository {

    private val messagesCollection = firestore.collection(ChatMessage.COLLECTION)

    override suspend fun sendMessage(message: ChatMessage): Result<Unit> {
        return try {
            messagesCollection.add(message).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getMessagesFlow(groupId: String): Flow<List<ChatMessage>> = callbackFlow {
        val listenerRegistration: ListenerRegistration = messagesCollection
            .whereEqualTo("groupId", groupId)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val messages = snapshot?.toObjects(ChatMessage::class.java).orEmpty()
                trySend(messages)
            }

        awaitClose { listenerRegistration.remove() }
    }
}
