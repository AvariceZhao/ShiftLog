package com.clockin.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ClockRecordDao {
    @Query("SELECT * FROM clock_records WHERE shiftDate = :shiftDate LIMIT 1")
    fun observeByShiftDate(shiftDate: String): Flow<ClockRecordEntity?>

    @Query("SELECT * FROM clock_records WHERE shiftDate = :shiftDate LIMIT 1")
    suspend fun getByShiftDate(shiftDate: String): ClockRecordEntity?

    @Query(
        """
        SELECT * FROM clock_records
        WHERE shiftDate >= :start AND shiftDate <= :end
        ORDER BY shiftDate DESC
        """,
    )
    fun observeInRange(start: String, end: String): Flow<List<ClockRecordEntity>>

    @Query(
        """
        SELECT * FROM clock_records
        WHERE clockInTime IS NOT NULL AND clockOutTime IS NULL
        ORDER BY shiftDate DESC
        LIMIT 1
        """,
    )
    fun observeLatestOpenShift(): Flow<ClockRecordEntity?>

    @Query(
        """
        SELECT * FROM clock_records
        WHERE clockInTime IS NOT NULL AND clockOutTime IS NULL
        ORDER BY shiftDate DESC
        LIMIT 1
        """,
    )
    suspend fun findLatestOpenShift(): ClockRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: ClockRecordEntity)

    @Query("DELETE FROM clock_records WHERE shiftDate = :shiftDate")
    suspend fun delete(shiftDate: String)
}
