package com.stylo.batterymonitor.ui

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

/** Smooth device orientation for the 3D battery. Emits degrees: pitch, roll, yaw. */
class DeviceTiltMonitor(context: Context) : SensorEventListener {
    private val manager = context.applicationContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val sensor = manager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private var listener: ((Float, Float, Float) -> Unit)? = null
    private var lastPitch = 0f
    private var lastRoll = 0f
    private var lastYaw = 0f

    fun start(onTilt: (Float, Float, Float) -> Unit) {
        listener = onTilt
        sensor?.let { manager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
    }

    fun stop() {
        manager.unregisterListener(this)
        listener = null
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ROTATION_VECTOR) return
        val rotation = FloatArray(9)
        SensorManager.getRotationMatrixFromVector(rotation, event.values)
        val orientation = FloatArray(3)
        SensorManager.getOrientation(rotation, orientation)
        val rawYaw = Math.toDegrees(orientation[0].toDouble()).toFloat()
        val rawPitch = Math.toDegrees(orientation[1].toDouble()).toFloat()
        val rawRoll = Math.toDegrees(orientation[2].toDouble()).toFloat()

        // Dead-zone + exponential smoothing keeps the WebView stable on a handheld phone.
        val pitch = smooth(lastPitch, deadZone(rawPitch))
        val roll = smooth(lastRoll, deadZone(rawRoll))
        val yaw = smooth(lastYaw, deadZone(rawYaw))
        lastPitch = pitch; lastRoll = roll; lastYaw = yaw
        listener?.invoke(pitch, roll, yaw)
    }

    private fun deadZone(value: Float): Float = if (abs(value) < 1.2f) 0f else value
    private fun smooth(previous: Float, target: Float): Float = previous + (target - previous) * 0.16f
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
