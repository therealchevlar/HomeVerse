package com.example.engine3d

import androidx.compose.ui.graphics.Color

data class Face3D(
    val v0: Int,
    val v1: Int,
    val v2: Int,
    val normal: Vector3,
    val color: Color,
    val isCeiling: Boolean = false,
    val roomId: String = ""
)

data class Point3D(
    val position: Vector3,
    val color: Color,
    val size: Float = 3f
)

data class BoundingBox(
    val min: Vector3,
    val max: Vector3
) {
    fun contains(p: Vector3): Boolean {
        return p.x in min.x..max.x &&
                p.y in min.y..max.y &&
                p.z in min.z..max.z
    }
}

/**
 * Geometric Mesh representation holding 3D vertices, polygon faces,
 * sparse reconstructed point cloud, and spatial bounding volume.
 */
data class Mesh3D(
    val name: String,
    val vertices: List<Vector3>,
    val faces: List<Face3D>,
    val pointCloud: List<Point3D> = emptyList(),
    val bounds: BoundingBox = BoundingBox(Vector3.ZERO, Vector3.ZERO)
) {
    companion object {
        val EMPTY = Mesh3D("Empty", emptyList(), emptyList())
    }
}
