package com.studygroup.finder.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studygroup.finder.data.model.Notification
import com.studygroup.finder.data.repository.AuthRepository
import com.studygroup.finder.data.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI state for the Notifications screen.
 *
 * @property notifications  all notifications for the current user (newest first).
 * @property unreadCount    number of unread notifications (drives badge on Home).
 * @property isLoading      true while the initial snapshot is loading.
 * @property message        user-facing feedback after an action.
 */
data class NotificationUiState(
    val notifications: List<Notification> = emptyList(),
    val unreadCount: Int = 0,
    val isLoading: Boolean = true,
    val message: String? = null
)

/**
 * ViewModel for the Notifications screen.
 *
 * Streams the current user's notifications from Firestore in real-time
 * and exposes mark-as-read actions.
 */
@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationUiState())
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()

    init {
        loadNotifications()
    }

    // ── Core actions ────────────────────────────────

    /**
     * Start streaming notifications for the current user.
     */
    private fun loadNotifications() {
        val userId = authRepository.getCurrentUser()?.uid.orEmpty()
        if (userId.isEmpty()) {
            _uiState.update { it.copy(isLoading = false) }
            return
        }

        viewModelScope.launch {
            notificationRepository.getNotificationsForUser(userId)
                .catch { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            message = throwable.localizedMessage
                                ?: "Failed to load notifications"
                        )
                    }
                }
                .collect { notifications ->
                    _uiState.update {
                        it.copy(
                            notifications = notifications,
                            unreadCount = notifications.count { n -> !n.isRead },
                            isLoading = false
                        )
                    }
                }
        }
    }

    /**
     * Mark a single notification as read.
     */
    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            notificationRepository.markAsRead(notificationId)
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            message = throwable.localizedMessage
                                ?: "Failed to mark as read"
                        )
                    }
                }
        }
    }

    /**
     * Mark every unread notification as read.
     */
    fun markAllAsRead() {
        val unread = _uiState.value.notifications.filter { !it.isRead }
        viewModelScope.launch {
            for (notification in unread) {
                notificationRepository.markAsRead(notification.notificationId)
            }
            _uiState.update { it.copy(message = "All notifications marked as read") }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
