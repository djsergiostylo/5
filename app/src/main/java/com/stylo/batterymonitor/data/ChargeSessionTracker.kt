package com.stylo.batterymonitor.data

import com.stylo.batterymonitor.data.local.BatteryDao
import com.stylo.batterymonitor.data.local.BatterySnapshotEntity
import com.stylo.batterymonitor.data.local.ChargeSessionEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.max

/** Tracks plug-in → unplug/full sessions and persists snapshots. */
class ChargeSessionTracker(private val dao: BatteryDao) {
    private val _activeSession = MutableStateFlow<ChargeSessionEntity?>(null)
    val activeSession: StateFlow<ChargeSessionEntity?> = _activeSession.asStateFlow()

    suspend fun consume(snapshot: BatterySnapshot): Long {
        var session = _activeSession.value ?: dao.latestSession()?.takeUnless { it.completed }

        if (snapshot.isCharging && session == null) {
            session = ChargeSessionEntity(
                startMs = System.currentTimeMillis(),
                startLevel = snapshot.levelPercent,
            )
            val id = dao.insertSession(session)
            session = session.copy(id = id)
        }

        if (session != null) {
            val peak = listOfNotNull(session.peakTempC, snapshot.temperatureC).maxOrNull()
            val current = snapshot.currentMa?.takeIf { it > 0.0 }
            val previousAvg = session.avgCurrentMa
            val avg = when {
                previousAvg == null -> current
                current == null -> previousAvg
                else -> (previousAvg + current) / 2.0
            }
            val shouldComplete = !snapshot.isCharging || snapshot.isFull || snapshot.levelPercent >= 100
            val updated = session.copy(
                endMs = if (shouldComplete) System.currentTimeMillis() else null,
                endLevel = if (shouldComplete) snapshot.levelPercent else null,
                peakTempC = peak,
                avgCurrentMa = avg,
                completed = shouldComplete,
            )
            if (updated != session) dao.updateSession(updated)
            session = updated
            _activeSession.value = if (shouldComplete) null else session
        }

        val sessionId = if (session?.id ?: 0L > 0L) session!!.id else 0L
        dao.insertSnapshot(
            BatterySnapshotEntity(
                sessionId = sessionId,
                timestampMs = System.currentTimeMillis(),
                levelPercent = snapshot.levelPercent,
                temperatureC = snapshot.temperatureC,
                voltageMv = snapshot.voltageMv,
                currentMa = snapshot.currentMa,
                powerMw = snapshot.powerMw,
                isCharging = snapshot.isCharging,
            ),
        )
        return sessionId
    }

    companion object {
        fun estimatedChargedMah(session: ChargeSessionEntity): Double? {
            val durationHours = session.endMs?.let { max(0L, it - session.startMs) / 3_600_000.0 } ?: return null
            val averageMa = session.avgCurrentMa ?: return null
            return (averageMa * durationHours).takeIf { it > 0.0 }
        }
    }
}
