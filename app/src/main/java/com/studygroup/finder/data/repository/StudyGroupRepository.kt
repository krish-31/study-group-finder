package com.studygroup.finder.data.repository

import com.studygroup.finder.data.model.StudyGroup
import kotlinx.coroutines.flow.Flow

/**
 * Contract for study group operations.
 */
interface StudyGroupRepository {

    suspend fun createGroup(group: StudyGroup): Result<String>

    suspend fun getGroupById(groupId: String): Result<StudyGroup>

    fun getAllGroupsFlow(): Flow<List<StudyGroup>>

    fun searchGroupsFlow(query: String): Flow<List<StudyGroup>>

    suspend fun updateGroup(group: StudyGroup): Result<Unit>

    suspend fun deleteGroup(groupId: String): Result<Unit>

    suspend fun addMemberToGroup(groupId: String, userId: String): Result<Unit>
}
