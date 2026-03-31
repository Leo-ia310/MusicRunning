package com.example.chillmusic.logic.sensor

import com.example.chillmusic.data.model.MotionState

data class MotionComputation(
    val state: MotionState,
    val effectiveSpeed: Float
)

object MotionStateCalculator {

    fun compute(
        sensitivity: Int,
        walkingThreshold: Float,
        runningThreshold: Float,
        cadence: Int,
        gpsSpeed: Float,
        lastGpsTimestamp: Long,
        now: Long
    ): MotionComputation {
        val factor = (11 - sensitivity.coerceIn(1, 10)) / 5f
        val adjustedWalkingThreshold = walkingThreshold * factor
        val adjustedRunningThreshold = runningThreshold * factor
        val isGpsValid = (now - lastGpsTimestamp) < 10_000L
        val cadenceSpeed = if (cadence > 0) cadence * 0.015f else 0f
        val effectiveSpeed = if (isGpsValid && gpsSpeed > 0.5f) gpsSpeed else cadenceSpeed

        val state = when {
            effectiveSpeed >= adjustedRunningThreshold || cadence > 150 -> MotionState.RUNNING
            effectiveSpeed >= adjustedWalkingThreshold || cadence > 50 -> MotionState.WALKING
            else -> MotionState.STOPPED
        }

        return MotionComputation(state = state, effectiveSpeed = effectiveSpeed)
    }
}

object StepSourcePolicy {

    fun shouldUseAccelerometerFallback(
        stepDetectorAvailable: Boolean,
        detectionStartTimestamp: Long,
        lastHardwareStepTimestamp: Long,
        now: Long,
        fallbackSilenceMillis: Long = 5_000L
    ): Boolean {
        if (!stepDetectorAvailable) return true
        val referenceTimestamp = if (lastHardwareStepTimestamp > 0L) {
            lastHardwareStepTimestamp
        } else {
            detectionStartTimestamp
        }
        return (now - referenceTimestamp) >= fallbackSilenceMillis
    }
}
