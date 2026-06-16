package com.clockin.app.data

import com.clockin.app.domain.AppSettings
import com.clockin.app.domain.ClockRecord
import com.clockin.app.domain.PayCycle
import com.clockin.app.domain.ShiftCalculator
import com.clockin.app.domain.toShiftDateString
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime

class ClockRepository(
    private val dao: ClockRecordDao,
    private val settingsRepository: SettingsRepository,
) {
    val settings: Flow<AppSettings> = settingsRepository.settings

    fun observeActiveShift(): Flow<Pair<String, ClockRecord?>> =
        combine(settings, dao.observeLatestOpenShift()) { currentSettings, openEntity ->
            val open = openEntity?.toDomain()
            ShiftCalculator.resolveActiveShiftDate(LocalDateTime.now(), currentSettings, open)
        }.flatMapLatest { shiftDate ->
            dao.observeByShiftDate(shiftDate).map { entity ->
                shiftDate to entity?.toDomain()
            }
        }

    fun observeCycleRecords(cycle: PayCycle): Flow<List<ClockRecord>> =
        dao.observeInRange(
            cycle.start.toShiftDateString(),
            cycle.end.toShiftDateString(),
        ).map { list -> list.map { it.toDomain() } }

    suspend fun performClockIn() {
        val nowMs = System.currentTimeMillis()
        val shiftDate = ShiftCalculator.shiftDateForClockIn(nowMs)
        val existing = dao.getByShiftDate(shiftDate)?.toDomain()
        if (existing?.clockInTime != null) return
        dao.upsert(
            ClockRecord(
                shiftDate = shiftDate,
                clockInTime = nowMs,
                clockOutTime = existing?.clockOutTime,
            ).toEntity(),
        )
    }

    suspend fun performClockOut() {
        val settings = settings.first()
        val nowMs = System.currentTimeMillis()
        val open = dao.findLatestOpenShift()?.toDomain()
        val shiftDate = ShiftCalculator.resolveActiveShiftDate(
            LocalDateTime.now(),
            settings,
            open,
        )
        val existing = dao.getByShiftDate(shiftDate)?.toDomain()
        if (existing?.clockInTime == null) return
        dao.upsert(
            ClockRecord(
                shiftDate = shiftDate,
                clockInTime = existing.clockInTime,
                clockOutTime = nowMs,
            ).toEntity(),
        )
    }

    suspend fun saveRecord(record: ClockRecord) {
        dao.upsert(record.toEntity())
    }

    suspend fun deleteRecord(shiftDate: String) {
        dao.delete(shiftDate)
    }

    suspend fun updateSettings(settings: AppSettings) {
        settingsRepository.updateSettings(settings)
    }

    suspend fun getCycleRecords(cycle: PayCycle): List<ClockRecord> =
        observeCycleRecords(cycle).first()
}
