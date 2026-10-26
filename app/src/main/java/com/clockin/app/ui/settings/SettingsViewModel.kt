package com.clockin.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.clockin.app.data.ClockRepository
import com.clockin.app.domain.AppSettings
import com.clockin.app.holiday.HolidayCalendarStatus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SettingsViewModel(private val repository: ClockRepository) : ViewModel() {
    private val updateMutex = Mutex()

    val settings: StateFlow<AppSettings> = repository.settings.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        AppSettings(),
    )

    val holidayCalendarStatus = repository.holidayCalendarStatus.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        HolidayCalendarStatus(),
    )

    fun refreshHolidayCalendar() {
        viewModelScope.launch { repository.refreshHolidayCalendar(force = true) }
    }

    fun update(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch {
            updateMutex.withLock {
                repository.updateSettings(transform(repository.settings.first()))
            }
        }
    }

    class Factory(private val repository: ClockRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SettingsViewModel(repository) as T
    }
}
