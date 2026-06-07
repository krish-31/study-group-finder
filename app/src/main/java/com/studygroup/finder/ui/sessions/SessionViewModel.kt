package com.studygroup.finder.ui.sessions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studygroup.finder.data.model.Notification
import com.studygroup.finder.data.model.StudySession
import com.studygroup.finder.data.repository.AuthRepository
import com.studygroup.finder.data.repository.NotificationRepository
import com.studygroup.finder.data.repository.SessionRepository
import com.studygroup.finder.data.repository.StudyGroupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * Form state for the Schedule Session screen.
 *
 * @property title           session title / topic.
 * @property dateTimeMillis  selected date+time in epoch milliseconds.
 * @property durationMinutes expected duration in minutes.
 * @property location        physical address or "Online".
 */
data class SessionFormState(
    val title: String = "",
    val dateTimeMillis: Long = System.currentTimeMillis() + 3_600_000, // default: 1 h from now
    val durationMinutes: String = "60",
    val location: String = "Online"
)

/**
 * UI state for session-related screens.
 *
 * @property sessions    list of sessions for the current group.
 * @property formState   schedule-session form values.
 * @property isLoading   true while data is being fetched or submitted.
 * @property message     user-facing success / error message.
 * @property isSessionCreated true after a session has been successfully created.
 */
data class SessionUiState(
    val sessions: List<StudySession> = emptyList(),
    val formState: SessionFormState = SessionFormState(),
    val isLoading: Boolean = false,
    val message: String? = null,
    val isSessionCreated: Boolean = false
)

/**
 * ViewModel for study-session scheduling and management.
 *
 * Handles creating sessions, streaming them in real-time, updating
 * their status, and sending notifications to group members.
 */
@HiltViewModel
class SessionViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val notificationRepository: NotificationRepository,
    private val studyGroupRepository: StudyGroupRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SessionUiState())
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

    /** Tracks the group being observed to avoid duplicate collectors. */
    private var observedGroupId: String? = null

    // ── Form field updaters ─────────────────────────

    fun onTitleChanged(title: String) {
        _uiState.update { it.copy(formState = it.formState.copy(title = title)) }
    }

    fun onDateTimeSelected(millis: Long) {
        _uiState.update { it.copy(formState = it.formState.copy(dateTimeMillis = millis)) }
    }

    fun onDurationChanged(duration: String) {
        _uiState.update { it.copy(formState = it.formState.copy(durationMinutes = duration)) }
    }

    fun onLocationChanged(location: String) {
        _uiState.update { it.copy(formState = it.formState.copy(location = location)) }
    }

    // ── Core actions ────────────────────────────────

    /**
     * Stream sessions for [groupId] in real-time.
     */
    fun loadSessionsForGroup(groupId: String) {
        if (groupId == observedGroupId) return
        observedGroupId = groupId

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            sessionRepository.getSessionsForGroup(groupId)
                .catch { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            message = throwable.localizedMessage
                                ?: "Failed to load sessions"
                        )
                    }
                }
                .collect { sessions ->
                    _uiState.update {
                        it.copy(
                            sessions = sessions.sortedBy { s -> s.dateTime },
                            isLoading = false
                        )
                    }
                }
        }
    }

    /**
     * Create a new study session and notify all group members.
     */
    fun createSession(groupId: String) {
        val form = _uiState.value.formState
        if (form.title.isBlank()) {
            _uiState.update { it.copy(message = "Session title cannot be empty") }
            return
        }

        val duration = form.durationMinutes.toIntOrNull()
        if (duration == null || duration <= 0) {
            _uiState.update { it.copy(message = "Please enter a valid duration") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val session = StudySession(
                groupId = groupId,
                title = form.title.trim(),
                dateTime = form.dateTimeMillis,
                durationMinutes = duration,
                location = form.location.trim().ifBlank { "Online" },
                status = StudySession.STATUS_UPCOMING
            )

            sessionRepository.createSession(session)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            message = "Session scheduled successfully!",
                            isSessionCreated = true,
                            formState = SessionFormState() // reset form
                        )
                    }
                    // Notify all group members
                    notifyGroupMembers(groupId, session)
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            message = throwable.localizedMessage
                                ?: "Failed to schedule session"
                        )
                    }
                }
        }
    }

    /**
     * Update a session's status to "completed".
     */
    fun markSessionComplete(sessionId: String) {
        viewModelScope.launch {
            sessionRepository.updateSessionStatus(sessionId, StudySession.STATUS_COMPLETED)
                .onSuccess {
                    _uiState.update { it.copy(message = "Session marked as completed") }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            message = throwable.localizedMessage
                                ?: "Failed to update session"
                        )
                    }
                }
        }
    }

    /**
     * Update a session's status to "active".
     */
    fun markSessionActive(sessionId: String) {
        viewModelScope.launch {
            sessionRepository.updateSessionStatus(sessionId, StudySession.STATUS_ACTIVE)
                .onSuccess {
                    _uiState.update { it.copy(message = "Session started!") }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            message = throwable.localizedMessage
                                ?: "Failed to start session"
                        )
                    }
                }
        }
    }

    /** Get the current user's UID. */
    fun getCurrentUserId(): String =
        authRepository.getCurrentUser()?.uid.orEmpty()

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun resetSessionCreated() {
        _uiState.update { it.copy(isSessionCreated = false) }
    }

    // ── Private helpers ─────────────────────────────

    /**
     * Send a "session" notification to every member of the group.
     */
    private fun notifyGroupMembers(groupId: String, session: StudySession) {
        viewModelScope.launch {
            studyGroupRepository.getGroupById(groupId)
                .onSuccess { group ->
                    val dateStr = SimpleDateFormat(
                        "EEE, dd MMM yyyy 'at' h:mm a",
                        Locale.getDefault()
                    ).format(Date(session.dateTime))

                    for (memberId in group.members) {
                        val notification = Notification(
                            userId = memberId,
                            title = "New Session: ${session.title}",
                            message = "${group.name} scheduled a session on $dateStr",
                            type = Notification.TYPE_SESSION
                        )
                        notificationRepository.createNotification(notification)
                    }
                }
        }
    }
}
