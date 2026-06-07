package com.studygroup.finder.data.repository.impl

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.studygroup.finder.data.model.User
import com.studygroup.finder.data.repository.AuthRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase-backed implementation of [AuthRepository].
 */
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) : AuthRepository {

    override suspend fun registerUser(
        email: String,
        password: String,
        name: String
    ): Result<User> {
        return try {
            val authResult = firebaseAuth
                .createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user
                ?: return Result.failure(Exception("Registration succeeded but user is null"))

            val profileUpdate = UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build()
            firebaseUser.updateProfile(profileUpdate).await()

            val user = User(
                userId = firebaseUser.uid,
                name = name,
                email = email,
                createdAt = System.currentTimeMillis()
            )
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun loginUser(email: String, password: String): Result<User> {
        return try {
            val authResult = firebaseAuth
                .signInWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user
                ?: return Result.failure(Exception("Login succeeded but user is null"))

            val user = User(
                userId = firebaseUser.uid,
                name = firebaseUser.displayName.orEmpty(),
                email = firebaseUser.email.orEmpty()
            )
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getCurrentUser(): FirebaseUser? = firebaseAuth.currentUser

    override suspend fun logoutUser() {
        firebaseAuth.signOut()
    }

    override fun isLoggedIn(): Boolean = firebaseAuth.currentUser != null
}
