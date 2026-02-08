package com.example.chillmusic.logic.sensor

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Looper
import com.example.chillmusic.data.model.MotionSettings
import com.example.chillmusic.data.model.MotionState
import com.example.chillmusic.data.model.MotionState.*
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sqrt

class MotionDetector(private val context: Context, private val scope: CoroutineScope) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)

    private val _motionState = MutableStateFlow(STOPPED)
    val motionState: StateFlow<MotionState> = _motionState.asStateFlow()

    private val _currentSpeed = MutableStateFlow(0f)
    val currentSpeed: StateFlow<Float> = _currentSpeed.asStateFlow()

    private var motionSettings = MotionSettings()
    private var isDetecting = false

    private var lastAcceleration = 0f
    private var lastGeoSpeed = 0f

    // Buffer for smoothing acceleration (simple moving average)
    private val accelBuffer = FloatArray(10)
    private var bufferIndex = 0

    fun updateSettings(settings: MotionSettings) {
        this.motionSettings = settings
        // Re-evaluate current state with new settings
        evaluateMotion()
    }

    @SuppressLint("MissingPermission")
    fun startDetection() {
        if (isDetecting) return
        isDetecting = true

        // Accelerometer
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }

        // GPS
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000)
            .setMinUpdateIntervalMillis(1000)
            .build()

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            // Permission should be checked before calling startDetection
            e.printStackTrace()
        }
    }

    fun stopDetection() {
        if (!isDetecting) return
        isDetecting = false
        sensorManager.unregisterListener(this)
        fusedLocationClient.removeLocationUpdates(locationCallback)
        
        // Reset state
        _motionState.value = STOPPED
        _currentSpeed.value = 0f
        lastAcceleration = 0f
        lastGeoSpeed = 0f
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (it.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                val x = it.values[0]
                val y = it.values[1]
                val z = it.values[2]

                val magnitude = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
                val netAcceleration = abs(magnitude - 9.81f)

                // Smoothing
                accelBuffer[bufferIndex] = netAcceleration
                bufferIndex = (bufferIndex + 1) % accelBuffer.size
                lastAcceleration = accelBuffer.average().toFloat()

                evaluateMotion()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { location ->
                if (location.hasSpeed()) {
                    lastGeoSpeed = location.speed
                    evaluateMotion()
                }
            }
        }
    }

    private fun evaluateMotion() {
        if (!isDetecting) return

        val factor = (11 - motionSettings.sensitivity) / 5f
        val walkThreshold = motionSettings.walkingThreshold * factor
        val runThreshold = motionSettings.runningThreshold * factor

        // Prefer GPS speed if available and significant, otherwise use acceleration
        // Transforming acceleration to "speed equivalent" for classification is tricky physically,
        // but specified logic uses 'speed' variable source switching.
        // For acceleration, we treat the magnitude of motion as a proxy for speed.
        
        val effectiveSpeed = if (lastGeoSpeed > 0.5f) lastGeoSpeed else lastAcceleration

        val newState = when {
            effectiveSpeed >= runThreshold -> RUNNING
            effectiveSpeed >= walkThreshold -> WALKING
            else -> STOPPED
        }

        scope.launch(Dispatchers.Main) {
            _motionState.value = newState
            _currentSpeed.value = effectiveSpeed
        }
    }
}
