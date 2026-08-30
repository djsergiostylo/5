package com.stylo.batterymonitor.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.stylo.batterymonitor.data.BatteryHealthAnalyzer
import com.stylo.batterymonitor.data.BatteryMonitorRepository
import com.stylo.batterymonitor.data.ChargeSessionTracker
import com.stylo.batterymonitor.data.ChargingTimePredictor
import com.stylo.batterymonitor.data.local.BatteryDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BatteryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = BatteryMonitorRepository(application)
    private val dao = BatteryDatabase.getInstance(application).batteryDao()
    private val sessionTracker = ChargeSessionTracker(dao)
    private val healthAnalyzer = BatteryHealthAnalyzer(dao)

    private val _prediction = MutableStateFlow<ChargingTimePredictor.Prediction?>(null)
    val prediction: StateFlow<ChargingTimePredictor.Prediction?> = _prediction.asStateFlow()

    private val _health = MutableStateFlow<BatteryHealthAnalyzer.HealthResult?>(null)
    val health: StateFlow<BatteryHealthAnalyzer.HealthResult?> = _health.asStateFlow()

    val snapshot = repository.snapshot

    init {
        repository.start()
        viewModelScope.launch {
            repository.snapshot.collect { value ->
                val sessionId = sessionTracker.consume(value)
                if (value.isCharging && sessionId > 0L) {
                    val points = dao.latestSnapshotsForSession(sessionId, 12)
                        .reversed()
                        .map { ChargingTimePredictor.SnapshotPoint(it.timestampMs, it.levelPercent) }
                    _prediction.value = ChargingTimePredictor.predict(points)
                } else if (!value.isCharging) {
                    _prediction.value = null
                    if (sessionId > 0L) {
                        _health.value = healthAnalyzer.analyze()
                    }
                }
            }
        }
        refreshHealth()
    }

    fun refreshHealth() {
        viewModelScope.launch { _health.value = healthAnalyzer.analyze() }
    }

    override fun onCleared() {
        repository.stop()
        super.onCleared()
    }
}
