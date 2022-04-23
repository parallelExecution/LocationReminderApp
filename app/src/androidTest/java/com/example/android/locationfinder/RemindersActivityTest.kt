package com.example.android.locationfinder

import android.app.Application
import android.app.PendingIntent.getActivity
import android.view.View
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.withDecorView
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.firebase.FirebaseApp
import com.example.android.locationfinder.locationreminders.RemindersActivity
import com.example.android.locationfinder.locationreminders.data.ReminderDataSource
import com.example.android.locationfinder.locationreminders.data.dto.ReminderDTO
import com.example.android.locationfinder.locationreminders.data.local.LocalDB
import com.example.android.locationfinder.locationreminders.data.local.RemindersLocalRepository
import com.example.android.locationfinder.locationreminders.reminderslist.ReminderDataItem
import com.example.android.locationfinder.locationreminders.reminderslist.RemindersListViewModel
import com.example.android.locationfinder.locationreminders.savereminder.SaveReminderViewModel
import com.example.android.locationfinder.util.CustomMatcher.atPosition
import com.example.android.locationfinder.util.DataBindingIdlingResource
import com.example.android.locationfinder.util.ToastMatcher
import com.example.android.locationfinder.util.monitorActivity
import com.example.android.locationfinder.utils.EspressoIdlingResource
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.*
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
@LargeTest
//END TO END test to black box test the app
class RemindersActivityTest :
    AutoCloseKoinTest() {// Extended Koin Test - embed autoclose @after method to close Koin after every test

    private lateinit var repository: ReminderDataSource
    private lateinit var appContext: Application
    private lateinit var viewModelList: RemindersListViewModel
    private lateinit var viewModelSave: SaveReminderViewModel

    // An idling resource that waits for Data Binding to have no pending bindings.
    private val dataBindingIdlingResource = DataBindingIdlingResource()

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    /**
     * Idling resources tell Espresso that the app is idle or busy. This is needed when operations
     * are not scheduled in the main Looper (for example when executed on a different thread).
     */
    @Before
    fun registerIdlingResource() {
        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)
        IdlingRegistry.getInstance().register(dataBindingIdlingResource)
    }

    /**
     * Unregister your Idling Resource so it can be garbage collected and does not leak any memory.
     */
    @After
    fun unregisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.countingIdlingResource)
        IdlingRegistry.getInstance().unregister(dataBindingIdlingResource)
    }

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
                    get() as ReminderDataSource
                )
            }
            single {
                SaveReminderViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            single { RemindersLocalRepository(get()) as ReminderDataSource }
            single { LocalDB.createRemindersDao(appContext) }
        }
        //declare a new koin module
        startKoin {
            modules(listOf(myModule))
        }
        //Get our real repository
        repository = get()

        // Get our viewmodels
        viewModelList = get()
        viewModelSave = get()

        //clear the data to start fresh
        runBlocking {
            repository.deleteAllReminders()
        }
    }

    @Test
    fun successfullyAddReminder_showToast() = runBlocking {
        // Set initial state.
        repository.saveReminder(ReminderDTO("TITLE", "DESCRIPTION", "LOCATION", 1.0, 1.0))

        // Start up Reminder screen.
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        // Espresso code will go here.
        // THEN - Reminder details are displayed on the screen
        // make sure that the title/description are both shown and correct
        onView(withId(R.id.reminderssRecyclerView))
            .check(matches(atPosition(0, hasDescendant(withText("TITLE")))))
            .check(matches(atPosition(0, hasDescendant(withText("DESCRIPTION")))))
            .check(matches(atPosition(0, hasDescendant(withText("LOCATION")))))

        // Click on the FAB button and save reminder.
        onView(withId(R.id.addReminderFAB)).perform(click())
        val reminderDataItem = ReminderDataItem(
            "googleplex",
            "building",
            "Googleplex",
            37.42206582174193,
            122.08409070968628
        )

        viewModelSave.reminderTitle.value = reminderDataItem.title
        viewModelSave.reminderSelectedLocationStr.value = reminderDataItem.location
        viewModelSave.reminderDescription.value = reminderDataItem.description
        viewModelSave.latitude.value = reminderDataItem.latitude
        viewModelSave.longitude.value = reminderDataItem.longitude

        // Clicking on save button
        onView(withId(R.id.saveReminder)).perform(click())

        // Check if reminder saved toast is displayed
        // https://stackoverflow.com/a/49834662
        onView(withText(R.string.reminder_saved))
            .inRoot(ToastMatcher().apply {
                matches(isDisplayed())
            });

        // make sure second reminder is displayed in recycler view
        onView(withId(R.id.reminderssRecyclerView))
            .check(matches(atPosition(1, hasDescendant(withText("googleplex")))))
            .check(matches(atPosition(1, hasDescendant(withText("building")))))
            .check(matches(atPosition(1, hasDescendant(withText("Googleplex")))))

        // Make sure the activity is closed before resetting the db:
        activityScenario.close()
    }

    @Test
    fun titleEmpty_showSnackbar() = runBlocking {
        // Set initial state.
        repository.saveReminder(ReminderDTO("TITLE", "DESCRIPTION", "LOCATION", 1.0, 1.0))

        // Start up Reminder screen.
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        // Espresso code will go here.
        // THEN - Reminder details are displayed on the screen
        // make sure that the title/description are both shown and correct
        onView(withId(R.id.reminderssRecyclerView))
            .check(matches(atPosition(0, hasDescendant(withText("TITLE")))))
            .check(matches(atPosition(0, hasDescendant(withText("DESCRIPTION")))))
            .check(matches(atPosition(0, hasDescendant(withText("LOCATION")))))

        // Click on the FAB button and save reminder.
        onView(withId(R.id.addReminderFAB)).perform(click())
        val reminderDataItem = ReminderDataItem(
            "googleplex",
            "building",
            "Googleplex",
            37.42206582174193,
            122.08409070968628
        )

        // Given title empty
        // viewModelSave.reminderTitle.value = reminderDataItem.title
        viewModelSave.reminderSelectedLocationStr.value = reminderDataItem.location
        viewModelSave.reminderDescription.value = reminderDataItem.description
        viewModelSave.latitude.value = reminderDataItem.latitude
        viewModelSave.longitude.value = reminderDataItem.longitude

        // Clicking on save button
        onView(withId(R.id.saveReminder)).perform(click())

        // Snackbar message should be displayed when the reminder title is empty
        onView(allOf(withId(R.id.snackbar_text), withText(R.string.err_enter_title)))
            .check(matches(isDisplayed()))

        // Make sure the activity is closed before resetting the db:
        activityScenario.close()
    }

    @Test
    fun locationEmpty_showSnackbar() = runBlocking {
        // Set initial state.
        repository.saveReminder(ReminderDTO("TITLE", "DESCRIPTION", "LOCATION", 1.0, 1.0))

        // Start up Reminder screen.
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        // Espresso code will go here.
        // THEN - Reminder details are displayed on the screen
        // make sure that the title/description are both shown and correct
        onView(withId(R.id.reminderssRecyclerView))
            .check(matches(atPosition(0, hasDescendant(withText("TITLE")))))
            .check(matches(atPosition(0, hasDescendant(withText("DESCRIPTION")))))
            .check(matches(atPosition(0, hasDescendant(withText("LOCATION")))))

        // Click on the FAB button and save reminder.
        onView(withId(R.id.addReminderFAB)).perform(click())
        val reminderDataItem = ReminderDataItem(
            "googleplex",
            "building",
            "Googleplex",
            37.42206582174193,
            122.08409070968628
        )

        // Given location empty
        viewModelSave.reminderTitle.value = reminderDataItem.title
        // viewModelSave.reminderSelectedLocationStr.value = reminderDataItem.location
        viewModelSave.reminderDescription.value = reminderDataItem.description
        viewModelSave.latitude.value = reminderDataItem.latitude
        viewModelSave.longitude.value = reminderDataItem.longitude

        // Clicking on save button
        onView(withId(R.id.saveReminder)).perform(click())

        // Snackbar message should be displayed when the reminder title is empty
        onView(allOf(withId(R.id.snackbar_text), withText(R.string.err_select_location)))
            .check(matches(isDisplayed()))

        // Make sure the activity is closed before resetting the db:
        activityScenario.close()
    }
}
