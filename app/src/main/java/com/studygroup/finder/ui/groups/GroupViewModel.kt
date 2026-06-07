package com.studygroup.finder.ui.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studygroup.finder.data.model.JoinRequest
import com.studygroup.finder.data.model.Review
import com.studygroup.finder.data.model.StudyGroup
import com.studygroup.finder.data.model.StudySession
import com.studygroup.finder.data.model.User
import com.studygroup.finder.data.model.Notification
import com.studygroup.finder.data.repository.AuthRepository
import com.studygroup.finder.data.repository.JoinRequestRepository
import com.studygroup.finder.data.repository.NotificationRepository
import com.studygroup.finder.data.repository.ReviewRepository
import com.studygroup.finder.data.repository.SessionRepository
import com.studygroup.finder.data.repository.StudyGroupRepository
import com.studygroup.finder.data.repository.UserRepository
import com.studygroup.finder.core.utils.ErrorHandler
import com.studygroup.finder.core.utils.NetworkUtils
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
    private val reviewRepository: ReviewRepository,
    private val notificationRepository: NotificationRepository,
    private val networkUtils: NetworkUtils
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

    private val _myRequestStatus = MutableStateFlow<String?>(null)
    val myRequestStatus: StateFlow<String?> = _myRequestStatus.asStateFlow()

    private val _pendingRequests = MutableStateFlow<List<JoinRequest>>(emptyList())
    val pendingRequests: StateFlow<List<JoinRequest>> = _pendingRequests.asStateFlow()

    private val _requestSenders = MutableStateFlow<Map<String, User>>(emptyMap())
    val requestSenders: StateFlow<Map<String, User>> = _requestSenders.asStateFlow()

    private var requestStatusJob: kotlinx.coroutines.Job? = null

    init {
        loadCurrentUser()
    }

    private fun loadCurrentUser() {
        val currentUserId = authRepository.getCurrentUser()?.uid.orEmpty()
        if (currentUserId.isNotEmpty()) {
            viewModelScope.launch {
                if (!networkUtils.isNetworkAvailable()) return@launch
                try {
                    userRepository.getUserProfile(currentUserId)
                        .onSuccess { user ->
                            _currentUserProfile.value = user
                            loadUserGroups(user.userId)
                        }
                } catch (e: Exception) {
                    // Ignore background load failures
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
            if (!networkUtils.isNetworkAvailable()) {
                _message.value = "No internet connection. Please check your network settings and try again."
                _isLoading.value = false
                return@launch
            }

            try {
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
                        _message.value = ErrorHandler.getErrorMessage(throwable, networkUtils)
                    }
            } catch (e: Exception) {
                _message.value = ErrorHandler.getErrorMessage(e, networkUtils)
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
            if (!networkUtils.isNetworkAvailable()) {
                _message.value = "No internet connection. Please check your network settings and try again."
                _isLoading.value = false
                return@launch
            }

            try {
                studyGroupRepository.getAllGroupsFlow()
                    .catch { throwable ->
                        _message.value = ErrorHandler.getErrorMessage(throwable, networkUtils)
                        _isLoading.value = false
                    }
                    .collect { allGroups ->
                        val userJoined = allGroups.filter { group ->
                            userId in group.members || group.createdBy == userId
                        }
                        _userGroups.value = userJoined
                        _isLoading.value = false
                    }
            } catch (e: Exception) {
                _message.value = ErrorHandler.getErrorMessage(e, networkUtils)
                _isLoading.value = false
            }
        }
    }

    fun loadGroupDetail(groupId: String) {
        val currentUserId = authRepository.getCurrentUser()?.uid.orEmpty()
        requestStatusJob?.cancel()
        if (currentUserId.isNotEmpty()) {
            requestStatusJob = viewModelScope.launch {
                if (!networkUtils.isNetworkAvailable()) return@launch
                try {
                    joinRequestRepository.getRequestsByUser(currentUserId)
                        .catch { }
                        .collect { requests ->
                            val req = requests.find { it.groupId == groupId }
                            _myRequestStatus.value = req?.status
                        }
                } catch (e: Exception) {
                    // Fail silently for background request status stream
                }
            }
        }

        viewModelScope.launch {
            _isLoading.value = true
            if (!networkUtils.isNetworkAvailable()) {
                _message.value = "No internet connection. Please check your network settings and try again."
                _isLoading.value = false
                return@launch
            }

            try {
                studyGroupRepository.getGroupById(groupId)
                    .onSuccess { group ->
                        _groupDetail.value = group
                        // Load associated members, sessions, and reviews
                        loadAssociatedDetails(group)
                    }
                    .onFailure { throwable ->
                        _message.value = ErrorHandler.getErrorMessage(throwable, networkUtils)
                        _isLoading.value = false
                    }
            } catch (e: Exception) {
                _message.value = ErrorHandler.getErrorMessage(e, networkUtils)
                _isLoading.value = false
            }
        }
    }

    private suspend fun loadAssociatedDetails(group: StudyGroup) {
        if (!networkUtils.isNetworkAvailable()) return

        // 1. Fetch member profiles
        val memberProfiles = mutableListOf<User>()
        try {
            for (memberId in group.members) {
                userRepository.getUserProfile(memberId)
                    .onSuccess { memberProfiles.add(it) }
            }
            _groupMembers.value = memberProfiles
        } catch (e: Exception) {
            // Silence member load errors
        }

        // 2. Observe sessions for this group
        viewModelScope.launch {
            try {
                sessionRepository.getSessionsForGroup(group.groupId)
                    .catch { throwable ->
                        _message.value = ErrorHandler.getErrorMessage(throwable, networkUtils)
                    }
                    .collect { sessions ->
                        _groupSessions.value = sessions.sortedBy { it.dateTime }
                    }
            } catch (e: Exception) {
                _message.value = ErrorHandler.getErrorMessage(e, networkUtils)
            }
        }

        // 3. Observe reviews for this group
        viewModelScope.launch {
            try {
                reviewRepository.getReviewsForGroup(group.groupId)
                    .catch { throwable ->
                        _message.value = ErrorHandler.getErrorMessage(throwable, networkUtils)
                    }
                    .collect { reviews ->
                        _groupReviews.value = reviews.sortedByDescending { it.createdAt }
                    }
            } catch (e: Exception) {
                _message.value = ErrorHandler.getErrorMessage(e, networkUtils)
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
            if (!networkUtils.isNetworkAvailable()) {
                _message.value = "No internet connection. Please check your network settings and try again."
                _isLoading.value = false
                return@launch
            }

            try {
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
                            _message.value = ErrorHandler.getErrorMessage(throwable, networkUtils)
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
                            _message.value = ErrorHandler.getErrorMessage(throwable, networkUtils)
                        }
                }
            } catch (e: Exception) {
                _message.value = ErrorHandler.getErrorMessage(e, networkUtils)
            }
            _isLoading.value = false
        }
    }

    fun observePendingRequests(groupId: String) {
        viewModelScope.launch {
            if (!networkUtils.isNetworkAvailable()) {
                _message.value = "No internet connection. Please check your network settings and try again."
                return@launch
            }

            try {
                joinRequestRepository.getPendingRequestsForGroup(groupId)
                    .catch { throwable ->
                        _message.value = ErrorHandler.getErrorMessage(throwable, networkUtils)
                    }
                    .collect { requests ->
                        _pendingRequests.value = requests
                        val currentMap = _requestSenders.value.toMutableMap()
                        requests.forEach { request ->
                            if (!currentMap.containsKey(request.senderId)) {
                                userRepository.getUserProfile(request.senderId)
                                    .onSuccess { user ->
                                        currentMap[request.senderId] = user
                                        _requestSenders.value = currentMap.toMap()
                                    }
                            }
                        }
                    }
            } catch (e: Exception) {
                _message.value = ErrorHandler.getErrorMessage(e, networkUtils)
            }
        }
    }

    fun acceptRequest(requestId: String, groupId: String, userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            if (!networkUtils.isNetworkAvailable()) {
                _message.value = "No internet connection. Please check your network settings and try again."
                _isLoading.value = false
                return@launch
            }

            try {
                joinRequestRepository.respondToRequest(requestId, "accepted")
                    .onSuccess {
                        studyGroupRepository.addMemberToGroup(groupId, userId)
                            .onSuccess {
                                // Update the requester's user profile with the new group
                                userRepository.getUserProfile(userId)
                                    .onSuccess { userProfile ->
                                        if (groupId !in userProfile.joinedGroups) {
                                            userRepository.updateUserProfile(
                                                userProfile.copy(joinedGroups = userProfile.joinedGroups + groupId)
                                            )
                                        }
                                    }

                                // Fetch the group name to display in the notification message
                                val groupName = _groupDetail.value?.name ?: "Study Group"
                                val notif = Notification(
                                    userId = userId,
                                    title = "Join Request Accepted",
                                    message = "Your request to join $groupName was accepted!",
                                    type = Notification.TYPE_JOIN_REQUEST,
                                    groupId = groupId,
                                    isRead = false,
                                    createdAt = System.currentTimeMillis()
                                )
                                notificationRepository.createNotification(notif)
                                
                                _message.value = "Request accepted successfully!"
                                // Reload details to get updated members
                                loadGroupDetail(groupId)
                            }
                            .onFailure { throwable ->
                                _message.value = ErrorHandler.getErrorMessage(throwable, networkUtils)
                            }
                    }
                    .onFailure { throwable ->
                        _message.value = ErrorHandler.getErrorMessage(throwable, networkUtils)
                    }
            } catch (e: Exception) {
                _message.value = ErrorHandler.getErrorMessage(e, networkUtils)
            }
            _isLoading.value = false
        }
    }

    fun rejectRequest(requestId: String, userId: String, groupId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            if (!networkUtils.isNetworkAvailable()) {
                _message.value = "No internet connection. Please check your network settings and try again."
                _isLoading.value = false
                return@launch
            }

            try {
                joinRequestRepository.respondToRequest(requestId, "rejected")
                    .onSuccess {
                        val groupName = _groupDetail.value?.name ?: "Study Group"
                        val notif = Notification(
                            userId = userId,
                            title = "Join Request Declined",
                            message = "Your request to join $groupName was declined.",
                            type = Notification.TYPE_JOIN_REQUEST,
                            groupId = groupId,
                            isRead = false,
                            createdAt = System.currentTimeMillis()
                        )
                        notificationRepository.createNotification(notif)

                        _message.value = "Request declined successfully"
                    }
                    .onFailure { throwable ->
                        _message.value = ErrorHandler.getErrorMessage(throwable, networkUtils)
                    }
            } catch (e: Exception) {
                _message.value = ErrorHandler.getErrorMessage(e, networkUtils)
            }
            _isLoading.value = false
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}
