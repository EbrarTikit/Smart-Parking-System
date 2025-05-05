package com.example.smartparkingsystem.ui.signin

import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.example.smartparkingsystem.R
import com.example.smartparkingsystem.utils.launchFragmentInHiltContainer
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@LargeTest
class SignInFragmentTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Before
    fun setUp() {
        hiltRule.inject()
    }


    @Test
    fun testSignInWithValidInput_showsLoadingIndicator() {
        // Launch fragment
        launchFragmentInHiltContainer<SignInFragment>(themeResId = R.style.Theme_SmartParkingSystem)

        onView(withId(R.id.emailEditText)).perform(
            typeText("test@example.com"),
            closeSoftKeyboard()
        )
        onView(withId(R.id.passwordEditText)).perform(typeText("password123"), closeSoftKeyboard())

        onView(withId(R.id.loginButton)).perform(click())

        Thread.sleep(300)
        onView(withId(R.id.progressBar)).check(matches(isDisplayed()))
    }

    @Test
    fun testSuccessfulSignIn_navigatesToLocationAccess() {
        val mockNavController = mockk<NavController>(relaxed = true)

        // Launch fragment with mocked NavController
        launchFragmentInHiltContainer<SignInFragment>(themeResId = R.style.Theme_SmartParkingSystem) {
            Navigation.setViewNavController(requireView(), mockNavController)
        }

        onView(withId(R.id.emailEditText)).perform(
            typeText("test@example.com"),
            closeSoftKeyboard()
        )
        onView(withId(R.id.passwordEditText)).perform(typeText("password123"), closeSoftKeyboard())

        onView(withId(R.id.loginButton)).perform(click())

        Thread.sleep(2000)

        verify {
            mockNavController.navigate(R.id.action_signInFragment_to_locationAccessFragment)
        }
    }

    @Test
    fun testCreateAccountNavigation() {
        val mockNavController = mockk<NavController>(relaxed = true)

        // Launch fragment with mocked NavController
        launchFragmentInHiltContainer<SignInFragment>(themeResId = R.style.Theme_SmartParkingSystem) {
            Navigation.setViewNavController(requireView(), mockNavController)
        }

        // Click on create account text
        onView(withId(R.id.createAccountText)).perform(click())

        // Verify navigation to sign up fragment
        verify {
            mockNavController.navigate(R.id.action_signInFragment_to_signUpFragment)
        }
    }
}