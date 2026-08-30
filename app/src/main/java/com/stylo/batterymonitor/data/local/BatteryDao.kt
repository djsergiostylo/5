package com.stylo.batterymonitor.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BatteryDao {

    // ---- Snapshots ----

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnapshot(snapshot: BatterySnapshotEntity): Long

    @Query("SELECT * FROM battery_snapshots WHERE sessionId = :sessionId ORDER BY timestampMs ASC")
    fun snapshotsForSession(sessionId: Long): Flow<List<BatterySnapshotEntity>>

    /** Last N snapshots for the predictor — keep query light. */
    @Query("SELECT * FROM battery_snapshots ORDER BY timestampMs DESC LIMIT :limit")
    suspend fun latestSnapshots(limit: Int = 20): List<BatterySnapshotEntity>

    // ---- Sessions ----

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ChargeSessionEntity): Long

    @Update
    suspend fun updateSession(session: ChargeSessionEntity)

    @Query("SELECT * FROM charge_sessions ORDER BY startMs DESC LIMIT 1")
    suspend fun latestSession(): ChargeSessionEntity?

    /** Completed sessions for health analysis — capped at 30 to stay light. */
    @Query("SELECT * FROM charge_sessions WHERE completed = 1 ORDER BY startMs DESC LIMIT 30")
    suspend fun completedSessions(): List<ChargeSessionEntity>

    @Query("SELECT * FROM charge_sessions ORDER BY startMs DESC")
    fun allSessionsFlow(): Flow<List<ChargeSessionEntity>>
}
