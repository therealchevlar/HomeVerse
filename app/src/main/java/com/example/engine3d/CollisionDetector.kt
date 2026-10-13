package com.example.engine3d

/**
 * Spatial collision and navigation boundaries for First-Person Explore mode.
 */
object CollisionDetector {

    /**
     * Resolves movement from currentPos to desiredPos within house boundaries.
     * Prevents walking outside the house perimeter while permitting movement through interior doors.
     */
    fun clampToHousePerimeter(
        currentPos: Vector3,
        desiredPos: Vector3,
        bounds: BoundingBox,
        padding: Float = 0.4f
    ): Vector3 {
        val minX = bounds.min.x + padding
        val maxX = bounds.max.x - padding
        val minZ = bounds.min.z + padding
        val maxZ = bounds.max.z - padding

        val clampedX = desiredPos.x.coerceIn(minX, maxX)
        val clampedZ = desiredPos.z.coerceIn(minZ, maxZ)
        // Keep eye level fixed at 1.6m above floor
        return Vector3(clampedX, 1.6f, clampedZ)
    }

    /**
     * Finds which room currently contains the player's 3D position.
     */
    fun findCurrentRoomId(pos: Vector3, roomBounds: Map<String, BoundingBox>): String? {
        for ((roomId, bbox) in roomBounds) {
            if (bbox.contains(pos)) {
                return roomId
            }
        }
        return null
    }
}
