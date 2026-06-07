package com.studygroup.finder.data.repository

import com.studygroup.finder.data.model.ChatMessage
import kotlinx.coroutines.flow.Flow

/**
 * Contract for group chat operations.
 */
interface ChatRepository {

    suspend fun sendMessage(message: ChatMessage): Result<Unit>

    fun getMessagesFlow(groupId: String): Flow<List<ChatMessage>>
}
