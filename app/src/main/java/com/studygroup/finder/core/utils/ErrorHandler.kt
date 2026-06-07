package com.studygroup.finder.core.utils

import com.google.firebase.firestore.FirebaseFirestoreException
import java.io.IOException

/**
 * Centrally manages error classification and translates exceptions to user-friendly messages.
 */
object ErrorHandler {

    /**
     * Translates a throwable to a user-friendly display message.
     */
    fun getErrorMessage(throwable: Throwable, networkUtils: NetworkUtils): String {
        if (!networkUtils.isNetworkAvailable()) {
            return "No internet connection. Please check your network settings and try again."
        }
        return when (throwable) {
            is FirebaseFirestoreException -> {
                when (throwable.code) {
                    FirebaseFirestoreException.Code.PERMISSION_DENIED -> 
                        "Access denied. You don't have permission to perform this action."
                    FirebaseFirestoreException.Code.UNAVAILABLE -> 
                        "The study group database is temporarily unavailable. Please try again later."
                    FirebaseFirestoreException.Code.NOT_FOUND -> 
                        "The requested study group or resource was not found."
                    FirebaseFirestoreException.Code.ALREADY_EXISTS -> 
                        "The resource already exists."
                    FirebaseFirestoreException.Code.ABORTED ->
                        "The database operation was aborted. Please retry."
                    FirebaseFirestoreException.Code.DEADLINE_EXCEEDED ->
                        "Connection timed out. Please check your network and try again."
                    else -> "Database error: ${throwable.localizedMessage ?: "Unknown database error"}"
                }
            }
            is IOException -> "Network issue: failed to connect to study group finder servers."
            is com.google.firebase.auth.FirebaseAuthInvalidUserException ->
                "Account not found. Please register or verify your credentials."
            is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException ->
                "Invalid email or password. Please try again."
            is com.google.firebase.auth.FirebaseAuthUserCollisionException ->
                "An account with this email address already exists."
            else -> throwable.localizedMessage ?: "An unexpected error occurred. Please try again."
        }
    }
}
