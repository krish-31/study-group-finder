package com.studygroup.finder.data.repository

import com.studygroup.finder.data.model.Notification
import kotlinx.coroutines.flow.Flow

/**
 * Contract for in-app notification operations.
 */
interface NotificationRepository {

    suspend fun createNotification(notification: Notification): Result<Unit>

    fun getNotificationsForUser(userId: String): Flow<List<Notification>>

    suspend fun markAsRead(notificationId: String): Result<Unit>
}
