package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "homes")
data class HomeEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val createdAt: Long = System.currentTimeMillis(),
    val coverImagePath: String = "",
    val totalFloors: Int = 1,
    val totalRooms: Int = 0,
    val totalAreaSqM: Float = 0f,
    val isSample: Boolean = false
)

@Entity(tableName = "floors")
data class FloorEntity(
    @PrimaryKey val id: String,
    val homeId: String,
    val floorNumber: Int,
    val name: String,
    val levelElevationMeters: Float = 0f
)

enum class RoomType(val displayName: String) {
    LIVING_ROOM("Living Room"),
    KITCHEN("Kitchen & Dining"),
    MASTER_BEDROOM("Master Suite"),
    BEDROOM("Bedroom"),
    BATHROOM("Bathroom"),
    OFFICE("Home Office"),
    BALCONY("Balcony Terrace"),
    HALLWAY("Foyer / Hallway"),
    CUSTOM("Custom Space")
}

enum class ReconstructionStatus {
    NOT_STARTED,
    CAPTURING,
    PROCESSING,
    READY,
    FAILED
}

@Entity(tableName = "rooms")
data class RoomEntity(
    @PrimaryKey val id: String,
    val floorId: String,
    val homeId: String,
    val name: String,
    val roomType: RoomType,
    val widthMeters: Float = 4.5f,
    val lengthMeters: Float = 4.0f,
    val heightMeters: Float = 2.8f,
    val offsetX: Float = 0f,
    val offsetZ: Float = 0f,
    val colorHex: String = "#00F0FF",
    val status: ReconstructionStatus = ReconstructionStatus.READY,
    val notes: String = ""
)

@Entity(tableName = "capture_sessions")
data class CaptureSessionEntity(
    @PrimaryKey val id: String,
    val roomId: String,
    val homeId: String,
    val frameCount: Int = 0,
    val startedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val packagePath: String = "",
    val status: String = "COMPLETED"
)

enum class PipelineStage(val stageName: String, val order: Int) {
    VALIDATE_CAPTURE("Validating Capture Integrity", 1),
    POSE_ESTIMATION("Estimating 6DoF Camera Poses", 2),
    FEATURE_MATCHING("Feature Point Triangulation", 3),
    POINT_CLOUD_DENSIFICATION("Densifying Spatial Point Cloud", 4),
    SURFACE_RECONSTRUCTION("Reconstructing Polygonal Mesh", 5),
    ASSET_PUBLISHING("Publishing Digital Twin Manifest", 6),
    COMPLETED("Ready to Explore", 7),
    FAILED("Reconstruction Failed", -1)
}

@Entity(tableName = "reconstruction_jobs")
data class ReconstructionJobEntity(
    @PrimaryKey val id: String,
    val captureSessionId: String,
    val roomId: String,
    val homeId: String,
    val currentStage: PipelineStage = PipelineStage.VALIDATE_CAPTURE,
    val progressPercent: Int = 0,
    val status: String = "IN_PROGRESS",
    val logOutput: String = "",
    val assetManifestPath: String = "",
    val startedAt: Long = System.currentTimeMillis(),
    val finishedAt: Long? = null
)

data class WaypointNode(
    val id: String,
    val roomId: String,
    val name: String,
    val x: Float,
    val y: Float,
    val z: Float,
    val description: String,
    val focalHeadingYaw: Float = 0f
)
