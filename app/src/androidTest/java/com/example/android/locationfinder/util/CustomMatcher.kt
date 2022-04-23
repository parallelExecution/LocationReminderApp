package com.example.android.locationfinder.util

import android.view.View
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.intent.Checks
import org.hamcrest.Description
import org.hamcrest.Matcher

// https://stackoverflow.com/questions/31394569/how-to-assert-inside-a-recyclerview-in-espresso
object CustomMatcher {
    fun atPosition(position: Int, itemMatcher: Matcher<View>): BoundedMatcher<View?, RecyclerView> {
        Checks.checkNotNull(itemMatcher)
        return object : BoundedMatcher<View?, RecyclerView>(RecyclerView::class.java) {
            override fun describeTo(description: Description) {
                description.appendText("has item at position $position: ")
                itemMatcher.describeTo(description)
            }

            override fun matchesSafely(view: RecyclerView): Boolean {
                val viewHolder = view.findViewHolderForAdapterPosition(position)
                    ?: // has no item on such position
                    return false
                return itemMatcher.matches(viewHolder.itemView)
            }
        }
    }
}