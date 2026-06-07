package com.studygroup.finder.data.repository

import com.studygroup.finder.data.model.JoinRequest
import kotlinx.coroutines.flow.Flow

/**
 * Contract for join request operations.
 */
interface JoinRequestRepository {

    suspend fun sendJoinRequest(request: JoinRequest): Result<Unit>

    suspend fun respondToRequest(requestId: String, status: String): Result<Unit>

    fun getPendingRequestsForGroup(groupId: String): Flow<List<JoinRequest>>

    fun getRequestsByUser(userId: String): Flow<List<JoinRequest>>
}
