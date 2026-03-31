package com.example.chillmusic.ui

import android.Manifest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionPermissionPolicyTest {

    @Test
    fun requires_activity_recognition_on_android10_and_higher() {
        val status = MotionPermissionPolicy.resolve(
            grants = mapOf(
                Manifest.permission.ACCESS_FINE_LOCATION to true,
                Manifest.permission.ACTIVITY_RECOGNITION to false
            ),
            sdkInt = 29
        )

        assertFalse(status.motionGranted)
        assertFalse(status.allGranted)
    }

    @Test
    fun allows_motion_without_activity_permission_before_android10() {
        val status = MotionPermissionPolicy.resolve(
            grants = mapOf(Manifest.permission.ACCESS_COARSE_LOCATION to true),
            sdkInt = 28
        )

        assertTrue(status.motionGranted)
        assertTrue(status.locationGranted)
        assertTrue(status.allGranted)
    }

    @Test
    fun requires_some_location_permission() {
        val status = MotionPermissionPolicy.resolve(
            grants = mapOf(Manifest.permission.ACTIVITY_RECOGNITION to true),
            sdkInt = 34
        )

        assertTrue(status.motionGranted)
        assertFalse(status.locationGranted)
        assertFalse(status.allGranted)
    }
}
