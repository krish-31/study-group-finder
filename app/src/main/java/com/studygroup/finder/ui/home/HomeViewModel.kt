package com.studygroup.finder.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studygroup.finder.data.model.StudyGroup
import com.studygroup.finder.data.model.User
import com.studygroup.finder.data.repository.AuthRepository
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
 * UI state for the Home dashboard.
 *
 * @property currentUser the signed-in user's profile (null while loading).
 * @property myGroups groups the current user has joined.
 * @property discoverGroups all public groups for the "Discover" section.
 * @property isLoading true while the initial data is being fetched.
 * @property errorMessage non-null when an error needs to be surfaced to the UI.
 */
data class HomeUiState(
    val currentUser: User? = null,
    val myGroups: List<StudyGroup> = emptyList(),
    val discoverGroups: List<StudyGroup> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

/**
 * ViewModel for the Home dashboard screen.
 *
 * Loads the current user profile and streams all public study groups from
 * Firestore, splitting them into "my groups" (groups the user has joined)
 * and "discover groups" (everything else).
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val studyGroupRepository: StudyGroupRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadUserProfile()
        observeGroups()
    }

    /**
     * Fetch the current user's Firestore profile and update [uiState].
     */
    private fun loadUserProfile() {
        viewModelScope.launch {
            val firebaseUser = authRepository.getCurrentUser()
            if (firebaseUser != null) {
                userRepository.getUserProfile(firebaseUser.uid)
                    .onSuccess { user ->
                        _uiState.update { it.copy(currentUser = user) }
                    }
                    .onFailure { throwable ->
                        _uiState.update {
                            it.copy(
                                errorMessage = throwable.localizedMessage
                                    ?: "Failed to load profile"
                            )
                        }
                    }
            }
        }
    }

    /**
     * Observe all groups from Firestore in real-time and split them into
     * "my groups" and "discover groups" based on the current user's ID.
     */
    private fun observeGroups() {
        viewModelScope.launch {
            studyGroupRepository.getAllGroupsFlow()
                .catch { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = throwable.localizedMessage
                                ?: "Failed to load groups"
                        )
                    }
                }
                .collect { allGroups ->
                    val userId = authRepository.getCurrentUser()?.uid.orEmpty()
                    _uiState.update {
                        it.copy(
                            myGroups = allGroups.filter { group ->
                                userId in group.members
                            },
                            discoverGroups = allGroups.filter { group ->
                                !group.isPrivate
                            },
                            isLoading = false
                        )
                    }
                }
        }
    }

    /** Clear any displayed error after the UI has shown it. */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    /** Re-trigger loading of both profile and groups. */
    fun refresh() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        loadUserProfile()
        observeGroups()
    }
}
