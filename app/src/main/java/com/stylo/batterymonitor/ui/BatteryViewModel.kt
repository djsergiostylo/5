package com.stylo.batterymonitor.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.stylo.batterymonitor.data.BatteryMonitorRepository

class BatteryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = BatteryMonitorRepository(application)

    val snapshot = repository.snapshot

    init {
        repository.start()
    }

    override fun onCleared() {
        repository.stop()
        super.onCleared()
    }
}
