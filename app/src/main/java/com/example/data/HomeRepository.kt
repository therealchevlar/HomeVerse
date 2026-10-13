package com.example.data

import androidx.compose.ui.graphics.Color
import com.example.data.model.CaptureSessionEntity
import com.example.data.model.FloorEntity
import com.example.data.model.HomeEntity
import com.example.data.model.ReconstructionJobEntity
import com.example.data.model.ReconstructionStatus
import com.example.data.model.RoomEntity
import com.example.data.model.RoomType
import com.example.data.model.WaypointNode
import com.example.engine3d.Mesh3D
import com.example.engine3d.SpatialPrimitives
import com.example.engine3d.Vector3
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.UUID

class HomeRepository(private val db: AppDatabase) {

    val allHomes: Flow<List<HomeEntity>> = db.homeDao().getAllHomes()

    fun getFloorsForHome(homeId: String): Flow<List<FloorEntity>> =
        db.floorDao().getFloorsForHome(homeId)

    fun getRoomsForHome(homeId: String): Flow<List<RoomEntity>> =
        db.roomDao().getRoomsForHome(homeId)

    fun getJobsForHome(homeId: String): Flow<List<ReconstructionJobEntity>> =
        db.reconstructionJobDao().getJobsForHome(homeId)

    suspend fun getHomeById(id: String): HomeEntity? = db.homeDao().getHomeById(id)

    suspend fun getRoomById(id: String): RoomEntity? = db.roomDao().getRoomById(id)

    suspend fun insertHome(home: HomeEntity) = db.homeDao().insertHome(home)

    suspend fun insertFloor(floor: FloorEntity) = db.floorDao().insertFloor(floor)

    suspend fun insertRoom(room: RoomEntity) = db.roomDao().insertRoom(room)

    suspend fun updateRoom(room: RoomEntity) = db.roomDao().updateRoom(room)

    suspend fun deleteRoom(roomId: String) = db.roomDao().deleteRoom(roomId)

    suspend fun insertJob(job: ReconstructionJobEntity) = db.reconstructionJobDao().insertJob(job)

    suspend fun updateJob(job: ReconstructionJobEntity) = db.reconstructionJobDao().updateJob(job)

    suspend fun insertCaptureSession(session: CaptureSessionEntity) =
        db.captureDao().insertSession(session)

    suspend fun deleteHome(homeId: String) {
        db.roomDao().deleteRoomsForHome(homeId)
        db.floorDao().deleteFloorsForHome(homeId)
        db.homeDao().deleteHome(homeId)
    }

