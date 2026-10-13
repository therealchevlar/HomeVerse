package com.example.engine3d

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

/**
 * 4x4 Matrix for 3D transformations, model-view-projection pipelines.
 * Internal array is column-major: index = col * 4 + row.
 */
class Matrix4x4(
    val m: FloatArray = FloatArray(16) { i -> if (i % 5 == 0) 1f else 0f }
) {
    operator fun get(row: Int, col: Int): Float = m[col * 4 + row]
    operator fun set(row: Int, col: Int, value: Float) {
        m[col * 4 + row] = value
    }

    operator fun times(other: Matrix4x4): Matrix4x4 {
        val result = FloatArray(16)
        for (col in 0..3) {
            for (row in 0..3) {
                var sum = 0f
                for (k in 0..3) {
                    sum += this[row, k] * other[k, col]
                }
                result[col * 4 + row] = sum
            }
        }
        return Matrix4x4(result)
    }

    fun transformPoint(v: Vector3): Vector3 {
        val x = v.x * m[0] + v.y * m[4] + v.z * m[8] + m[12]
        val y = v.x * m[1] + v.y * m[5] + v.z * m[9] + m[13]
        val z = v.x * m[2] + v.y * m[6] + v.z * m[10] + m[14]
        val w = v.x * m[3] + v.y * m[7] + v.z * m[11] + m[15]

        return if (w != 0f && w != 1f) {
            Vector3(x / w, y / w, z / w)
        } else {
            Vector3(x, y, z)
        }
    }

    fun transformDirection(v: Vector3): Vector3 {
        val x = v.x * m[0] + v.y * m[4] + v.z * m[8]
        val y = v.x * m[1] + v.y * m[5] + v.z * m[9]
        val z = v.x * m[2] + v.y * m[6] + v.z * m[10]
        return Vector3(x, y, z).normalize()
    }

    companion object {
        fun identity(): Matrix4x4 = Matrix4x4()

        fun translation(tx: Float, ty: Float, tz: Float): Matrix4x4 {
            val mat = Matrix4x4()
            mat[0, 3] = tx
            mat[1, 3] = ty
            mat[2, 3] = tz
            return mat
        }

        fun scale(sx: Float, sy: Float, sz: Float): Matrix4x4 {
            val mat = Matrix4x4()
            mat[0, 0] = sx
            mat[1, 1] = sy
            mat[2, 2] = sz
            return mat
        }

        fun rotationX(rad: Float): Matrix4x4 {
            val mat = Matrix4x4()
            val c = cos(rad)
            val s = sin(rad)
            mat[1, 1] = c
            mat[1, 2] = -s
            mat[2, 1] = s
            mat[2, 2] = c
            return mat
        }

        fun rotationY(rad: Float): Matrix4x4 {
            val mat = Matrix4x4()
            val c = cos(rad)
            val s = sin(rad)
            mat[0, 0] = c
            mat[0, 2] = s
            mat[2, 0] = -s
            mat[2, 2] = c
            return mat
        }

        fun rotationZ(rad: Float): Matrix4x4 {
            val mat = Matrix4x4()
            val c = cos(rad)
            val s = sin(rad)
            mat[0, 0] = c
            mat[0, 1] = -s
            mat[1, 0] = s
            mat[1, 1] = c
            return mat
        }

        fun lookAt(eye: Vector3, target: Vector3, up: Vector3): Matrix4x4 {
            val zAxis = (eye - target).normalize() // camera looks down -Z
            val xAxis = up.cross(zAxis).normalize()
            val yAxis = zAxis.cross(xAxis)

            val mat = Matrix4x4()
            mat[0, 0] = xAxis.x
            mat[0, 1] = xAxis.y
            mat[0, 2] = xAxis.z
            mat[0, 3] = -xAxis.dot(eye)

            mat[1, 0] = yAxis.x
            mat[1, 1] = yAxis.y
            mat[1, 2] = yAxis.z
            mat[1, 3] = -yAxis.dot(eye)

            mat[2, 0] = zAxis.x
            mat[2, 1] = zAxis.y
            mat[2, 2] = zAxis.z
            mat[2, 3] = -zAxis.dot(eye)

            mat[3, 0] = 0f
            mat[3, 1] = 0f
            mat[3, 2] = 0f
            mat[3, 3] = 1f
            return mat
        }

        fun perspective(fovYRadians: Float, aspect: Float, near: Float, far: Float): Matrix4x4 {
            val mat = Matrix4x4()
            val tanHalfFov = tan(fovYRadians / 2f)
            mat[0, 0] = 1f / (aspect * tanHalfFov)
            mat[1, 1] = 1f / tanHalfFov
            mat[2, 2] = -(far + near) / (far - near)
            mat[2, 3] = -(2f * far * near) / (far - near)
            mat[3, 2] = -1f
            mat[3, 3] = 0f
            return mat
        }
    }
}
