package com.studygroup.finder.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studygroup.finder.core.utils.ErrorHandler
import com.studygroup.finder.core.utils.NetworkUtils
import com.studygroup.finder.data.model.JoinRequest
import com.studygroup.finder.data.model.StudyGroup
import com.studygroup.finder.data.model.User
import com.studygroup.finder.data.repository.AuthRepository
import com.studygroup.finder.data.repository.JoinRequestRepository
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
 * UI state for the Admin Panel screen.
 *
 * @property currentUser       the signed-in admin's profile.
 * @property isAdmin           whether the current user has admin privileges.
 * @property allUsers          all registered users in the system.
 * @property allGroups         all study groups in the system.
 * @property pendingRequests   all pending join requests across groups.
 * @property totalReviews      total number of reviews across all groups.
 * @property activeSessions    count of sessions with "active" status today.
 * @property isLoading         true while data is being fetched.
 * @property errorMessage      non-null when an error needs to be surfaced.
 * @property successMessage    non-null when a success action needs to be surfaced.
 */
data class AdminUiState(
    val currentUser: User? = null,
    val isAdmin: Boolean = false,
    val allUsers: List<User> = emptyList(),
    val allGroups: List<StudyGroup> = emptyList(),
    val pendingRequests: List<JoinRequest> = emptyList(),
    val totalReviews: Int = 0,
    val activeSessions: Int = 0,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

/**
 * ViewModel for the Admin Panel screen.
 *
 * Provides admin-only operations: listing all users, approving users,
 * listing all groups, deleting groups, and viewing pending join requests.
 * Also computes summary statistics for the Reports tab.
 */
@HiltViewModel
class AdminViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val studyGroupRepository: StudyGroupRepository,
    private val joinRequestRepository: JoinRequestRepository,
    private val sessionRepository: SessionRepository,
    private val reviewRepository: ReviewRepository,
    private val networkUtils: NetworkUtils
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    init {
        checkAdminAndLoad()
    }

    /**
     * Verify the current user is an admin, then load all admin data.
     */
    private fun checkAdminAndLoad() {
        viewModelScope.launch {
            if (!networkUtils.isNetworkAvailable()) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isAdmin = false,
                        errorMessage = "No internet connection. Please check your network settings and try again."
                    )
                }
                return@launch
            }

            try {
                val firebaseUser = authRepository.getCurrentUser()
                if (firebaseUser == null) {
                    _uiState.update {
                        it.copy(isLoading = false, isAdmin = false, errorMessage = "Not logged in")
                    }
                    return@launch
                }

                userRepository.getUserProfile(firebaseUser.uid)
                    .onSuccess { user ->
                        _uiState.update {
                            it.copy(currentUser = user, isAdmin = user.isAdmin)
                        }
                        if (user.isAdmin) {
                            loadAllUsers()
                            loadAllGroups()
                            loadAllPendingRequests()
                        } else {
                            _uiState.update { it.copy(isLoading = false) }
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
     * Stream all registered users.
     */
    private fun loadAllUsers() {
        viewModelScope.launch {
            try {
                userRepository.getAllUsersFlow()
                    .catch { throwable ->
                        _uiState.update {
                            it.copy(errorMessage = ErrorHandler.getErrorMessage(throwable, networkUtils))
                        }
                    }
                    .collect { users ->
                        _uiState.update {
                            it.copy(allUsers = users, isLoading = false)
                        }
                    }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = ErrorHandler.getErrorMessage(e, networkUtils))
                }
            }
        }
    }

    /**
     * Stream all study groups.
     */
    private fun loadAllGroups() {
        viewModelScope.launch {
            try {
                studyGroupRepository.getAllGroupsFlow()
                    .catch { throwable ->
                        _uiState.update {
                            it.copy(errorMessage = ErrorHandler.getErrorMessage(throwable, networkUtils))
                        }
                    }
                    .collect { groups ->
                        _uiState.update { it.copy(allGroups = groups) }
                        // Count active sessions across all groups
                        loadActiveSessionsCount(groups)
                        // Count total reviews across all groups
                        loadTotalReviewsCount(groups)
                    }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = ErrorHandler.getErrorMessage(e, networkUtils))
                }
            }
        }
    }

    /**
     * Stream all pending join requests.
     */
    private fun loadAllPendingRequests() {
        viewModelScope.launch {
            try {
                joinRequestRepository.getAllPendingRequestsFlow()
                    .catch { throwable ->
                        _uiState.update {
                            it.copy(errorMessage = ErrorHandler.getErrorMessage(throwable, networkUtils))
                        }
                    }
                    .collect { requests ->
                        _uiState.update { it.copy(pendingRequests = requests) }
                    }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = ErrorHandler.getErrorMessage(e, networkUtils))
                }
            }
        }
    }

    /**
     * Count sessions with "active" status across all groups.
     */
    private fun loadActiveSessionsCount(groups: List<StudyGroup>) {
        viewModelScope.launch {
            try {
                var activeCount = 0
                for (group in groups) {
                    sessionRepository.getSessionsForGroup(group.groupId)
                        .catch { /* skip errors for individual groups */ }
                        .collect { sessions ->
                            activeCount += sessions.count { it.status == "active" }
                        }
                }
                _uiState.update { it.copy(activeSessions = activeCount) }
            } catch (e: Exception) {
                // Ignore or log error
            }
        }
    }

    /**
     * Count total reviews across all groups.
     */
    private fun loadTotalReviewsCount(groups: List<StudyGroup>) {
        viewModelScope.launch {
            try {
                var reviewCount = 0
                for (group in groups) {
                    reviewRepository.getReviewsForGroup(group.groupId)
                        .catch { /* skip errors for individual groups */ }
                        .collect { reviews ->
                            reviewCount += reviews.size
                        }
                }
                _uiState.update { it.copy(totalReviews = reviewCount) }
            } catch (e: Exception) {
                // Ignore or log error
            }
        }
    }

    /**
     * Approve a user by setting their [User.isApproved] flag to true.
     */
    fun approveUser(userId: String) {
        viewModelScope.launch {
            if (!networkUtils.isNetworkAvailable()) {
                _uiState.update {
                    it.copy(
                        errorMessage = "No internet connection. Please check your network settings and try again."
                    )
                }
                return@launch
            }

            try {
                val user = _uiState.value.allUsers.find { it.userId == userId } ?: return@launch
                val approvedUser = user.copy(isApproved = true)

                userRepository.updateUserProfile(approvedUser)
                    .onSuccess {
                        _uiState.update {
                            it.copy(
                                allUsers = it.allUsers.map { u ->
                                    if (u.userId == userId) approvedUser else u
                                },
                                successMessage = "${user.name} has been approved"
                            )
                        }
                    }
                    .onFailure { throwable ->
                        _uiState.update {
                            it.copy(
                                errorMessage = ErrorHandler.getErrorMessage(throwable, networkUtils)
                            )
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
     * Delete a study group by its ID.
     */
    fun deleteGroup(groupId: String) {
        viewModelScope.launch {
            if (!networkUtils.isNetworkAvailable()) {
                _uiState.update {
                    it.copy(
                        errorMessage = "No internet connection. Please check your network settings and try again."
                    )
                }
                return@launch
            }

            try {
                val group = _uiState.value.allGroups.find { it.groupId == groupId }

                studyGroupRepository.deleteGroup(groupId)
                    .onSuccess {
                        _uiState.update {
                            it.copy(
                                allGroups = it.allGroups.filter { g -> g.groupId != groupId },
                                successMessage = "${group?.name ?: "Group"} has been deleted"
                            )
                        }
                    }
                    .onFailure { throwable ->
                        _uiState.update {
                            it.copy(
                                errorMessage = ErrorHandler.getErrorMessage(throwable, networkUtils)
                            )
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

    /** Clear any displayed error after the UI has shown it. */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    /** Clear any displayed success message after the UI has shown it. */
    fun clearSuccess() {
        _uiState.update { it.copy(successMessage = null) }
    }
}
