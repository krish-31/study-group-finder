package com.studygroup.finder.ui.auth

import com.studygroup.finder.core.utils.NetworkUtils
import com.studygroup.finder.data.model.User
import com.studygroup.finder.data.repository.AuthRepository
import com.studygroup.finder.data.repository.UserRepository
import com.studygroup.finder.ui.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepository: AuthRepository = mockk(relaxed = true)
    private val userRepository: UserRepository = mockk(relaxed = true)
    private val networkUtils: NetworkUtils = mockk()

    private lateinit var viewModel: AuthViewModel

    private val testUser = User(
        userId = "test_uid",
        email = "test@example.com",
        name = "Test User",
        bio = "Bio",
        subjects = listOf("Math"),
        profilePicUrl = "",
        joinedGroups = emptyList(),
        isAdmin = false,
        isApproved = true
    )

    @Before
    fun setUp() {
        viewModel = AuthViewModel(authRepository, userRepository, networkUtils)
    }

    @Test
    fun login_success_emitsSuccessState() = runTest {
        // Given
        every { networkUtils.isNetworkAvailable() } returns true
        coEvery { authRepository.loginUser("test@example.com", "password") } returns Result.success(testUser)

        // When
        viewModel.login("test@example.com", "password")

        // Then
        assertTrue(viewModel.loginState.value is AuthState.Success)
        assertEquals(testUser, (viewModel.loginState.value as AuthState.Success).user)
    }

    @Test
    fun login_failure_emitsErrorState() = runTest {
        // Given
        every { networkUtils.isNetworkAvailable() } returns true
        val exception = Exception("Invalid credentials")
        coEvery { authRepository.loginUser("test@example.com", "wrong_password") } returns Result.failure(exception)

        // When
        viewModel.login("test@example.com", "wrong_password")

        // Then
        assertTrue(viewModel.loginState.value is AuthState.Error)
        assertEquals("Invalid credentials", (viewModel.loginState.value as AuthState.Error).message)
    }

    @Test
    fun login_noNetwork_emitsErrorState() = runTest {
        // Given
        every { networkUtils.isNetworkAvailable() } returns false

        // When
        viewModel.login("test@example.com", "password")

        // Then
        assertTrue(viewModel.loginState.value is AuthState.Error)
        assertTrue((viewModel.loginState.value as AuthState.Error).message.contains("No internet connection"))
    }
}
