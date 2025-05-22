package com.example.smartparkingsystem.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.smartparkingsystem.data.model.Building
import com.example.smartparkingsystem.data.model.ParkingLayoutResponse
import com.example.smartparkingsystem.data.model.Road
import com.example.smartparkingsystem.data.model.Spot
import com.example.smartparkingsystem.data.repository.ParkingManagementRepository
import com.example.smartparkingsystem.ui.parkinglayout.ParkingLayoutViewModel
import com.example.smartparkingsystem.utils.state.UiState
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class ParkingLayoutViewModelTest {

    private lateinit var viewModel: ParkingLayoutViewModel
    private lateinit var repository: ParkingManagementRepository

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
        viewModel = ParkingLayoutViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `getParkingLayout success should emit Success state with layout data`() = testScope.runTest {
        // Arrange
        val parkingId = 1
        val mockLayout = createMockParkingLayout()
        coEvery { repository.getParkingLayout(parkingId) } returns mockLayout

        // Act
        viewModel.getParkingLayout(parkingId)
        advanceUntilIdle()

        // Assert
        val result = viewModel.layout.value
        assertTrue(result is UiState.Success)
        assertEquals(mockLayout, (result as UiState.Success).data)
    }

    @Test
    fun `getParkingLayout failure should emit Error state`() = testScope.runTest {
        // Arrange
        val parkingId = 1
        val errorMessage = "Network error"
        coEvery { repository.getParkingLayout(parkingId) } throws Exception(errorMessage)

        // Act
        viewModel.getParkingLayout(parkingId)
        advanceUntilIdle()

        // Assert
        val result = viewModel.layout.value
        assertTrue(result is UiState.Error)
        assertEquals(errorMessage, (result as UiState.Error).message)
    }

    @Test
    fun `getParkingLayout should emit Loading state initially`() = testScope.runTest {
        // Arrange
        val parkingId = 1
        coEvery { repository.getParkingLayout(parkingId) } returns createMockParkingLayout()

        // Act
        viewModel.getParkingLayout(parkingId)

        // Assert
        val result = viewModel.layout.value
        assertTrue(result is UiState.Loading)
    }

    @Test
    fun `getParkingLayout with invalid parkingId should emit Error state`() = testScope.runTest {
        // Arrange
        val invalidParkingId = -1
        coEvery { repository.getParkingLayout(invalidParkingId) } throws Exception("Invalid parking ID")

        // Act
        viewModel.getParkingLayout(invalidParkingId)
        advanceUntilIdle()

        // Assert
        val result = viewModel.layout.value
        assertTrue(result is UiState.Error)
        assertEquals("Invalid parking ID", (result as UiState.Error).message)
    }

    @Test
    fun `getParkingLayout with empty layout should handle empty data correctly`() = testScope.runTest {
        // Arrange
        val parkingId = 1
        val emptyLayout = ParkingLayoutResponse(
            capacity = 0,
            columns = 0,
            parkingId = 1,
            parkingName = "Empty Parking",
            rows = 0,
            parkingSpots = emptyList(),
            roads = emptyList(),
            buildings = emptyList()
        )
        coEvery { repository.getParkingLayout(parkingId) } returns emptyLayout

        // Act
        viewModel.getParkingLayout(parkingId)
        advanceUntilIdle()

        // Assert
        val result = viewModel.layout.value
        assertTrue(result is UiState.Success)
        assertEquals(emptyLayout, (result as UiState.Success).data)
    }

    @Test
    fun `getParkingLayout should handle repository timeout`() = testScope.runTest {
        // Arrange
        val parkingId = 1
        coEvery { repository.getParkingLayout(parkingId) } throws Exception("Timeout")

        // Act
        viewModel.getParkingLayout(parkingId)
        advanceUntilIdle()

        // Assert
        val result = viewModel.layout.value
        assertTrue(result is UiState.Error)
        assertEquals("Timeout", (result as UiState.Error).message)
    }

    // Helper function to create mock parking layout
    private fun createMockParkingLayout(): ParkingLayoutResponse {
        return ParkingLayoutResponse(
            capacity = 10,
            columns = 3,
            parkingId = 1,
            parkingName = "Test Parking",
            rows = 3,
            parkingSpots = listOf(
                Spot(
                    id = 1,
                    row = 0,
                    column = 0,
                    occupied = false,
                    spotIdentifier = "A1",
                    sensorId = "SENSOR_001"
                ),
                Spot(
                    id = 2,
                    row = 0,
                    column = 1,
                    occupied = true,
                    spotIdentifier = "A2",
                    sensorId = "SENSOR_002"
                )
            ),
            roads = listOf(
                Road(
                    id = 1,
                    roadRow = 1,
                    roadColumn = 1,
                    roadIdentifier = "Main"
                )
            ),
            buildings = listOf(
                Building(
                    id = 1,
                    buildingRow = 2,
                    buildingColumn = 2
                )
            )
        )
    }
}