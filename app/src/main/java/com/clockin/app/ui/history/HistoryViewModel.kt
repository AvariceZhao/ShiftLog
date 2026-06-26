package com.clockin.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.clockin.app.data.ClockRepository
import com.clockin.app.domain.AppTimeTicker
import com.clockin.app.domain.AppBackup
import com.clockin.app.domain.AppSettings
import com.clockin.app.domain.BackupExporter
import com.clockin.app.domain.BackupImporter
import com.clockin.app.domain.ClockRecord
import com.clockin.app.domain.CycleCalculator
import com.clockin.app.domain.CsvExporter
import com.clockin.app.domain.CycleStats
import com.clockin.app.domain.PayCycle
import com.clockin.app.domain.RecordDetail
import com.clockin.app.domain.ShiftCalculator
import com.clockin.app.domain.StatsCalculator
import com.clockin.app.domain.TargetProgress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HistoryUiState(
    val cycle: PayCycle? = null,
    val records: List<RecordDetail> = emptyList(),
    val stats: CycleStats? = null,
    val progress: TargetProgress? = null,
)

class HistoryViewModel(private val repository: ClockRepository) : ViewModel() {
    private val cycleOverride = MutableStateFlow<PayCycle?>(null)

    val uiState: StateFlow<HistoryUiState> =
        combine(
            repository.settings,
            cycleOverride,
            AppTimeTicker.localDateFlow(),
        ) { settings, override, today ->
            settings to (override ?: CycleCalculator.currentCycle(settings.cycleStartDay, today))
        }.flatMapLatest { (settings, cycle) ->
            repository.observeCycleRecords(cycle).map { records ->
                HistoryUiState(
                    cycle = cycle,
                    records = records.map { ShiftCalculator.buildRecordDetail(it, settings) },
                    stats = StatsCalculator.cycleStats(records, settings, cycle),
                    progress = StatsCalculator.targetProgress(records, settings, cycle),
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HistoryUiState())

    private val settingsState = repository.settings.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        AppSettings(),
    )

    val settings: StateFlow<AppSettings> = settingsState

    fun previousCycle() {
        val current = uiState.value.cycle ?: return
        val settings = settingsState.value
        cycleOverride.update { CycleCalculator.previousCycle(current, settings.cycleStartDay) }
    }

    fun nextCycle() {
        val current = uiState.value.cycle ?: return
        val settings = settingsState.value
        cycleOverride.update { CycleCalculator.nextCycle(current, settings.cycleStartDay) }
    }

    fun goToCurrentCycle() {
        cycleOverride.value = null
    }

    fun deleteRecord(shiftDate: String) {
        viewModelScope.launch { repository.deleteRecord(shiftDate) }
    }

    fun saveRecord(record: ClockRecord) {
        viewModelScope.launch { repository.saveRecord(record) }
    }

    fun buildExport(onReady: (content: String, fileName: String) -> Unit) {
        viewModelScope.launch {
            val settings = settingsState.value
            val records = repository.getAllRecords()
            onReady(
                CsvExporter.export(records, settings),
                CsvExporter.fileName(),
            )
        }
    }

    fun buildBackup(onReady: (content: String, fileName: String) -> Unit) {
        viewModelScope.launch {
            val settings = settingsState.value
            val records = repository.getAllRecords()
            onReady(
                BackupExporter.export(settings, records),
                BackupExporter.fileName(),
            )
        }
    }

    fun parseBackupJson(
        text: String,
        onReady: (AppBackup) -> Unit,
        onError: (String) -> Unit,
    ) {
        BackupImporter.parseJson(text)
            .onSuccess(onReady)
            .onFailure { onError(it.message ?: "备份文件无效") }
    }

    fun parseCsvImport(
        text: String,
        onReady: (List<ClockRecord>) -> Unit,
        onError: (String) -> Unit,
    ) {
        viewModelScope.launch {
            val settings = settingsState.value
            BackupImporter.parseCsv(text, settings)
                .onSuccess(onReady)
                .onFailure { onError(it.message ?: "CSV 文件无效") }
        }
    }

    fun restoreBackup(backup: AppBackup, replaceExisting: Boolean, onDone: (String) -> Unit) {
        viewModelScope.launch {
            runCatching {
                repository.restoreBackup(backup, replaceExisting = replaceExisting)
            }.onSuccess {
                onDone(if (replaceExisting) "已完全恢复备份" else "已合并导入备份")
            }.onFailure {
                onDone(it.message ?: "恢复失败")
            }
        }
    }

    fun mergeCsvRecords(records: List<ClockRecord>, onDone: (String) -> Unit) {
        viewModelScope.launch {
            runCatching {
                repository.mergeRecords(records)
            }.onSuccess {
                onDone("已导入 ${records.size} 条记录")
            }.onFailure {
                onDone(it.message ?: "导入失败")
            }
        }
    }

    class Factory(private val repository: ClockRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            HistoryViewModel(repository) as T
    }
}
