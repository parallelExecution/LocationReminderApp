package com.example.android.locationfinder.locationreminders.reminderslist

import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.core.os.bundleOf
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.core.app.ApplicationProvider
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.*
import androidx.test.espresso.action.ViewActions.click
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.firebase.FirebaseApp
import com.example.android.locationfinder.R
import com.example.android.locationfinder.data.FakeDataSource
import com.example.android.locationfinder.locationreminders.data.dto.ReminderDTO
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.AutoCloseKoinTest
import org.koin.test.get
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import com.example.android.locationfinder.util.CustomMatcher.atPosition
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.Matcher
import org.hamcrest.core.IsNot.not
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
//UI Testing
@MediumTest
class ReminderListFragmentTest : AutoCloseKoinTest() {

    private lateinit var repository: FakeDataSource
    private lateinit var appContext: Application
    private lateinit var viewModel: RemindersListViewModel

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    /**
     * As we use Koin as a Service Locator Library to develop our code, we'll also use Koin to test our code.
     * at this step we will initialize Koin related code to be able to use it in out testing.
     */
    @Before
    fun init() {
        stopKoin()//stop the original app koin
        appContext = getApplicationContext()
        FirebaseApp.initializeApp(appContext)
        val myModule = module {
            viewModel {
                RemindersListViewModel(
                    appContext,
                    get() as FakeDataSource
                )
            }

            single { FakeDataSource() }
        }
        //declare a new koin module
        startKoin {
            modules(listOf(myModule))
        }
        //Get our real repository
        repository = get()

        // Get our viewmodel
        viewModel = get()

        //clear the data to start fresh and add few reminders
        runBlocking {
            repository.deleteAllReminders()
        }
    }

    @After
    fun end() {
        autoClose()
    }

    @Test
    fun reminderDetails_DisplayedInUi() = runBlockingTest {
        // Given a reminder
        val reminder1 = ReminderDTO("title1", "description1", "location1", 1.0, 1.0)
        val reminder2 = ReminderDTO("title2", "description2", "location2", 1.0, 1.0)
        repository.saveReminder(reminder1)
        repository.saveReminder(reminder2)

        // WHEN - Details fragment launched to display task
        launchFragmentInContainer<ReminderListFragment>(bundleOf(), R.style.AppTheme)

        // THEN - Reminder details are displayed on the screen
        // make sure that the title/description are both shown and correct
        onView(withId(R.id.reminderssRecyclerView))
            .check(matches(atPosition(0, hasDescendant(withText("title1")))))
            .check(matches(atPosition(0, hasDescendant(withText("description1")))))
            .check(matches(atPosition(0, hasDescendant(withText("location1")))))

        onView(withId(R.id.reminderssRecyclerView))
            .check(matches(atPosition(1, hasDescendant(withText("title2")))))
            .check(matches(atPosition(1, hasDescendant(withText("description2")))))
            .check(matches(atPosition(1, hasDescendant(withText("location2")))))
    }

    @Test
    fun clickFAB_navigateToSaveReminderFragment() {
        // GIVEN - On the home screen
        val scenario = launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)
        val navController = mock(NavController::class.java)
        scenario.onFragment {
            Navigation.setViewNavController(it.view!!, navController)
        }

        // WHEN - Click on the "+" button
        onView(withId(R.id.addReminderFAB)).perform(click())

        // THEN - Verify that we navigate to the add screen
        verify(navController).navigate(
            ReminderListFragmentDirections.toSaveReminder()
        )
    }

    @Test
    fun emptyReminders_noDataIndicatorIsDisplayed() {
        // GIVEN - On the home screen
        launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)

        onView(withId(R.id.noDataTextView)).check(matches(withText(R.string.no_data)))
    }
}
