package com.clockin.app

import android.app.Application
import com.clockin.app.data.AppDatabase
import com.clockin.app.data.ClockRepository
import com.clockin.app.data.SettingsRepository

class ClockInApplication : Application() {
    lateinit var repository: ClockRepository
        private set

    override fun onCreate() {
        super.onCreate()
        val db = AppDatabase.create(this)
        val settingsRepository = SettingsRepository(this)
        repository = ClockRepository(db.clockRecordDao(), settingsRepository)
    }
}
