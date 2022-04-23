package com.example.android.locationfinder.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.example.android.locationfinder.locationreminders.data.ReminderDataSource
import com.example.android.locationfinder.locationreminders.data.dto.ReminderDTO
import com.example.android.locationfinder.locationreminders.data.dto.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Medium Test to test the repository
@MediumTest
class RemindersLocalRepositoryTest {

    // Executes each task synchronously using Architecture Components.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var localDataSource: RemindersLocalRepository
    private lateinit var database: RemindersDatabase

    @Before
    fun setup() {
        // Using an in-memory database for testing, because it doesn't survive killing the process.
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()

        localDataSource =
            RemindersLocalRepository(
                database.reminderDao(),
                Dispatchers.Main
            )
    }

    @After
    fun cleanUp() {
        database.close()
    }

    @Test
    fun saveReminder_retrievesReminder() = runBlocking {
        // GIVEN - A new reminder saved in the database.
        val reminder = ReminderDTO("title", "description", "location", 1.0, 1.0)
        localDataSource.saveReminder(reminder)

        // WHEN  - reminder retrieved by ID.
        val result = localDataSource.getReminder(reminder.id)

        // THEN - Same reminder is returned.
        result as Result.Success
        assertThat(result.data.title, `is`("title"))
        assertThat(result.data.description, `is`("description"))
        assertThat(result.data.location, `is`("location"))
        assertThat(result.data.latitude, `is`(1.0))
        assertThat(result.data.longitude, `is`(1.0))
    }

    @Test
    fun dontSaveReminderAndRetrieveReminderById_returnsError() = runBlocking {
        // GIVEN - A new reminder saved in the database.
        val reminder = ReminderDTO("title", "description", "location", 1.0, 1.0)

        // WHEN  - reminder retrieved by ID.
        val result = localDataSource.getReminder(reminder.id)

        // THEN - Error is returned
        result as Result.Error
        assertThat(result.message, `is`("Reminder not found!"))
    }
}