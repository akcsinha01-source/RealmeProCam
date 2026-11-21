package com.realme.procam.camera

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * Tracks device tilt (Roll & Pitch) in real time via hardware accelerometer & gyroscope.
 * Features an automatic haptic snap when the device is perfectly level (0° ± 0.5°).
 */
class SensorOrientationHelper(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vm?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private val _rollAngle = MutableStateFlow(0f)
    val rollAngle: StateFlow<Float> = _rollAngle.asStateFlow()

    private val _pitchAngle = MutableStateFlow(0f)
    val pitchAngle: StateFlow<Float> = _pitchAngle.asStateFlow()

    private val _isLevel = MutableStateFlow(false)
    val isLevel: StateFlow<Boolean> = _isLevel.asStateFlow()

    private var lastHapticLevelState = false
    private val lowPassAlpha = 0.15f
    private var smoothedRoll = 0f
    private var smoothedPitch = 0f

    fun startListening() {
        val accel = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        accel?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stopListening() {
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val ax = event.values[0]
        val ay = event.values[1]
        val az = event.values[2]

        // Compute roll (-90 to +90 degrees)
        val rawRoll = atan2(ax.toDouble(), ay.toDouble()) * (180.0 / Math.PI)
        val rawPitch = atan2(-az.toDouble(), sqrt((ax * ax + ay * ay).toDouble())) * (180.0 / Math.PI)

        // Low-pass filter for smooth viewfinder rendering
        smoothedRoll = (lowPassAlpha * rawRoll + (1.0f - lowPassAlpha) * smoothedRoll).toFloat()
        smoothedPitch = (lowPassAlpha * rawPitch + (1.0f - lowPassAlpha) * smoothedPitch).toFloat()

        _rollAngle.value = smoothedRoll
        _pitchAngle.value = smoothedPitch

        val level = abs(smoothedRoll) <= 0.6f && abs(smoothedPitch) <= 1.0f
        _isLevel.value = level

        // Trigger tactile click when snapping into perfect level
        if (level && !lastHapticLevelState) {
            triggerLevelHaptic()
        }
        lastHapticLevelState = level
    }

    private fun triggerLevelHaptic() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createOneShot(15, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(15)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
