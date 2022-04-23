package com.example.android.locationfinder.util

import android.os.IBinder
import android.view.WindowManager
import android.view.WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
import android.view.WindowManager.LayoutParams.TYPE_TOAST
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Root
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher

// https://stackoverflow.com/a/49834662

class ToastMatcher : TypeSafeMatcher<Root?>() {

    override fun matchesSafely(item: Root?): Boolean {
        val type: Int? = item?.windowLayoutParams?.get()?.type
        if (type == WindowManager.LayoutParams.FIRST_APPLICATION_WINDOW) {
            val windowToken: IBinder = item.decorView.windowToken
            val appToken: IBinder = item.decorView.applicationWindowToken
            if (windowToken === appToken) { // means this window isn't contained by any other windows.
                return true
            }
        }
        return false
    }

    override fun describeTo(description: Description?) {
        description?.appendText("is toast")
    }
}