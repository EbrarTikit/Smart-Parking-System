package com.example.smartparkingsystem.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.smartparkingsystem.data.model.FavoriteParking
import com.example.smartparkingsystem.data.model.FavoriteResponse
import com.example.smartparkingsystem.data.repository.UserRepository
import com.example.smartparkingsystem.ui.favorites.FavoritesViewModel
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
class FavoritesViewModelTest {

    private lateinit var viewModel: FavoritesViewModel
    private lateinit var repository: UserRepository

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
        viewModel = FavoritesViewModel(repository)
        mockkStatic(android.util.Log::class)
        every { android.util.Log.e(any(), any(), any()) } returns 0
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadFavorites success should emit Success state with favorites list`() = testScope.runTest {
        // Arrange
        val userId = 1
        val mockFavorites = createMockFavorites()
        coEvery { repository.getFavorites(userId) } returns Result.success(mockFavorites)

        // Act
        viewModel.loadFavorites(userId)
        advanceUntilIdle()

        // Assert
        val result = viewModel.favoritesState.first()
        assertTrue(result is UiState.Success)
        assertEquals(mockFavorites, (result as UiState.Success).data)
    }

    @Test
    fun `loadFavorites with empty list should emit Success state with empty list`() = testScope.runTest {
        // Arrange
        val userId = 1
        coEvery { repository.getFavorites(userId) } returns Result.success(emptyList())

        // Act
        viewModel.loadFavorites(userId)
        advanceUntilIdle()

        // Assert
        val result = viewModel.favoritesState.first()
        assertTrue(result is UiState.Success)
        assertTrue((result as UiState.Success).data.isEmpty())
    }

    @Test
    fun `loadFavorites failure should emit Error state`() = testScope.runTest {
        // Arrange
        val userId = 1
        val errorMessage = "Failed to load favorites"
        coEvery { repository.getFavorites(userId) } returns Result.failure(Exception(errorMessage))

        // Act
        viewModel.loadFavorites(userId)
        advanceUntilIdle()

        // Assert
        val result = viewModel.favoritesState.first()
        assertTrue(result is UiState.Error)
        assertEquals(errorMessage, (result as UiState.Error).message)
    }

    @Test
    fun `removeFavorite success should update favorites list`() = testScope.runTest {
        // Arrange
        val userId = 1
        val parkingId = 1
        val mockFavorites = createMockFavorites()
        coEvery { repository.getFavorites(userId) } returns Result.success(mockFavorites)
        coEvery { repository.removeFavorite(userId, parkingId) } returns Result.success(Unit)

        // Act
        viewModel.loadFavorites(userId)
        advanceUntilIdle()
        viewModel.removeFavorite(userId, parkingId)
        advanceUntilIdle()

        // Assert
        val result = viewModel.favoritesState.first()
        assertTrue(result is UiState.Success)
        val updatedList = (result as UiState.Success).data
        assertTrue(updatedList.none { it.id == parkingId })
    }

    @Test
    fun `removeFavorite failure should reload favorites`() = testScope.runTest {
        // Arrange
        val userId = 1
        val parkingId = 1
        val mockFavorites = createMockFavorites()
        coEvery { repository.getFavorites(userId) } returns Result.success(mockFavorites)
        coEvery { repository.removeFavorite(userId, parkingId) } returns Result.failure(
            Exception("Failed to remove favorite")
        )

        // Act
        viewModel.loadFavorites(userId)
        advanceUntilIdle()
        viewModel.removeFavorite(userId, parkingId)
        advanceUntilIdle()

        // Assert
        val result = viewModel.favoritesState.first()
        assertTrue(result is UiState.Success)
        val currentList = (result as UiState.Success).data
        assertTrue(currentList.containsAll(mockFavorites))
    }

    @Test
    fun `loadFavorites should emit Loading state initially`() = testScope.runTest {
        // Arrange
        val userId = 1
        coEvery { repository.getFavorites(userId) } returns Result.success(createMockFavorites())

        // Act
        viewModel.loadFavorites(userId)

        // Assert
        val result = viewModel.favoritesState.first()
        assertTrue(result is UiState.Loading)
    }

    // Helper function to create mock favorites
    private fun createMockFavorites(): List<FavoriteParking> {
        return listOf(
            FavoriteParking(
                id = 1,
                name = "Test Parking 1",
                location = "Test Location 1",
                imageUrl = "https://example.com/image1.jpg",
                rate = 10.0
            ),
            FavoriteParking(
                id = 2,
                name = "Test Parking 2",
                location = "Test Location 2",
                imageUrl = "https://example.com/image2.jpg",
                rate = 15.0
            )
        )
    }
}