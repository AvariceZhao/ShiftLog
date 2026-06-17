package com.clockin.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.clockin.app.data.ClockRepository
import com.clockin.app.domain.AppTimeTicker
import com.clockin.app.domain.CycleCalculator
import com.clockin.app.domain.RecordDetail
import com.clockin.app.domain.ShiftCalculator
import com.clockin.app.domain.StatsCalculator
import com.clockin.app.domain.TargetProgress
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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
        viewModelScope.launch { repository.performClockIn() }
    }

    fun clockOut() {
        viewModelScope.launch { repository.performClockOut() }
    }

    class Factory(private val repository: ClockRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            HomeViewModel(repository) as T
    }
}
