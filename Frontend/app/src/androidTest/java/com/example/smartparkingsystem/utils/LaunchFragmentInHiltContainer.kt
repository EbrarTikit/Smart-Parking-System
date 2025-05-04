@file:Suppress("UNCHECKED_CAST")

package com.example.smartparkingsystem.utils

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.annotation.StyleRes
import androidx.core.util.Preconditions
import androidx.fragment.app.Fragment
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.example.smartparkingsystem.HiltTestActivity
import com.example.smartparkingsystem.R

inline fun <reified T : Fragment> launchFragmentInHiltContainer(
    @StyleRes themeResId: Int = R.style.Theme_SmartParkingSystem,
    crossinline action: T.() -> Unit = {}
): ActivityScenario<HiltTestActivity> {
    val startActivityIntent = Intent.makeMainActivity(
        ComponentName(
            ApplicationProvider.getApplicationContext(),
            HiltTestActivity::class.java
        )
    ).putExtra("androidx.fragment.app.testing.FragmentScenario.FragmentClass", T::class.java.name)
        .putExtra("androidx.fragment.app.testing.FragmentScenario.FragmentArgs", null as Bundle?)
        .putExtra("androidx.fragment.app.testing.FragmentScenario.ThemeResId", themeResId)

    val scenario = ActivityScenario.launch<HiltTestActivity>(startActivityIntent)
    scenario.onActivity { activity ->
        val fragment = activity.supportFragmentManager.fragmentFactory.instantiate(
            Preconditions.checkNotNull(T::class.java.classLoader),
            T::class.java.name
        )
        activity.supportFragmentManager
            .beginTransaction()
            .add(android.R.id.content, fragment, "")
            .commitNow()

        (fragment as T).action()
    }
    return scenario
}