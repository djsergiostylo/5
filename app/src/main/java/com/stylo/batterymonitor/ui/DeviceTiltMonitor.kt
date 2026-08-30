package com.stylo.batterymonitor.ui

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.PI

/** Native motion source for the immersive 3D battery view. */
class DeviceTiltMonitor(context: Context) : SensorEventListener {
    fun interface Listener {
        fun onTilt(pitch: Float, roll: Float, yaw: Float)
    }

    private val sensorManager = context.applicationContext
        .getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private var listener: Listener? = null
    private val rotationMatrix = FloatArray(9)
    private val orientation = FloatArray(3)
    private var running = false

    fun start(listener: Listener) {
        this.listener = listener
        if (running || rotationSensor == null) return
        running = sensorManager.registerListener(
            this,
            rotationSensor,
            SensorManager.SENSOR_DELAY_GAME,
        )
    }

    fun stop() {
        if (running) sensorManager.unregisterListener(this)
        running = false
        listener = null
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ROTATION_VECTOR) return
        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
        SensorManager.getOrientation(rotationMatrix, orientation)

        val yaw = (orientation[0] * 180f / PI.toFloat()).coerceIn(-180f, 180f)
        val pitch = (orientation[1] * 180f / PI.toFloat()).coerceIn(-90f, 90f)
        val roll = (orientation[2] * 180f / PI.toFloat()).coerceIn(-180f, 180f)
        listener?.onTilt(pitch, roll, yaw)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
