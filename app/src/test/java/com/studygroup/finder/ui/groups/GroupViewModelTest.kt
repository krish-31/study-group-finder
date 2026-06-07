package com.studygroup.finder.ui.groups

import com.studygroup.finder.core.utils.NetworkUtils
import com.studygroup.finder.data.model.StudyGroup
import com.studygroup.finder.data.repository.AuthRepository
import com.studygroup.finder.data.repository.JoinRequestRepository
import com.studygroup.finder.data.repository.NotificationRepository
import com.studygroup.finder.data.repository.ReviewRepository
import com.studygroup.finder.data.repository.SessionRepository
import com.studygroup.finder.data.repository.StudyGroupRepository
import com.studygroup.finder.data.repository.UserRepository
import com.studygroup.finder.ui.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GroupViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepository: AuthRepository = mockk(relaxed = true)
    private val studyGroupRepository: StudyGroupRepository = mockk(relaxed = true)
    private val joinRequestRepository: JoinRequestRepository = mockk(relaxed = true)
    private val userRepository: UserRepository = mockk(relaxed = true)
    private val sessionRepository: SessionRepository = mockk(relaxed = true)
    private val reviewRepository: ReviewRepository = mockk(relaxed = true)
    private val notificationRepository: NotificationRepository = mockk(relaxed = true)
    private val networkUtils: NetworkUtils = mockk()

    private lateinit var viewModel: GroupViewModel

    @Before
    fun setUp() {
        every { networkUtils.isNetworkAvailable() } returns true
        viewModel = GroupViewModel(
            authRepository,
            studyGroupRepository,
            joinRequestRepository,
            userRepository,
            sessionRepository,
            reviewRepository,
            notificationRepository,
            networkUtils
        )
    }

    @Test
    fun createGroup_emptyName_setsErrorMessage() = runTest {
        // When
        viewModel.createGroup(
            name = "",
            subject = "Math",
            description = "Description",
            maxMembers = 5,
            isPrivate = false,
            onSuccess = {}
        )

        // Then
        assertEquals("Group name and subject cannot be empty", viewModel.message.value)
    }

    @Test
    fun createGroup_success_callsOnSuccess() = runTest {
        // Given
        val firebaseUser = mockk<com.google.firebase.auth.FirebaseUser>()
        every { firebaseUser.uid } returns "creator_uid"
        every { authRepository.getCurrentUser() } returns firebaseUser
        every { networkUtils.isNetworkAvailable() } returns true

        coEvery {
            studyGroupRepository.createGroup(any())
        } returns Result.success("new_group_id")

        var successGroupId: String? = null

        // When
        viewModel.createGroup(
            name = "Calculus Study Group",
            subject = "Math",
            description = "Let's study Calculus",
            maxMembers = 5,
            isPrivate = false,
            onSuccess = { successGroupId = it }
        )

        // Then
        assertEquals("new_group_id", successGroupId)
        assertEquals("Group created successfully!", viewModel.message.value)
    }
}
