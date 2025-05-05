package com.example.smartparkingsystem.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.smartparkingsystem.data.model.SignInResponse
import com.example.smartparkingsystem.data.repository.UserRepository
import com.example.smartparkingsystem.ui.signin.SignInViewModel
import com.example.smartparkingsystem.utils.SessionManager
import com.example.smartparkingsystem.utils.state.UiState
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.*

@ExperimentalCoroutinesApi
class SignInViewModelTest {

    private lateinit var viewModel: SignInViewModel
    private lateinit var userRepository: UserRepository
    private lateinit var sessionManager: SessionManager

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    // LiveData'ların test ortamında senkron çalışmasını sağlar
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setup() {
        // Testlerde main thread olarak kullanılacak dispatcher ayarlıyoruz
        Dispatchers.setMain(testDispatcher)

        // Mock nesnelerimiz
        userRepository = mockk()
        // relaxed: her fonksiyona varsayılan yanit döner
        sessionManager = mockk(relaxed = true)
        viewModel = SignInViewModel(userRepository, sessionManager)
    }

    //Test bitince Dispatchers.Main'i sıfırlar
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `valid credentials should emit success state`() = testScope.runTest {
        // Test için mock API yanıtı
        val mockResponse = SignInResponse(id = 1, token = "abc123")
        coEvery { userRepository.signIn("test@example.com", "password123") } returns Result.success(mockResponse)

        viewModel.signIn("test@example.com", "password123")

        // Coroutine'in tamamlanmasını bekler
        advanceUntilIdle()

        val result = viewModel.signInState.value
        assert(result is UiState.Success && result.data == mockResponse)

        verify { sessionManager.saveUserSession(1, "abc123") }
    }

    @Test
    fun `invalid credentials should emit error state`() = testScope.runTest {
        // Hatalı API çağrısı için sahte hata
        coEvery { userRepository.signIn(any(), any()) } returns Result.failure(Exception("Unauthorized"))

        viewModel.signIn("wrong@example.com", "wrong")
        advanceUntilIdle()

        val result = viewModel.signInState.value
        assert(result is UiState.Error && result.message == "Unauthorized")
    }
}