    /**
     * Seeds "Villa Lumina" showcase project if no homes exist yet.
     */
    suspend fun seedSampleHomeIfNeeded() {
        val existing = db.homeDao().getAllHomes().first()
        if (existing.isNotEmpty()) return

        val sampleHomeId = "villa-lumina-sample"
        val floor1Id = "fl-ground-$sampleHomeId"

        val sampleHome = HomeEntity(
            id = sampleHomeId,
            name = "Villa Lumina",
            description = "Modernist pavilion residence with open-concept living, glass atrium, and scenic terrace.",
            createdAt = System.currentTimeMillis(),
            totalFloors = 1,
            totalRooms = 4,
            totalAreaSqM = 118.5f,
            isSample = true
        )
        db.homeDao().insertHome(sampleHome)

        val groundFloor = FloorEntity(
            id = floor1Id,
            homeId = sampleHomeId,
            floorNumber = 1,
            name = "Ground Floor",
            levelElevationMeters = 0f
        )
        db.floorDao().insertFloor(groundFloor)

        // 4 Architectural Rooms:
        // 1. Living Atrium (0, 0) - 5.5 x 5.0m
        val livingRoom = RoomEntity(
            id = "rm-living-$sampleHomeId",
            floorId = floor1Id,
            homeId = sampleHomeId,
            name = "Grand Living Atrium",
            roomType = RoomType.LIVING_ROOM,
            widthMeters = 5.5f,
            lengthMeters = 5.0f,
            heightMeters = 3.2f,
            offsetX = -2.75f,
            offsetZ = -2.5f,
            colorHex = "#00F0FF",
            status = ReconstructionStatus.READY,
            notes = "Reconstructed with 48 registered keyframes and dense depth mapping."
        )

        // 2. Kitchen & Dining (-2.75, 2.5) - 4.5 x 4.0m
        val kitchen = RoomEntity(
            id = "rm-kitchen-$sampleHomeId",
            floorId = floor1Id,
            homeId = sampleHomeId,
            name = "Chef's Kitchen & Dining",
            roomType = RoomType.KITCHEN,
            widthMeters = 4.5f,
            lengthMeters = 4.0f,
            heightMeters = 2.8f,
            offsetX = -2.75f,
            offsetZ = 2.5f,
            colorHex = "#10B981",
            status = ReconstructionStatus.READY,
            notes = "Marble waterfall counter and integrated minimalist cabinetry."
        )

        // 3. Master Suite (2.75, -2.5) - 4.8 x 4.5m
        val masterSuite = RoomEntity(
            id = "rm-master-$sampleHomeId",
            floorId = floor1Id,
            homeId = sampleHomeId,
            name = "Master Sanctuarium",
            roomType = RoomType.MASTER_BEDROOM,
            widthMeters = 4.8f,
            lengthMeters = 4.5f,
            heightMeters = 2.9f,
            offsetX = 2.75f,
            offsetZ = -2.5f,
            colorHex = "#8B5CF6",
            status = ReconstructionStatus.READY,
            notes = "King-size floating platform with integrated reading luminescence."
        )

        // 4. Balcony Terrace (1.75, 2.5) - 4.0 x 3.0m
        val balcony = RoomEntity(
            id = "rm-terrace-$sampleHomeId",
            floorId = floor1Id,
            homeId = sampleHomeId,
            name = "Panoramic Skyline Terrace",
            roomType = RoomType.BALCONY,
            widthMeters = 4.0f,
            lengthMeters = 3.0f,
            heightMeters = 1.2f,
            offsetX = 1.75f,
            offsetZ = 2.5f,
            colorHex = "#F59E0B",
            status = ReconstructionStatus.READY,
            notes = "Glass perimeter balustrade overlooking city skyline."
        )

        db.roomDao().insertRoom(livingRoom)
        db.roomDao().insertRoom(kitchen)
        db.roomDao().insertRoom(masterSuite)
        db.roomDao().insertRoom(balcony)
    }

