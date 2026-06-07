package com.studygroup.finder.ui.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studygroup.finder.data.model.JoinRequest
import com.studygroup.finder.data.model.Review
import com.studygroup.finder.data.model.StudyGroup
import com.studygroup.finder.data.model.StudySession
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
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * ViewModel for managing study groups (listing, detail, and creation flows).
 */
@HiltViewModel
class GroupViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val studyGroupRepository: StudyGroupRepository,
    private val joinRequestRepository: JoinRequestRepository,
    private val userRepository: UserRepository,
    private val sessionRepository: SessionRepository,
    private val reviewRepository: ReviewRepository
) : ViewModel() {

    private val _groupDetail = MutableStateFlow<StudyGroup?>(null)
    val groupDetail: StateFlow<StudyGroup?> = _groupDetail.asStateFlow()

    private val _userGroups = MutableStateFlow<List<StudyGroup>>(emptyList())
    val userGroups: StateFlow<List<StudyGroup>> = _userGroups.asStateFlow()

    private val _isLoading = MutableStateFlow<Boolean>(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    // Additional flows for the GroupDetail tabs
    private val _groupMembers = MutableStateFlow<List<User>>(emptyList())
    val groupMembers: StateFlow<List<User>> = _groupMembers.asStateFlow()

    private val _groupSessions = MutableStateFlow<List<StudySession>>(emptyList())
    val groupSessions: StateFlow<List<StudySession>> = _groupSessions.asStateFlow()

    private val _groupReviews = MutableStateFlow<List<Review>>(emptyList())
    val groupReviews: StateFlow<List<Review>> = _groupReviews.asStateFlow()

    // Store current user profile locally
    private val _currentUserProfile = MutableStateFlow<User?>(null)
    val currentUserProfile: StateFlow<User?> = _currentUserProfile.asStateFlow()

    init {
        loadCurrentUser()
    }

    private fun loadCurrentUser() {
        val currentUserId = authRepository.getCurrentUser()?.uid.orEmpty()
        if (currentUserId.isNotEmpty()) {
            viewModelScope.launch {
                userRepository.getUserProfile(currentUserId)
                    .onSuccess { user ->
                        _currentUserProfile.value = user
                        loadUserGroups(user.userId)
                    }
                    .onFailure {
                        // Silence profile load errors or handle them gracefully
                    }
            }
        }
    }

    /**
     * Create a new Study Group.
     */
    fun createGroup(
        name: String,
        subject: String,
        description: String,
        maxMembers: Int,
        isPrivate: Boolean,
        onSuccess: (String) -> Unit
    ) {
        if (name.isBlank() || subject.isBlank()) {
            _message.value = "Group name and subject cannot be empty"
            return
        }

        val currentUserId = authRepository.getCurrentUser()?.uid.orEmpty()
        if (currentUserId.isEmpty()) {
            _message.value = "User not logged in"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            val group = StudyGroup(
                name = name,
                subject = subject,
                description = description,
                createdBy = currentUserId,
                members = listOf(currentUserId),
                maxMembers = maxMembers,
                isPrivate = isPrivate,
                createdAt = System.currentTimeMillis()
            )

            studyGroupRepository.createGroup(group)
                .onSuccess { groupId ->
                    // Automatically add the created group to user's joinedGroups
                    _currentUserProfile.value?.let { user ->
                        val updatedUser = user.copy(joinedGroups = user.joinedGroups + groupId)
                        userRepository.updateUserProfile(updatedUser)
                        _currentUserProfile.value = updatedUser
                    }
                    _message.value = "Group created successfully!"
                    onSuccess(groupId)
                }
                .onFailure { throwable ->
                    _message.value = throwable.localizedMessage ?: "Failed to create group"
                }
            _isLoading.value = false
        }
    }

    /**
     * Load all groups the user has joined.
     */
    fun loadUserGroups(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            studyGroupRepository.getAllGroupsFlow()
                .catch { throwable ->
                    _message.value = throwable.localizedMessage ?: "Failed to load groups"
                    _isLoading.value = false
                }
                .collect { allGroups ->
                    val userJoined = allGroups.filter { group ->
                        userId in group.members || group.createdBy == userId
                    }
                    _userGroups.value = userJoined
                    _isLoading.value = false
                }
        }
    }

    /**
     * Load full detail for a group including its members, sessions, and reviews.
     */
    fun loadGroupDetail(groupId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            studyGroupRepository.getGroupById(groupId)
                .onSuccess { group ->
                    _groupDetail.value = group
                    // Load associated members, sessions, and reviews
                    loadAssociatedDetails(group)
                }
                .onFailure { throwable ->
                    _message.value = throwable.localizedMessage ?: "Failed to load group detail"
                    _isLoading.value = false
                }
        }
    }

    private suspend fun loadAssociatedDetails(group: StudyGroup) {
        // 1. Fetch member profiles
        val memberProfiles = mutableListOf<User>()
        for (memberId in group.members) {
            userRepository.getUserProfile(memberId)
                .onSuccess { memberProfiles.add(it) }
        }
        _groupMembers.value = memberProfiles

        // 2. Observe sessions for this group
        viewModelScope.launch {
            sessionRepository.getSessionsForGroup(group.groupId)
                .catch { /* handle session error flow */ }
                .collect { sessions ->
                    _groupSessions.value = sessions.sortedBy { it.dateTime }
                }
        }

        // 3. Observe reviews for this group
        viewModelScope.launch {
            reviewRepository.getReviewsForGroup(group.groupId)
                .catch { /* handle reviews error flow */ }
                .collect { reviews ->
                    _groupReviews.value = reviews.sortedByDescending { it.createdAt }
                }
        }

        _isLoading.value = false
    }

    /**
     * Send a Join Request to a private group, or join a public group directly.
     */
    fun sendJoinRequest(groupId: String) {
        val currentUser = _currentUserProfile.value
        if (currentUser == null) {
            _message.value = "User profile not loaded"
            return
        }

        val group = _groupDetail.value
        if (group == null) {
            _message.value = "Group details not loaded"
            return
        }

        if (currentUser.userId in group.members) {
            _message.value = "You are already a member of this group"
            return
        }

        if (group.members.size >= group.maxMembers) {
            _message.value = "This group is already full"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            if (group.isPrivate) {
                // Submit join request
                val request = JoinRequest(
                    requestId = UUID.randomUUID().toString(),
                    groupId = groupId,
                    senderId = currentUser.userId,
                    senderName = currentUser.name,
                    status = "pending",
                    createdAt = System.currentTimeMillis()
                )
                joinRequestRepository.sendJoinRequest(request)
                    .onSuccess {
                        _message.value = "Join request sent to admin"
                    }
                    .onFailure { throwable ->
                        _message.value = throwable.localizedMessage ?: "Failed to send join request"
                    }
            } else {
                // Join public group directly
                studyGroupRepository.addMemberToGroup(groupId, currentUser.userId)
                    .onSuccess {
                        val updatedUser = currentUser.copy(joinedGroups = currentUser.joinedGroups + groupId)
                        userRepository.updateUserProfile(updatedUser)
                        _currentUserProfile.value = updatedUser

                        // Refresh details
                        loadGroupDetail(groupId)
                        _message.value = "Joined group successfully!"
                    }
                    .onFailure { throwable ->
                        _message.value = throwable.localizedMessage ?: "Failed to join group"
                    }
            }
            _isLoading.value = false
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}
