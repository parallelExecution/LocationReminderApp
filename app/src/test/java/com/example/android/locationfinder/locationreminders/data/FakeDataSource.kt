package com.example.android.locationfinder.locationreminders.data

import com.example.android.locationfinder.locationreminders.data.dto.ReminderDTO
import com.example.android.locationfinder.locationreminders.data.dto.Result

//Use FakeDataSource that acts as a test double to the LocalDataSource
class FakeDataSource(var reminders: MutableList<ReminderDTO>? = mutableListOf()) :
    ReminderDataSource {

    private var shouldReturnError = false

    fun setReturnError(value: Boolean) {
        shouldReturnError = value
    }

    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        if (shouldReturnError) {
            return Result.Error("Reminders not found")
        }
        reminders?.let { return Result.Success(ArrayList(it)) }
        return Result.Error("Reminders not found")
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        reminders?.add(reminder)
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        if (shouldReturnError) {
            return Result.Error("Reminder not found")
        }
        reminders?.let {
            for (reminder in it) {
                if (reminder.id == id) {
                    return Result.Success(reminder)
                }
            }
            return Result.Error("Reminder not found")
        }
        return Result.Error("Reminders not found")
    }

    override suspend fun deleteAllReminders() {
        reminders?.clear()
    }
}