    /**
     * Builds the complete 3D digital twin mesh for a home and its rooms.
     */
    fun buildHome3DMesh(home: HomeEntity, rooms: List<RoomEntity>): Mesh3D {
        val subMeshes = mutableListOf<Mesh3D>()

        for (room in rooms) {
            val wallColor = when (room.roomType) {
                RoomType.LIVING_ROOM -> Color(0xFF16253B)
                RoomType.KITCHEN -> Color(0xFF16342D)
                RoomType.MASTER_BEDROOM -> Color(0xFF261D3B)
                RoomType.BALCONY -> Color(0xFF332B1A)
                else -> Color(0xFF1E293B)
            }
            val floorColor = when (room.roomType) {
                RoomType.LIVING_ROOM -> Color(0xFF0F172A)
                RoomType.KITCHEN -> Color(0xFF0B1F1A)
                RoomType.MASTER_BEDROOM -> Color(0xFF1A1329)
                RoomType.BALCONY -> Color(0xFF241D12)
                else -> Color(0xFF111827)
            }

            // Room shell with doorway
            val roomShell = SpatialPrimitives.createRoomMesh(
                roomId = room.id,
                name = room.name,
                originX = room.offsetX,
                originZ = room.offsetZ,
                width = room.widthMeters,
                length = room.lengthMeters,
                height = room.heightMeters,
                wallColor = wallColor,
                floorColor = floorColor,
                hasDoorwayOnSouth = true
            )
            subMeshes.add(roomShell)

            // Architectural furniture based on room type
            val centerX = room.offsetX + room.widthMeters / 2f
            val centerZ = room.offsetZ + room.lengthMeters / 2f

            when (room.roomType) {
                RoomType.LIVING_ROOM -> {
                    // Sofa
                    subMeshes.add(
                        SpatialPrimitives.createBox(
                            "Sofa", room.id,
                            centerX, 0.4f, centerZ + 1.0f,
                            2.4f, 0.6f, 1.0f,
                            Color(0xFF2563EB)
                        )
                    )
                    // Coffee table
                    subMeshes.add(
                        SpatialPrimitives.createBox(
                            "Coffee Table", room.id,
                            centerX, 0.25f, centerZ - 0.2f,
                            1.2f, 0.4f, 0.7f,
                            Color(0xFF00F0FF)
                        )
                    )
                    // Media console
                    subMeshes.add(
                        SpatialPrimitives.createBox(
                            "Media Console", room.id,
                            centerX, 0.35f, room.offsetZ + 0.3f,
                            2.2f, 0.5f, 0.45f,
                            Color(0xFF334155)
                        )
                    )
                }
                RoomType.KITCHEN -> {
                    // Kitchen Island
                    subMeshes.add(
                        SpatialPrimitives.createBox(
                            "Kitchen Island", room.id,
                            centerX, 0.5f, centerZ,
                            2.2f, 0.9f, 1.1f,
                            Color(0xFF10B981)
                        )
                    )
                    // Dining Table
                    subMeshes.add(
                        SpatialPrimitives.createBox(
                            "Dining Table", room.id,
                            centerX, 0.4f, room.offsetZ + room.lengthMeters - 0.9f,
                            1.8f, 0.75f, 0.9f,
                            Color(0xFF047857)
                        )
                    )
                }
                RoomType.MASTER_BEDROOM -> {
                    // King Bed
                    subMeshes.add(
                        SpatialPrimitives.createBox(
                            "King Bed", room.id,
                            centerX, 0.35f, centerZ + 0.5f,
                            2.1f, 0.65f, 2.3f,
                            Color(0xFF8B5CF6)
                        )
                    )
                    // Nightstand Left
                    subMeshes.add(
                        SpatialPrimitives.createBox(
                            "Nightstand Left", room.id,
                            centerX - 1.4f, 0.3f, centerZ + 1.2f,
                            0.5f, 0.55f, 0.5f,
                            Color(0xFF4C1D95)
                        )
                    )
                    // Nightstand Right
                    subMeshes.add(
                        SpatialPrimitives.createBox(
                            "Nightstand Right", room.id,
                            centerX + 1.4f, 0.3f, centerZ + 1.2f,
                            0.5f, 0.55f, 0.5f,
                            Color(0xFF4C1D95)
                        )
                    )
                }
                RoomType.BALCONY -> {
                    // Lounge chair
                    subMeshes.add(
                        SpatialPrimitives.createBox(
                            "Lounge Chair", room.id,
                            centerX - 0.8f, 0.25f, centerZ,
                            0.8f, 0.45f, 1.6f,
                            Color(0xFFD97706)
                        )
                    )
                }
                else -> {}
            }
        }

        return SpatialPrimitives.combineMeshes(home.name, subMeshes)
    }

    /**
     * Builds guided tour waypoint nodes across the rooms of a home.
     */
    fun buildTourWaypoints(rooms: List<RoomEntity>): List<WaypointNode> {
        val list = mutableListOf<WaypointNode>()
        for (room in rooms) {
            val cx = room.offsetX + room.widthMeters / 2f
            val cz = room.offsetZ + room.lengthMeters / 2f
            list.add(
                WaypointNode(
                    id = "wp-${room.id}",
                    roomId = room.id,
                    name = room.name,
                    x = cx,
                    y = 1.6f,
                    z = cz,
                    description = room.notes.ifEmpty { "High-fidelity digital twin reconstruction point." },
                    focalHeadingYaw = 0f
                )
            )
        }
        return list
    }
}
