package com.example.smartparkingsystem.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.smartparkingsystem.data.model.LocationResponse
import com.example.smartparkingsystem.data.repository.NavigationRepository
import com.example.smartparkingsystem.data.repository.ParkingManagementRepository
import com.example.smartparkingsystem.data.repository.UserRepository
import com.example.smartparkingsystem.ui.home.HomeViewModel
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
class LocationViewModelTest {

    private lateinit var viewModel: HomeViewModel
    private lateinit var navigationRepository: NavigationRepository
    private lateinit var parkingManagementRepository: ParkingManagementRepository
    private lateinit var userRepository: UserRepository

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        navigationRepository = mockk()
        parkingManagementRepository = mockk()
        userRepository = mockk()
        viewModel = HomeViewModel(navigationRepository, parkingManagementRepository, userRepository)
        mockkStatic(android.util.Log::class)
        every { android.util.Log.e(any(), any(), any()) } returns 0
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `fetchLocations success should emit Success state with locations list`() = testScope.runTest {
        // Arrange
        val mockLocations = createMockLocations()
        coEvery { navigationRepository.getAllLocations() } returns mockLocations

        // Act
        viewModel.fetchLocations()
        advanceUntilIdle()

        // Assert
        val result = viewModel.uiState.first()
        assertTrue(result is UiState.Success)
        assertEquals(mockLocations, (result as UiState.Success).data)
    }

    @Test
    fun `fetchLocations failure should emit Error state`() = testScope.runTest {
        // Arrange
        val errorMessage = "Error when loading locations"
        coEvery { navigationRepository.getAllLocations() } throws Exception(errorMessage)

        // Act
        viewModel.fetchLocations()
        advanceUntilIdle()

        // Assert
        val result = viewModel.uiState.first()
        assertTrue(result is UiState.Error)
        assertEquals(errorMessage, (result as UiState.Error).message)
    }

    @Test
    fun `fetchLocations should emit Loading state initially`() = testScope.runTest {
        // Arrange
        coEvery { navigationRepository.getAllLocations() } returns createMockLocations()

        // Act
        viewModel.fetchLocations()

        // Assert
        val result = viewModel.uiState.first()
        assertTrue(result is UiState.Loading)
    }

    @Test
    fun `fetchLocations with empty list should emit Success state with empty list`() = testScope.runTest {
        // Arrange
        coEvery { navigationRepository.getAllLocations() } returns emptyList()

        // Act
        viewModel.fetchLocations()
        advanceUntilIdle()

        // Assert
        val result = viewModel.uiState.first()
        assertTrue(result is UiState.Success)
        assertTrue((result as UiState.Success).data.isEmpty())
    }

    // Helper functions to create mock data
    private fun createMockLocation(id: Int): LocationResponse {
        return LocationResponse(
            id = id,
            latitude = 41.0082,
            longitude = 28.9784,
            name = "Test Location $id"
        )
    }

    private fun createMockLocations(): List<LocationResponse> {
        return listOf(
            createMockLocation(1),
            createMockLocation(2),
            createMockLocation(3)
        )
    }
}