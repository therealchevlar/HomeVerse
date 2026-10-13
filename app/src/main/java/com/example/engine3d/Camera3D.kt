package com.example.engine3d

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class CameraMode {
    ORBIT,
    FIRST_PERSON,
    WAYPOINT
}

/**
 * 3D Camera supporting Orbit (Digital Twin), First-Person (Explore), and Waypoint (Tour).
 */
class Camera3D(
    var mode: CameraMode = CameraMode.ORBIT,
    var position: Vector3 = Vector3(0f, 6f, 12f),
    var target: Vector3 = Vector3(0f, 1f, 0f),
    var up: Vector3 = Vector3(0f, 1f, 0f),

    // Orbit parameters
    var orbitYaw: Float = 0.6f,    // radians
    var orbitPitch: Float = 0.55f, // radians
    var orbitDistance: Float = 14f,

    // First person parameters
    var fpYaw: Float = 0f,         // radians
    var fpPitch: Float = 0f,       // radians

    var fovRadians: Float = (60.0 * PI / 180.0).toFloat(),
    var near: Float = 0.2f,
    var far: Float = 100f
) {
    fun updateOrbit() {
        // Clamp pitch to avoid gimbal flipping
        val clampedPitch = orbitPitch.coerceIn(0.1f, 1.45f)
        val hDist = orbitDistance * cos(clampedPitch)
        val y = target.y + orbitDistance * sin(clampedPitch)
        val x = target.x + hDist * sin(orbitYaw)
        val z = target.z + hDist * cos(orbitYaw)
        position = Vector3(x, y, z)
    }

    fun getViewMatrix(): Matrix4x4 {
        return when (mode) {
            CameraMode.ORBIT -> {
                updateOrbit()
                Matrix4x4.lookAt(position, target, up)
            }
            CameraMode.FIRST_PERSON, CameraMode.WAYPOINT -> {
                val forward = Vector3(
                    sin(fpYaw) * cos(fpPitch),
                    sin(fpPitch),
                    cos(fpYaw) * cos(fpPitch)
                ).normalize()
                val lookTarget = position + forward
                Matrix4x4.lookAt(position, lookTarget, up)
            }
        }
    }

    fun getProjectionMatrix(aspect: Float): Matrix4x4 {
        return Matrix4x4.perspective(fovRadians, aspect, near, far)
    }
}
