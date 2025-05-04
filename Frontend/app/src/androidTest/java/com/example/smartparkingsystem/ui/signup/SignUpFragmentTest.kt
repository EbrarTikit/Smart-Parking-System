package com.example.smartparkingsystem.ui.signup

import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
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
class SignUpFragmentTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun testSignUpWithValidInput_showsLoadingIndicator() {
        // Launch fragment
        launchFragmentInHiltContainer<SignUpFragment>(themeResId = R.style.Theme_SmartParkingSystem)

        onView(withId(R.id.usernameEditText)).perform(replaceText("test"), closeSoftKeyboard())
        onView(withId(R.id.emailEditText)).perform(replaceText("t@t.com"), closeSoftKeyboard())
        onView(withId(R.id.passwordEditText)).perform(replaceText("pass123"), closeSoftKeyboard())
        onView(withId(R.id.confirmPasswordEditText)).perform(
            replaceText("pass123"),
            closeSoftKeyboard()
        )

        onView(withId(R.id.signUpButton)).perform(click())

        try {
            Thread.sleep(300)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        onView(withId(R.id.progressBar)).check(matches(isDisplayed()))
    }

    @Test
    fun testSuccessfulSignUp_navigatesToSignIn() {
        val mockNavController = mockk<NavController>(relaxed = true)

        launchFragmentInHiltContainer<SignUpFragment>(themeResId = R.style.Theme_SmartParkingSystem) {
            Navigation.setViewNavController(requireView(), mockNavController)
        }

        onView(withId(R.id.usernameEditText)).perform(typeText("test"), closeSoftKeyboard())
        onView(withId(R.id.emailEditText)).perform(
            typeText("t@t.com"),
            closeSoftKeyboard()
        )
        onView(withId(R.id.passwordEditText)).perform(typeText("pass123"), closeSoftKeyboard())
        onView(withId(R.id.confirmPasswordEditText)).perform(
            typeText("pass123"),
            closeSoftKeyboard()
        )

        onView(withId(R.id.signUpButton)).perform(click())

        Thread.sleep(2000)

        verify {
            mockNavController.navigate(R.id.action_signUpFragment_to_signInFragment)
        }
    }

}