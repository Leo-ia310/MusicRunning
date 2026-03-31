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
import com.example.chillmusic.data.model.MotionState.STOPPED
import com.example.chillmusic.data.repository.FitnessRepository
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs
import kotlin.math.sqrt

class MotionDetector(
    context: Context,
    private val fitnessRepo: FitnessRepository
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val stepDetector = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val _motionState = MutableStateFlow(MotionState.STOPPED)
    val motionState: StateFlow<MotionState> = _motionState.asStateFlow()

    private val _currentSpeed = MutableStateFlow(0f)
    val currentSpeed: StateFlow<Float> = _currentSpeed.asStateFlow()

    private val _stepCadence = MutableStateFlow(0)
    val stepCadence: StateFlow<Int> = _stepCadence.asStateFlow()

    private val stepTimestamps = mutableListOf<Long>()

    private var motionSettings = MotionSettings()
    private var isDetecting = false
    private var lastGeoSpeed = 0f
    private var lastGeoSpeedTimestamp = 0L
    private var lastStepDetectionTimestamp = 0L
    private var detectionStartTimestamp = 0L
    private var lastHardwareStepTimestamp = 0L
    private var lastPeakMagnitude = 0f
    private var isSearchingForPeak = true
    private val stepMagnitudeThreshold = 2.2f

    fun updateSettings(settings: MotionSettings) {
        motionSettings = settings
        evaluateMotion()
    }

    @SuppressLint("MissingPermission")
    fun startDetection(): Boolean {
        if (isDetecting) return true
        isDetecting = true
        detectionStartTimestamp = System.currentTimeMillis()

        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        stepDetector?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .setMinUpdateIntervalMillis(3000)
            .build()

        return try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
            true
        } catch (_: SecurityException) {
            stopDetection()
            false
        }
    }

    fun stopDetection() {
        if (!isDetecting) return
        isDetecting = false
        sensorManager.unregisterListener(this)
        fusedLocationClient.removeLocationUpdates(locationCallback)

        _motionState.value = MotionState.STOPPED
        _currentSpeed.value = 0f
        _stepCadence.value = 0
        stepTimestamps.clear()
        lastGeoSpeed = 0f
        lastGeoSpeedTimestamp = 0L
        lastStepDetectionTimestamp = 0L
        detectionStartTimestamp = 0L
        lastHardwareStepTimestamp = 0L
        lastPeakMagnitude = 0f
        isSearchingForPeak = true
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return

        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                val magnitude = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
                val netAcceleration = abs(magnitude - 9.81f)

                if (StepSourcePolicy.shouldUseAccelerometerFallback(
                        stepDetectorAvailable = stepDetector != null,
                        detectionStartTimestamp = detectionStartTimestamp,
                        lastHardwareStepTimestamp = lastHardwareStepTimestamp,
                        now = System.currentTimeMillis()
                    )
                ) {
                    detectStepFromAccelerometer(netAcceleration)
                }

                evaluateMotion()
            }

            Sensor.TYPE_STEP_DETECTOR -> {
                lastHardwareStepTimestamp = System.currentTimeMillis()
                registerStep()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun detectStepFromAccelerometer(magnitude: Float) {
        val factor = (11 - motionSettings.sensitivity) / 5f
        val threshold = stepMagnitudeThreshold * factor

        if (isSearchingForPeak) {
            if (magnitude > threshold) {
                isSearchingForPeak = false
                lastPeakMagnitude = magnitude
            }
            return
        }

        if (magnitude < threshold * 0.8f) {
            isSearchingForPeak = true
            val now = System.currentTimeMillis()
            if (now - lastStepDetectionTimestamp > 200) {
                registerStep()
                lastStepDetectionTimestamp = now
            }
        } else if (magnitude > lastPeakMagnitude) {
            lastPeakMagnitude = magnitude
        }
    }

    private fun registerStep() {
        fitnessRepo.addStep()

        val now = System.currentTimeMillis()
        stepTimestamps.add(now)
        lastStepDetectionTimestamp = now
        pruneOldSteps(now)

        if (stepTimestamps.size >= 2) {
            val durationMs = stepTimestamps.last() - stepTimestamps.first()
            if (durationMs > 0) {
                _stepCadence.value = ((stepTimestamps.size - 1) * 60000L / durationMs).toInt()
            }
        }

        evaluateMotion()
    }

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

        val now = System.currentTimeMillis()
        pruneOldSteps(now)

        if (stepTimestamps.size < 2 && (now - lastStepDetectionTimestamp) > 5_000L) {
            _stepCadence.value = 0
        }

        val computation = MotionStateCalculator.compute(
            sensitivity = motionSettings.sensitivity,
            walkingThreshold = motionSettings.walkingThreshold,
            runningThreshold = motionSettings.runningThreshold,
            cadence = _stepCadence.value,
            gpsSpeed = lastGeoSpeed,
            lastGpsTimestamp = lastGeoSpeedTimestamp,
            now = now
        )

        if (computation.state == STOPPED && _motionState.value != STOPPED) {
            stepTimestamps.clear()
            _stepCadence.value = 0
        }

        _motionState.value = computation.state
        _currentSpeed.value = computation.effectiveSpeed
    }

    private fun pruneOldSteps(now: Long) {
        stepTimestamps.removeAll { timestamp -> now - timestamp > 5_000L }
    }
}
