package com.studygroup.finder.data.repository

import com.studygroup.finder.data.model.StudySession
import kotlinx.coroutines.flow.Flow

/**
 * Contract for study session operations.
 */
interface SessionRepository {

    suspend fun createSession(session: StudySession): Result<Unit>

    fun getSessionsForGroup(groupId: String): Flow<List<StudySession>>

    suspend fun updateSessionStatus(sessionId: String, status: String): Result<Unit>
}
