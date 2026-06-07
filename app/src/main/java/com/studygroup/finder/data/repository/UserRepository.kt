package com.studygroup.finder.data.repository

import com.studygroup.finder.data.model.User
import kotlinx.coroutines.flow.Flow

/**
 * Contract for user profile operations.
 */
interface UserRepository {

    suspend fun createUserProfile(user: User): Result<Unit>

    suspend fun getUserProfile(userId: String): Result<User>

    suspend fun updateUserProfile(user: User): Result<Unit>

    fun getUserProfileFlow(userId: String): Flow<User?>

    /** Stream all registered users (used by admin panel). */
    fun getAllUsersFlow(): Flow<List<User>>
}
