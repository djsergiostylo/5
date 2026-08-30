package com.stylo.batterymonitor.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BatteryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnapshot(snapshot: BatterySnapshotEntity): Long

    @Query("SELECT * FROM battery_snapshots WHERE sessionId = :sessionId ORDER BY timestampMs ASC")
    fun snapshotsForSession(sessionId: Long): Flow<List<BatterySnapshotEntity>>

    @Query("SELECT * FROM battery_snapshots WHERE sessionId = :sessionId ORDER BY timestampMs DESC LIMIT :limit")
    suspend fun latestSnapshotsForSession(sessionId: Long, limit: Int = 20): List<BatterySnapshotEntity>

    @Query("SELECT * FROM battery_snapshots ORDER BY timestampMs DESC LIMIT :limit")
    suspend fun latestSnapshots(limit: Int = 20): List<BatterySnapshotEntity>

    @Query("DELETE FROM battery_snapshots WHERE id NOT IN (SELECT id FROM battery_snapshots ORDER BY timestampMs DESC LIMIT :keep)")
    suspend fun trimSnapshots(keep: Int = MAX_SNAPSHOT_ROWS)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ChargeSessionEntity): Long

    @Update
    suspend fun updateSession(session: ChargeSessionEntity)

    @Query("SELECT * FROM charge_sessions ORDER BY startMs DESC LIMIT 1")
    suspend fun latestSession(): ChargeSessionEntity?

    @Query("SELECT * FROM charge_sessions WHERE completed = 1 ORDER BY startMs DESC LIMIT 30")
    suspend fun completedSessions(): List<ChargeSessionEntity>

    @Query("SELECT * FROM charge_sessions ORDER BY startMs DESC")
    fun allSessionsFlow(): Flow<List<ChargeSessionEntity>>

    companion object {
        const val MAX_SNAPSHOT_ROWS = 10_000
    }
}
