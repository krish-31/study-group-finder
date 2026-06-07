package com.studygroup.finder.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studygroup.finder.data.model.Notification
import com.studygroup.finder.data.repository.AuthRepository
import com.studygroup.finder.data.repository.NotificationRepository
import com.studygroup.finder.core.utils.ErrorHandler
import com.studygroup.finder.core.utils.NetworkUtils
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
    private val authRepository: AuthRepository,
    private val networkUtils: NetworkUtils
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
    fun loadNotifications() {
        val userId = authRepository.getCurrentUser()?.uid.orEmpty()
        if (userId.isEmpty()) {
            _uiState.update { it.copy(isLoading = false) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = null) }
            if (!networkUtils.isNetworkAvailable()) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        message = "No internet connection. Please check your network settings and try again."
                    )
                }
                return@launch
            }

            try {
                notificationRepository.getNotificationsForUser(userId)
                    .catch { throwable ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                message = ErrorHandler.getErrorMessage(throwable, networkUtils)
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
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        message = ErrorHandler.getErrorMessage(e, networkUtils)
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
            if (!networkUtils.isNetworkAvailable()) {
                _uiState.update { it.copy(message = "No internet connection. Failed to update notification.") }
                return@launch
            }

            try {
                notificationRepository.markAsRead(notificationId)
                    .onFailure { throwable ->
                        _uiState.update {
                            it.copy(
                                message = ErrorHandler.getErrorMessage(throwable, networkUtils)
                            )
                        }
                    }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        message = ErrorHandler.getErrorMessage(e, networkUtils)
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
            if (!networkUtils.isNetworkAvailable()) {
                _uiState.update { it.copy(message = "No internet connection. Failed to update notifications.") }
                return@launch
            }

            try {
                for (notification in unread) {
                    notificationRepository.markAsRead(notification.notificationId)
                }
                _uiState.update { it.copy(message = "All notifications marked as read") }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        message = ErrorHandler.getErrorMessage(e, networkUtils)
                    )
                }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
