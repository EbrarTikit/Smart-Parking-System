package com.example.smartparkingsystem.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.smartparkingsystem.data.model.NotificationPreferences
import com.example.smartparkingsystem.data.repository.UserRepository
import com.example.smartparkingsystem.ui.profile.ProfileViewModel
import com.example.smartparkingsystem.utils.state.UiState
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class ProfileViewModelTest {

    private lateinit var viewModel: ProfileViewModel
    private lateinit var userRepository: UserRepository

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        userRepository = mockk()
        viewModel = ProfileViewModel(userRepository)
        mockkStatic(android.util.Log::class)
        every { android.util.Log.e(any(), any(), any()) } returns 0
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `getNotificationPreferences success should emit Success state with preferences`() = testScope.runTest {
        // Arrange
        val userId = 1
        val mockPreferences = createMockNotificationPreferences(true)
        coEvery { userRepository.getNotificationPreferences(userId) } returns Result.success(mockPreferences)

        // Act
        viewModel.getNotificationPreferences(userId)
        advanceUntilIdle()

        // Assert
        val result = viewModel.notificationState.first()
        assertTrue(result is UiState.Success)
        assertEquals(mockPreferences, (result as UiState.Success).data)
    }

    @Test
    fun `getNotificationPreferences failure should emit Error state`() = testScope.runTest {
        // Arrange
        val userId = 1
        val errorMessage = "Failed to get notification preferences"
        coEvery { userRepository.getNotificationPreferences(userId) } returns Result.failure(Exception(errorMessage))

        // Act
        viewModel.getNotificationPreferences(userId)
        advanceUntilIdle()

        // Assert
        val result = viewModel.notificationState.first()
        assertTrue(result is UiState.Error)
        assertEquals(errorMessage, (result as UiState.Error).message)
    }

    @Test
    fun `setNotificationPreferences when value changes should update preferences`() = testScope.runTest {
        // Arrange
        val userId = 1
        val currentPreferences = createMockNotificationPreferences(false)
        val updatedPreferences = createMockNotificationPreferences(true)

        coEvery { userRepository.getNotificationPreferences(userId) } returns Result.success(currentPreferences)
        coEvery { userRepository.toggleNotificationPreferences(userId) } returns Result.success(updatedPreferences)

        // Act
        viewModel.setNotificationPreferences(userId, true)
        advanceUntilIdle()

        // Assert
        val result = viewModel.notificationState.first()
        assertTrue(result is UiState.Success)
        assertEquals(updatedPreferences, (result as UiState.Success).data)
    }

    @Test
    fun `setNotificationPreferences when value unchanged should keep current preferences`() = testScope.runTest {
        // Arrange
        val userId = 1
        val currentPreferences = createMockNotificationPreferences(true)

        coEvery { userRepository.getNotificationPreferences(userId) } returns Result.success(currentPreferences)

        // Act
        viewModel.setNotificationPreferences(userId, true)
        advanceUntilIdle()

        // Assert
        val result = viewModel.notificationState.first()
        assertTrue(result is UiState.Success)
        assertEquals(currentPreferences, (result as UiState.Success).data)
    }

    @Test
    fun `setNotificationPreferences failure should emit Error state`() = testScope.runTest {
        // Arrange
        val userId = 1
        val errorMessage = "Failed to update notification preferences"
        coEvery { userRepository.getNotificationPreferences(userId) } returns Result.failure(Exception(errorMessage))

        // Act
        viewModel.setNotificationPreferences(userId, true)
        advanceUntilIdle()

        // Assert
        val result = viewModel.notificationState.first()
        assertTrue(result is UiState.Error)
        assertEquals(errorMessage, (result as UiState.Error).message)
    }

    @Test
    fun `getNotificationPreferences should emit Loading state initially`() = testScope.runTest {
        // Arrange
        val userId = 1
        coEvery { userRepository.getNotificationPreferences(userId) } returns Result.success(createMockNotificationPreferences(true))

        // Act
        viewModel.getNotificationPreferences(userId)

        // Assert
        val result = viewModel.notificationState.first()
        assertTrue(result is UiState.Loading)
    }

    @Test
    fun `setNotificationPreferences should emit Loading state initially`() = testScope.runTest {
        // Arrange
        val userId = 1
        coEvery { userRepository.getNotificationPreferences(userId) } returns Result.success(createMockNotificationPreferences(false))
        coEvery { userRepository.toggleNotificationPreferences(userId) } returns Result.success(createMockNotificationPreferences(true))

        // Act
        viewModel.setNotificationPreferences(userId, true)

        // Assert
        val result = viewModel.notificationState.first()
        assertTrue(result is UiState.Loading)
    }

    // Helper function to create mock notification preferences
    private fun createMockNotificationPreferences(enabled: Boolean): NotificationPreferences {
        return NotificationPreferences(parkingFullNotification = enabled)
    }
}