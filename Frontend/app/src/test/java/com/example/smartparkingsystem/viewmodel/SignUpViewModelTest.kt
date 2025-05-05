package com.example.smartparkingsystem.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.smartparkingsystem.data.model.SignUpResponse
import com.example.smartparkingsystem.data.repository.UserRepository
import com.example.smartparkingsystem.ui.signup.SignUpViewModel
import com.example.smartparkingsystem.utils.state.UiState
import com.example.smartparkingsystem.utils.validation.EmailValidator
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.*

@ExperimentalCoroutinesApi
class SignUpViewModelTest {

    private lateinit var viewModel: SignUpViewModel
    private lateinit var userRepository: UserRepository
    private lateinit var emailValidator: EmailValidator

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        userRepository = mockk()
        emailValidator = mockk()
        viewModel = SignUpViewModel(userRepository, emailValidator)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `Valid input should emit Success state`() = testScope.runTest {
        val response = SignUpResponse("User created successfully")
        every { emailValidator.isValid("user@example.com") } returns true
        coEvery { userRepository.signUp("User", "user@example.com", "password123") } returns Result.success(response)

        viewModel.signUp("User", "user@example.com", "password123", "password123")
        advanceUntilIdle()

        val result = viewModel.signUpState.value
        assert(result is UiState.Success && result.data == response)
    }

    @Test
    fun `Invalid email format should trigger validation error`() = testScope.runTest {
        every { emailValidator.isValid("invalid-email") } returns false

        viewModel.signUp("User", "invalid-email", "password123", "password123")
        advanceUntilIdle()

        val validation = viewModel.validationState.value
        assert(validation?.emailError == "Invalid email format!")
    }

    @Test
    fun `Mismatched passwords should trigger confirmPassword error`() = testScope.runTest {
        every { emailValidator.isValid("user@example.com") } returns true

        viewModel.signUp("User", "user@example.com", "password123", "different")
        advanceUntilIdle()

        val validation = viewModel.validationState.value
        assert(validation?.confirmPasswordError == "Passwords do not match!")
    }

    @Test
    fun `API failure should emit Error state`() = testScope.runTest {
        every { emailValidator.isValid("user@example.com") } returns true
        coEvery { userRepository.signUp(any(), any(), any()) } returns Result.failure(Exception("Registration failed"))

        viewModel.signUp("User", "user@example.com", "password123", "password123")
        advanceUntilIdle()

        val result = viewModel.signUpState.value
        assert(result is UiState.Error && result.message == "Registration failed")
    }
}
