package com.example.chillmusic.ui

import android.Manifest

data class MotionPermissionStatus(
    val motionGranted: Boolean,
    val locationGranted: Boolean
) {
    val allGranted: Boolean
        get() = motionGranted && locationGranted
}

object MotionPermissionPolicy {

    fun resolve(
        grants: Map<String, Boolean>,
        sdkInt: Int
    ): MotionPermissionStatus {
        val motionGranted = sdkInt < 29 || grants[Manifest.permission.ACTIVITY_RECOGNITION] == true
        val locationGranted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        return MotionPermissionStatus(
            motionGranted = motionGranted,
            locationGranted = locationGranted
        )
    }
}
