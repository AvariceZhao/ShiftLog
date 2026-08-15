package com.clockin.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.clockin.app.data.ClockRepository
import com.clockin.app.domain.AppSettings
import com.clockin.app.domain.AppTimeTicker
import com.clockin.app.domain.ClockRecord
import com.clockin.app.domain.CycleCalculator
import com.clockin.app.domain.PunchResult
import com.clockin.app.domain.RecordDetail
import com.clockin.app.domain.ShiftCalculator
import com.clockin.app.domain.StatsCalculator
import com.clockin.app.domain.TargetProgress
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class HomeUiState(
    val isReady: Boolean = false,
    val shiftDate: String = "",
    val detail: RecordDetail? = null,
    val canClockIn: Boolean = false,
    val canClockOut: Boolean = false,
    val cycleLabel: String = "",
    val progress: TargetProgress? = null,
)

class HomeViewModel(private val repository: ClockRepository) : ViewModel() {
    private val _fireworksEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val fireworksEvent = _fireworksEvent.asSharedFlow()

    val settings: StateFlow<AppSettings> = repository.settings.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        AppSettings(),
    )

    val uiState: StateFlow<HomeUiState> =
        combine(repository.settings, AppTimeTicker.localDateFlow()) { settings, today ->
            settings to today
        }.flatMapLatest { (settings, today) ->
            val cycle = CycleCalculator.currentCycle(settings.cycleStartDay, today)
            combine(
                repository.observeActiveShift(),
                repository.observeCycleRecords(cycle),
            ) { active, cycleRecords ->
                val (shiftDate, record) = active
                val detail = record?.let { ShiftCalculator.buildRecordDetail(it, settings) }
                HomeUiState(
                    isReady = true,
                    shiftDate = shiftDate,
                    detail = detail,
                    canClockIn = record?.clockInTime == null,
                    canClockOut = record?.clockInTime != null,
                    cycleLabel = cycle.label(),
                    progress = StatsCalculator.targetProgress(cycleRecords, settings, cycle),
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    fun clockIn() {
        viewModelScope.launch {
            if (repository.performClockIn() == PunchResult.Success) {
                celebrateIfTargetMet()
            }
        }
    }

    fun clockOut() {
        viewModelScope.launch {
            if (repository.performClockOut() == PunchResult.Success) {
                celebrateIfTargetMet()
            }
        }
    }

    fun saveRecord(record: ClockRecord) {
        viewModelScope.launch { repository.saveRecord(record) }
    }

    fun deleteRecord(shiftDate: String) {
        viewModelScope.launch { repository.deleteRecord(shiftDate) }
    }

    private suspend fun celebrateIfTargetMet() {
        val settings = repository.settings.first()
        val today = LocalDate.now()
        val cycle = CycleCalculator.currentCycle(settings.cycleStartDay, today)
        val records = repository.getCycleRecords(cycle)
        if (StatsCalculator.targetProgress(records, settings, cycle, today).isTargetMet) {
            _fireworksEvent.emit(Unit)
        }
    }

    class Factory(private val repository: ClockRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            HomeViewModel(repository) as T
    }
}
