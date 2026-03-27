package com.example.chillmusic.logic.sensor

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Looper
import com.example.chillmusic.data.repository.FitnessRepository
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
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.sqrt

class MotionDetector(
    private val context: Context,
    private val scope: CoroutineScope,
    private val fitnessRepo: FitnessRepository
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val stepDetector = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
    private val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)

    private val _motionState = MutableStateFlow(STOPPED)
    val motionState: StateFlow<MotionState> = _motionState.asStateFlow()

    private val _currentSpeed = MutableStateFlow(0f)
    val currentSpeed: StateFlow<Float> = _currentSpeed.asStateFlow()
    
    private val _stepCadence = MutableStateFlow(0)
    val stepCadence: StateFlow<Int> = _stepCadence.asStateFlow()
    
    private val stepTimestamps = mutableListOf<Long>()

    private var motionSettings = MotionSettings()
    private var isDetecting = false

    private var lastAcceleration = 0f
    private var lastGeoSpeed = 0f
    private var lastGeoSpeedTimestamp = 0L
    private var lastStepDetectionTimestamp = 0L
    private var isAccelerometerStepDetectorActive = false

    // Simple peak detection for accelerometer steps
    private var lastPeakMagnitude = 0f
    private var isSearchingForPeak = true
    private val STEP_MAGNITUDE_THRESHOLD = 2.2f // Adjust based on sensitivity
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
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        // Step detector
        stepDetector?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }

        // GPS - Optimized for battery (3s-5s)
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .setMinUpdateIntervalMillis(3000)
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
        _stepCadence.value = 0
        stepTimestamps.clear()
        lastAcceleration = 0f
        lastGeoSpeed = 0f
        lastGeoSpeedTimestamp = 0L
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (it.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                val x = it.values[0]
                val y = it.values[1]
                val z = it.values[2]

                val magnitude = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
                val netAcceleration = abs(magnitude - 9.81f)

                // 1. Fallback Step Detection logic (if hardware detector is silent)
                detectStepFromAccelerometer(netAcceleration)

                // 2. Smoothing for general motion Evaluation
                accelBuffer[bufferIndex] = netAcceleration
                bufferIndex = (bufferIndex + 1) % accelBuffer.size
                lastAcceleration = accelBuffer.average().toFloat()

                evaluateMotion()
            } else if (it.sensor.type == Sensor.TYPE_STEP_DETECTOR) {
                // If hardware detector works, we prefer it
                isAccelerometerStepDetectorActive = false
                registerStep()
            }
        }
    }

    private fun detectStepFromAccelerometer(magnitude: Float) {
        val factor = (11 - motionSettings.sensitivity) / 5f
        val threshold = STEP_MAGNITUDE_THRESHOLD * factor

        if (isSearchingForPeak) {
            if (magnitude > threshold) {
                isSearchingForPeak = false
                lastPeakMagnitude = magnitude
            }
        } else {
            if (magnitude < threshold * 0.8f) {
                // We found a peak and now magnitude dropped, count as step
                isSearchingForPeak = true
                
                // Debounce steps (max 5 per second)
                val now = System.currentTimeMillis()
                if (now - lastStepDetectionTimestamp > 200) {
                    isAccelerometerStepDetectorActive = true
                    registerStep()
                    lastStepDetectionTimestamp = now
                }
            } else if (magnitude > lastPeakMagnitude) {
                lastPeakMagnitude = magnitude
            }
        }
    }

    private fun registerStep() {
        fitnessRepo.addStep()
        
        val now = System.currentTimeMillis()
        stepTimestamps.add(now)
        // keep only last 5 seconds to calculate rolling cadence
        stepTimestamps.removeAll { t -> now - t > 5000 }
        
        if (stepTimestamps.size >= 2) {
            val durationMs = stepTimestamps.last() - stepTimestamps.first()
            if (durationMs > 0) {
                val cadence = ((stepTimestamps.size - 1) * 60000L / durationMs).toInt()
                scope.launch(Dispatchers.Main) { _stepCadence.value = cadence }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { location ->
                if (location.hasSpeed()) {
                    lastGeoSpeed = location.speed
                    lastGeoSpeedTimestamp = System.currentTimeMillis()
                    evaluateMotion()
                }
            }
        }
    }

    private fun evaluateMotion() {
        if (!isDetecting) return

        scope.launch(Dispatchers.Default) {
            val factor = (11 - motionSettings.sensitivity) / 5f
            val walkThreshold = motionSettings.walkingThreshold * factor
            val runThreshold = motionSettings.runningThreshold * factor

            val isGpsValid = (System.currentTimeMillis() - lastGeoSpeedTimestamp) < 10000
            val currentCadence = _stepCadence.value
            
            // Cadence to speed estimation: 120 steps/min ~ 1.6 m/s (walking/jogging)
            // If we have active steps, ensure a minimum speed to trigger WALKING
            val fakeSpeedFromCadence = if (currentCadence > 0) (currentCadence * 0.015f) else 0f
            
            val effectiveSpeed = if (isGpsValid && lastGeoSpeed > 0.5f) {
                lastGeoSpeed 
            } else {
                fakeSpeedFromCadence
            }

            val newState = when {
                effectiveSpeed >= runThreshold || currentCadence > 150 -> RUNNING
                effectiveSpeed >= walkThreshold || currentCadence > 50 -> WALKING
                else -> STOPPED
            }

            withContext(Dispatchers.Main) {
                if (newState == STOPPED && _motionState.value != STOPPED) {
                    stepTimestamps.clear()
                    _stepCadence.value = 0
                }
                _motionState.value = newState
                _currentSpeed.value = effectiveSpeed
            }
        }
    }
}
