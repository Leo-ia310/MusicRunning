package com.example.chillmusic.logic.sensor

import com.example.chillmusic.data.model.MotionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionMathTest {

    @Test
    fun calculator_uses_cadence_when_gps_is_stale() {
        val result = MotionStateCalculator.compute(
            sensitivity = 5,
            walkingThreshold = 1.5f,
            runningThreshold = 4.0f,
            cadence = 140,
            gpsSpeed = 0.1f,
            lastGpsTimestamp = 0L,
            now = 20_000L
        )

        assertEquals(MotionState.WALKING, result.state)
        assertTrue(result.effectiveSpeed > 0f)
    }

    @Test
    fun calculator_marks_running_for_high_speed() {
        val result = MotionStateCalculator.compute(
            sensitivity = 5,
            walkingThreshold = 1.5f,
            runningThreshold = 4.0f,
            cadence = 170,
            gpsSpeed = 4.8f,
            lastGpsTimestamp = 9_000L,
            now = 10_000L
        )

        assertEquals(MotionState.RUNNING, result.state)
    }

    @Test
    fun fallback_waits_when_hardware_detector_is_recent() {
        val shouldUseFallback = StepSourcePolicy.shouldUseAccelerometerFallback(
            stepDetectorAvailable = true,
            detectionStartTimestamp = 1_000L,
            lastHardwareStepTimestamp = 4_500L,
            now = 6_000L
        )

        assertFalse(shouldUseFallback)
    }

    @Test
    fun fallback_is_enabled_when_no_hardware_detector_exists() {
        val shouldUseFallback = StepSourcePolicy.shouldUseAccelerometerFallback(
            stepDetectorAvailable = false,
            detectionStartTimestamp = 0L,
            lastHardwareStepTimestamp = 0L,
            now = 1_000L
        )

        assertTrue(shouldUseFallback)
    }
}
