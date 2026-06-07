package com.studygroup.finder.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studygroup.finder.data.model.User
import com.studygroup.finder.data.repository.AuthRepository
import com.studygroup.finder.data.repository.UserRepository
import com.studygroup.finder.core.utils.ErrorHandler
import com.studygroup.finder.core.utils.NetworkUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Represents every possible UI state of the authentication screens.
 */
sealed class AuthState {
    /** No operation in progress — initial state. */
    data object Idle : AuthState()

    /** A sign-in / register call is in flight. */
    data object Loading : AuthState()

    /** Authentication succeeded; [user] is the authenticated profile. */
    data class Success(val user: User) : AuthState()

    /** Authentication failed; [message] describes the reason. */
    data class Error(val message: String) : AuthState()
}

/**
 * ViewModel shared between [LoginScreen] and [RegisterScreen].
 *
 * Exposes two [StateFlow]s — one for login state, one for registration —
 * so each screen can observe its own progress independently.
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val networkUtils: NetworkUtils
) : ViewModel() {

    // ── Login state ─────────────────────────────────
    private val _loginState = MutableStateFlow<AuthState>(AuthState.Idle)
    val loginState: StateFlow<AuthState> = _loginState.asStateFlow()

    // ── Register state ──────────────────────────────
    private val _registerState = MutableStateFlow<AuthState>(AuthState.Idle)
    val registerState: StateFlow<AuthState> = _registerState.asStateFlow()

    /**
     * Attempt to log in with [email] and [password].
     *
     * On success the Firestore user profile is fetched (or created
     * if it was somehow missing) and emitted via [loginState].
     */
    fun login(email: String, password: String) {
        viewModelScope.launch {
            _loginState.value = AuthState.Loading
            if (!networkUtils.isNetworkAvailable()) {
                _loginState.value = AuthState.Error("No internet connection. Please check your network settings and try again.")
                return@launch
            }

            try {
                authRepository.loginUser(email, password)
                    .onSuccess { user ->
                        _loginState.value = AuthState.Success(user)
                    }
                    .onFailure { throwable ->
                        _loginState.value = AuthState.Error(
                            ErrorHandler.getErrorMessage(throwable, networkUtils)
                        )
                    }
            } catch (e: Exception) {
                _loginState.value = AuthState.Error(
                    ErrorHandler.getErrorMessage(e, networkUtils)
                )
            }
        }
    }

    /**
     * Register a new account then create its Firestore profile.
     */
    fun register(email: String, password: String, name: String) {
        viewModelScope.launch {
            _registerState.value = AuthState.Loading
            if (!networkUtils.isNetworkAvailable()) {
                _registerState.value = AuthState.Error("No internet connection. Please check your network settings and try again.")
                return@launch
            }

            try {
                authRepository.registerUser(email, password, name)
                    .onSuccess { user ->
                        // Persist user profile to Firestore
                        userRepository.createUserProfile(user)
                            .onSuccess {
                                _registerState.value = AuthState.Success(user)
                            }
                            .onFailure { throwable ->
                                _registerState.value = AuthState.Error(
                                    ErrorHandler.getErrorMessage(throwable, networkUtils)
                                )
                            }
                    }
                    .onFailure { throwable ->
                        _registerState.value = AuthState.Error(
                            ErrorHandler.getErrorMessage(throwable, networkUtils)
                        )
                    }
            } catch (e: Exception) {
                _registerState.value = AuthState.Error(
                    ErrorHandler.getErrorMessage(e, networkUtils)
                )
            }
        }
    }

    /**
     * Sign the current user out of Firebase Auth.
     */
    fun logout() {
        viewModelScope.launch {
            authRepository.logoutUser()
            _loginState.value = AuthState.Idle
            _registerState.value = AuthState.Idle
        }
    }

    /** Check whether a user is currently signed in. */
    fun isLoggedIn(): Boolean = authRepository.isLoggedIn()

    /** Reset login state back to Idle (e.g. after showing an error). */
    fun resetLoginState() {
        _loginState.value = AuthState.Idle
    }

    /** Reset register state back to Idle. */
    fun resetRegisterState() {
        _registerState.value = AuthState.Idle
    }
}
