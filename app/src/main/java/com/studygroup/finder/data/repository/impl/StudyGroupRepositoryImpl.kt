package com.studygroup.finder.data.repository.impl

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.studygroup.finder.data.model.StudyGroup
import com.studygroup.finder.data.repository.StudyGroupRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase-backed implementation of [StudyGroupRepository].
 */
@Singleton
class StudyGroupRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : StudyGroupRepository {

    private val groupsCollection = firestore.collection(StudyGroup.COLLECTION)

    override suspend fun createGroup(group: StudyGroup): Result<String> {
        return try {
            val docRef = groupsCollection.add(group).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getGroupById(groupId: String): Result<StudyGroup> {
        return try {
            val snapshot = groupsCollection.document(groupId).get().await()
            val group = snapshot.toObject(StudyGroup::class.java)
                ?: return Result.failure(Exception("Study group not found: $groupId"))
            Result.success(group)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getAllGroupsFlow(): Flow<List<StudyGroup>> = callbackFlow {
        val listenerRegistration: ListenerRegistration = groupsCollection
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val groups = snapshot?.toObjects(StudyGroup::class.java).orEmpty()
                trySend(groups)
            }

        awaitClose { listenerRegistration.remove() }
    }

    override fun searchGroupsFlow(query: String): Flow<List<StudyGroup>> = callbackFlow {
        val endQuery = query + "\uf8ff"

        val listenerRegistration: ListenerRegistration = groupsCollection
            .orderBy("name")
            .startAt(query)
            .endAt(endQuery)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val groups = snapshot?.toObjects(StudyGroup::class.java).orEmpty()
                trySend(groups)
            }

        awaitClose { listenerRegistration.remove() }
    }

    override suspend fun updateGroup(group: StudyGroup): Result<Unit> {
        return try {
            groupsCollection.document(group.groupId).set(group).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteGroup(groupId: String): Result<Unit> {
        return try {
            groupsCollection.document(groupId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addMemberToGroup(groupId: String, userId: String): Result<Unit> {
        return try {
            groupsCollection.document(groupId)
                .update("members", FieldValue.arrayUnion(userId))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
