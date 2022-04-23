package com.example.android.locationfinder.locationreminders.reminderslist

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.FirebaseApp
import com.example.android.locationfinder.locationreminders.MainCoroutineRule
import com.example.android.locationfinder.locationreminders.data.FakeDataSource
import com.example.android.locationfinder.locationreminders.data.dto.ReminderDTO
import com.example.android.locationfinder.locationreminders.getOrAwaitValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.pauseDispatcher
import kotlinx.coroutines.test.resumeDispatcher
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
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

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class RemindersListViewModelTest : AutoCloseKoinTest() {
    // Extended Koin Test - embed autoclose @after method to close Koin after every test

    private lateinit var repository: FakeDataSource
    private lateinit var appContext: Application
    private lateinit var viewModel: RemindersListViewModel

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    /**
     * As we use Koin as a Service Locator Library to develop our code, we'll also use Koin to test our code.
     * at this step we will initialize Koin related code to be able to use it in out testing.
     */
    @Before
    fun init() {
        stopKoin()//stop the original app koin
        appContext = ApplicationProvider.getApplicationContext()
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
            val reminder1 = ReminderDTO("title1", "description1", "location1", 1.0, 1.0)
            val reminder2 = ReminderDTO("title2", "description2", "location2", 2.0, 2.0)
            repository.saveReminder(reminder1)
            repository.saveReminder(reminder2)
        }
    }

    @After
    fun end() {
        autoClose()
    }

    @Test
    fun loadReminders_showLoading() {
        // Pause dispatcher so you can verify initial values.
        mainCoroutineRule.pauseDispatcher()

        // Load the reminders in the view model.
        viewModel.loadReminders()

        // Then progress indicator is shown.
        assertThat(viewModel.showLoading.getOrAwaitValue(), `is`(true))

        // Execute pending coroutines actions.
        mainCoroutineRule.resumeDispatcher()

        // Then progress indicator is hidden.
        assertThat(viewModel.showLoading.getOrAwaitValue(), `is`(false))
    }

    @Test
    fun loadRemindersWhenRemindersAreUnavailable_showSnackBar() {
        // Set to return error
        repository.setReturnError(true)

        // Load the reminders in the view model.
        viewModel.loadReminders()

        // Assert that the snackbar has been updated with the correct text.
        val snackbarText = viewModel.showSnackBar.getOrAwaitValue()
        assertThat(snackbarText, `is`("Reminders not found"))
    }
}