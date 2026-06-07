package com.studygroup.finder.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studygroup.finder.core.utils.ErrorHandler
import com.studygroup.finder.core.utils.NetworkUtils
import com.studygroup.finder.data.model.StudyGroup
import com.studygroup.finder.data.model.User
import com.studygroup.finder.data.repository.AuthRepository
import com.studygroup.finder.data.repository.ReviewRepository
import com.studygroup.finder.data.repository.SessionRepository
import com.studygroup.finder.data.repository.StudyGroupRepository
import com.studygroup.finder.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI state for the Profile screen.
 *
 * @property currentUser       the signed-in user's profile (null while loading).
 * @property joinedGroups      list of groups the user has joined.
 * @property completedSessions count of sessions the user has completed.
 * @property reviewsGiven      count of reviews the user has submitted.
 * @property isLoading         true while data is being fetched.
 * @property errorMessage      non-null when an error needs to be surfaced.
 * @property isProfileUpdated  true after a successful profile update.
 * @property isLoggedOut       true after the user successfully logs out.
 */
data class ProfileUiState(
    val currentUser: User? = null,
    val joinedGroups: List<StudyGroup> = emptyList(),
    val completedSessions: Int = 0,
    val reviewsGiven: Int = 0,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val isProfileUpdated: Boolean = false,
    val isLoggedOut: Boolean = false
)

/**
 * ViewModel for the Profile and Edit Profile screens.
 *
 * Loads the current user's profile, their joined groups, completed session
 * count, and reviews count from Firestore via the repository layer.
 * Exposes [updateProfile] to persist changes and [logout] to sign out.
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val studyGroupRepository: StudyGroupRepository,
    private val sessionRepository: SessionRepository,
    private val reviewRepository: ReviewRepository,
    private val networkUtils: NetworkUtils
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    /**
     * Load the current user's full profile and associated statistics.
     */
    fun loadProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            if (!networkUtils.isNetworkAvailable()) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "No internet connection. Please check your network settings and try again."
                    )
                }
                return@launch
            }

            try {
                val firebaseUser = authRepository.getCurrentUser()
                if (firebaseUser == null) {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = "User not logged in")
                    }
                    return@launch
                }

                // 1. Load user profile
                userRepository.getUserProfile(firebaseUser.uid)
                    .onSuccess { user ->
                        _uiState.update { it.copy(currentUser = user) }

                        // 2. Load joined groups
                        loadJoinedGroups(user)

                        // 3. Load completed sessions count
                        loadCompletedSessions(user)

                        // 4. Load reviews given count
                        loadReviewsGiven(user)
                    }
                    .onFailure { throwable ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = ErrorHandler.getErrorMessage(throwable, networkUtils)
                            )
                        }
                    }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = ErrorHandler.getErrorMessage(e, networkUtils)
                    )
                }
            }
        }
    }

    /**
     * Load all groups that the user has joined.
     */
    private fun loadJoinedGroups(user: User) {
        viewModelScope.launch {
            try {
                studyGroupRepository.getAllGroupsFlow()
                    .catch { throwable ->
                        _uiState.update {
                            it.copy(
                                errorMessage = ErrorHandler.getErrorMessage(throwable, networkUtils)
                            )
                        }
                    }
                    .collect { allGroups ->
                        val joined = allGroups.filter { group ->
                            user.userId in group.members
                        }
                        _uiState.update {
                            it.copy(joinedGroups = joined, isLoading = false)
                        }
                    }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        errorMessage = ErrorHandler.getErrorMessage(e, networkUtils)
                    )
                }
            }
        }
    }

    /**
     * Count completed sessions across all groups the user has joined.
     */
    private fun loadCompletedSessions(user: User) {
        viewModelScope.launch {
            try {
                var totalCompleted = 0
                for (groupId in user.joinedGroups) {
                    sessionRepository.getSessionsForGroup(groupId)
                        .catch { /* skip errors for individual groups */ }
                        .collect { sessions ->
                            totalCompleted += sessions.count { it.status == "completed" }
                        }
                }
                _uiState.update { it.copy(completedSessions = totalCompleted) }
            } catch (e: Exception) {
                // Ignore or log error
            }
        }
    }

    /**
     * Count the number of reviews given by this user across all their groups.
     */
    private fun loadReviewsGiven(user: User) {
        viewModelScope.launch {
            try {
                var totalReviews = 0
                for (groupId in user.joinedGroups) {
                    reviewRepository.getReviewsForGroup(groupId)
                        .catch { /* skip errors for individual groups */ }
                        .collect { reviews ->
                            totalReviews += reviews.count { it.reviewerId == user.userId }
                        }
                }
                _uiState.update { it.copy(reviewsGiven = totalReviews) }
            } catch (e: Exception) {
                // Ignore or log error
            }
        }
    }

    /**
     * Update the current user's profile with new name, bio, and subjects.
     *
     * @param name     new display name.
     * @param bio      new biography text.
     * @param subjects updated list of subject interests.
     */
    fun updateProfile(name: String, bio: String, subjects: List<String>) {
        val currentUser = _uiState.value.currentUser ?: return

        if (name.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Name cannot be empty") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, isProfileUpdated = false) }

            if (!networkUtils.isNetworkAvailable()) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "No internet connection. Please check your network settings and try again."
                    )
                }
                return@launch
            }

            try {
                val updatedUser = currentUser.copy(
                    name = name.trim(),
                    bio = bio.trim(),
                    subjects = subjects
                )

                userRepository.updateUserProfile(updatedUser)
                    .onSuccess {
                        _uiState.update {
                            it.copy(
                                currentUser = updatedUser,
                                isLoading = false,
                                isProfileUpdated = true
                            )
                        }
                    }
                    .onFailure { throwable ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = ErrorHandler.getErrorMessage(throwable, networkUtils)
                            )
                        }
                    }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = ErrorHandler.getErrorMessage(e, networkUtils)
                    )
                }
            }
        }
    }

    /**
     * Sign out the current user.
     */
    fun logout() {
        viewModelScope.launch {
            try {
                authRepository.logoutUser()
                _uiState.update { it.copy(isLoggedOut = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        errorMessage = ErrorHandler.getErrorMessage(e, networkUtils)
                    )
                }
            }
        }
    }

    /** Clear any displayed error after the UI has shown it. */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    /** Reset the profile-updated flag after the UI has acted on it. */
    fun clearProfileUpdated() {
        _uiState.update { it.copy(isProfileUpdated = false) }
    }
}
