package com.studygroup.finder.data.repository

import com.google.firebase.auth.FirebaseUser
import com.studygroup.finder.data.model.User

/**
 * Contract for authentication operations.
 */
interface AuthRepository {

    suspend fun registerUser(email: String, password: String, name: String): Result<User>

    suspend fun loginUser(email: String, password: String): Result<User>

    fun getCurrentUser(): FirebaseUser?

    suspend fun logoutUser()

    fun isLoggedIn(): Boolean
}
