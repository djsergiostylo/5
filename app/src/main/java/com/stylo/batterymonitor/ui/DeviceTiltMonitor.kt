package com.stylo.batterymonitor.ui

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.abs

/**
 * Production orientation source for the 3D battery.
 * Reuses all buffers, handles yaw wrap-around and applies a delta dead-zone.
 */
class DeviceTiltMonitor(context: Context) : SensorEventListener {
    private val manager = context.applicationContext
        .getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val sensor = manager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    private var listener: ((Float, Float, Float) -> Unit)? = null
    private var lastPitch = 0f
    private var lastRoll = 0f
    private var lastYaw = 0f
    private var initialized = false

    // Reused for every sensor callback: no per-event allocations.
    private val rotationMatrix = FloatArray(9)
    private val orientationValues = FloatArray(3)

    fun start(onTilt: (Float, Float, Float) -> Unit) {
        stop()
        listener = onTilt
        initialized = false
        sensor?.let {
            manager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun stop() {
        manager.unregisterListener(this)
        listener = null
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ROTATION_VECTOR) return

        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
        SensorManager.getOrientation(rotationMatrix, orientationValues)

        val rawYaw = Math.toDegrees(orientationValues[0].toDouble()).toFloat()
        val rawPitch = Math.toDegrees(orientationValues[1].toDouble()).toFloat()
        val rawRoll = Math.toDegrees(orientationValues[2].toDouble()).toFloat()

        // Avoid a large first-frame interpolation from the default zero orientation.
        if (!initialized) {
            lastPitch = rawPitch
            lastRoll = rawRoll
            lastYaw = rawYaw
            initialized = true
            listener?.invoke(lastPitch, lastRoll, lastYaw)
            return
        }

        val targetPitch = applyDeadZone(lastPitch, rawPitch)
        val targetRoll = applyDeadZone(lastRoll, rawRoll)
        val targetYaw = applyDeadZone(lastYaw, shortestAngleDelta(lastYaw, rawYaw))

        lastPitch = smooth(lastPitch, targetPitch)
        lastRoll = smooth(lastRoll, targetRoll)
        lastYaw = normalizeAngle(lastYaw + shortestAngleDelta(lastYaw, targetYaw) * SMOOTHING)

        listener?.invoke(lastPitch, lastRoll, lastYaw)
    }

    private fun applyDeadZone(previous: Float, target: Float): Float {
        return if (abs(target - previous) < DEAD_ZONE_DEGREES) previous else target
    }

    private fun smooth(previous: Float, target: Float): Float =
        previous + (target - previous) * SMOOTHING

    private fun shortestAngleDelta(from: Float, to: Float): Float {
        var delta = (to - from) % 360f
        if (delta > 180f) delta -= 360f
        if (delta < -180f) delta += 360f
        return delta
    }

    private fun normalizeAngle(angle: Float): Float {
        var normalized = angle % 360f
        if (normalized > 180f) normalized -= 360f
        if (normalized < -180f) normalized += 360f
        return normalized
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private companion object {
        const val SMOOTHING = 0.16f
        const val DEAD_ZONE_DEGREES = 0.12f
    }
}
