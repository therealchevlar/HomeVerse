package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.data.AppDatabase
import com.example.data.HomeRepository
import com.example.data.model.CaptureSessionEntity
import com.example.data.model.FloorEntity
import com.example.data.model.HomeEntity
import com.example.data.model.PipelineStage
import com.example.data.model.ReconstructionJobEntity
import com.example.data.model.ReconstructionStatus
import com.example.data.model.RoomEntity
import com.example.data.model.RoomType
import com.example.data.model.WaypointNode
import com.example.engine3d.Camera3D
import com.example.engine3d.CameraMode
import com.example.engine3d.CollisionDetector
import com.example.engine3d.Mesh3D
import com.example.engine3d.Vector3
import com.example.reconstruction.CapturePackage
import com.example.reconstruction.FrameMetadata
import com.example.reconstruction.ReconstructionPipeline
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class AppScreen {
    ONBOARDING,
    DASHBOARD,
    SCAN_STUDIO,
    VIEWER_3D,
    RECONSTRUCTION_MONITOR
}

enum class ViewMode {
    DIGITAL_TWIN,   // 3D Dollhouse
    EXPLORE_MODE,   // 1st-Person Walkthrough
    VIRTUAL_TOUR    // 360 Waypoint Tour
}

class HomeverseViewModel(application: Application) : AndroidViewModel(application) {

    private val db = Room.databaseBuilder(
        application,
        AppDatabase::class.java,
        "homeverse.db"
    ).fallbackToDestructiveMigration().build()

    val repository = HomeRepository(db)
    private val pipeline = ReconstructionPipeline(application, repository)

    val homes: StateFlow<List<HomeEntity>> = repository.allHomes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Navigation & Selection
    private val _currentScreen = MutableStateFlow(AppScreen.DASHBOARD)
    val currentScreen = _currentScreen.asStateFlow()

    private val _selectedHome = MutableStateFlow<HomeEntity?>(null)
    val selectedHome = _selectedHome.asStateFlow()

    private val _currentRooms = MutableStateFlow<List<RoomEntity>>(emptyList())
    val currentRooms = _currentRooms.asStateFlow()

    private val _currentFloors = MutableStateFlow<List<FloorEntity>>(emptyList())
    val currentFloors = _currentFloors.asStateFlow()

    private val _activeRoomForScan = MutableStateFlow<RoomEntity?>(null)
    val activeRoomForScan = _activeRoomForScan.asStateFlow()

    // 3D Visualizer State
    private val _viewMode = MutableStateFlow(ViewMode.DIGITAL_TWIN)
    val viewMode = _viewMode.asStateFlow()

    val camera = Camera3D()

    private val _houseMesh = MutableStateFlow(Mesh3D.EMPTY)
    val houseMesh = _houseMesh.asStateFlow()

    private val _tourWaypoints = MutableStateFlow<List<WaypointNode>>(emptyList())
    val tourWaypoints = _tourWaypoints.asStateFlow()

    private val _activeWaypointIndex = MutableStateFlow(0)
    val activeWaypointIndex = _activeWaypointIndex.asStateFlow()

    private val _renderCeilings = MutableStateFlow(false) // Dollhouse mode (cutaway)
    val renderCeilings = _renderCeilings.asStateFlow()

    private val _showWireframe = MutableStateFlow(true)
    val showWireframe = _showWireframe.asStateFlow()

    private val _showPointCloud = MutableStateFlow(true)
    val showPointCloud = _showPointCloud.asStateFlow()

    private val _focusedRoomId = MutableStateFlow<String?>(null)
    val focusedRoomId = _focusedRoomId.asStateFlow()

    // 1st Person Explore State
    private val _playerPos = MutableStateFlow(Vector3(0f, 1.6f, 0f))
    val playerPos = _playerPos.asStateFlow()

    private val _currentRoomNameInExplore = MutableStateFlow("Entrance")
    val currentRoomNameInExplore = _currentRoomNameInExplore.asStateFlow()

    // Scan & Capture State
    private val _isCapturing = MutableStateFlow(false)
    val isCapturing = _isCapturing.asStateFlow()

    private val _capturedFrames = MutableStateFlow<List<FrameMetadata>>(emptyList())
    val capturedFrames = _capturedFrames.asStateFlow()

    private val _sensorYaw = MutableStateFlow(0f)
    val sensorYaw = _sensorYaw.asStateFlow()

    private val _sensorPitch = MutableStateFlow(0f)
    val sensorPitch = _sensorPitch.asStateFlow()

    private val _sensorRoll = MutableStateFlow(0f)
    val sensorRoll = _sensorRoll.asStateFlow()

    private val _trackingConfidence = MutableStateFlow(98)
    val trackingConfidence = _trackingConfidence.asStateFlow()

    // Reconstruction Job State
    private val _activeJob = MutableStateFlow<ReconstructionJobEntity?>(null)
    val activeJob = _activeJob.asStateFlow()

    init {
        viewModelScope.launch {
            repository.seedSampleHomeIfNeeded()
        }
    }

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun selectHome(home: HomeEntity) {
        _selectedHome.value = home
        viewModelScope.launch {
            repository.getRoomsForHome(home.id).collect { rooms ->
                _currentRooms.value = rooms
                refresh3DMesh(home, rooms)
            }
        }
        viewModelScope.launch {
            repository.getFloorsForHome(home.id).collect { floors ->
                _currentFloors.value = floors
            }
        }
    }

    fun openViewerForHome(home: HomeEntity) {
        selectHome(home)
        setViewMode(ViewMode.DIGITAL_TWIN)
        navigateTo(AppScreen.VIEWER_3D)
    }

    private fun refresh3DMesh(home: HomeEntity, rooms: List<RoomEntity>) {
        val mesh = repository.buildHome3DMesh(home, rooms)
        _houseMesh.value = mesh
        val waypoints = repository.buildTourWaypoints(rooms)
        _tourWaypoints.value = waypoints

        // Center camera target
        val midX = (mesh.bounds.min.x + mesh.bounds.max.x) / 2f
        val midZ = (mesh.bounds.min.z + mesh.bounds.max.z) / 2f
        camera.target = Vector3(midX, 1.2f, midZ)
        camera.orbitDistance = (mesh.bounds.max.x - mesh.bounds.min.x).coerceAtLeast(8f) * 1.5f
        camera.updateOrbit()

        // Place 1st-person initial position at first room
        if (rooms.isNotEmpty()) {
            val r0 = rooms[0]
            val px = r0.offsetX + r0.widthMeters / 2f
            val pz = r0.offsetZ + r0.lengthMeters / 2f
            _playerPos.value = Vector3(px, 1.6f, pz)
            _currentRoomNameInExplore.value = r0.name
        }
    }

    fun setViewMode(mode: ViewMode) {
        _viewMode.value = mode
        when (mode) {
            ViewMode.DIGITAL_TWIN -> {
                camera.mode = CameraMode.ORBIT
                _renderCeilings.value = false // dollhouse roof cutaway
            }
            ViewMode.EXPLORE_MODE -> {
                camera.mode = CameraMode.FIRST_PERSON
                camera.position = _playerPos.value
                camera.fpYaw = 0f
                camera.fpPitch = 0f
                _renderCeilings.value = true // full interior immersion
            }
            ViewMode.VIRTUAL_TOUR -> {
                camera.mode = CameraMode.WAYPOINT
                _renderCeilings.value = true
                jumpToWaypoint(_activeWaypointIndex.value)
            }
        }
    }

    fun toggleCeilings() {
        _renderCeilings.value = !_renderCeilings.value
    }

    fun toggleWireframe() {
        _showWireframe.value = !_showWireframe.value
    }

    fun togglePointCloud() {
        _showPointCloud.value = !_showPointCloud.value
    }

    fun setFocusedRoom(roomId: String?) {
        _focusedRoomId.value = roomId
    }

    // Camera Interactions
    fun updateOrbit(deltaYaw: Float, deltaPitch: Float, zoomFactor: Float = 1f) {
        camera.orbitYaw += deltaYaw
        camera.orbitPitch = (camera.orbitPitch + deltaPitch).coerceIn(0.1f, 1.45f)
        if (zoomFactor != 1f) {
            camera.orbitDistance = (camera.orbitDistance * zoomFactor).coerceIn(3f, 30f)
        }
        camera.updateOrbit()
    }

    fun updateFirstPersonLook(deltaYaw: Float, deltaPitch: Float) {
        camera.fpYaw += deltaYaw
        camera.fpPitch = (camera.fpPitch + deltaPitch).coerceIn(-1.3f, 1.3f)
    }

    fun updateFirstPersonMove(forwardAmount: Float, strafeAmount: Float) {
        val speed = 0.12f
        val yaw = camera.fpYaw

        val forwardX = sin(yaw) * forwardAmount * speed
        val forwardZ = cos(yaw) * forwardAmount * speed

        val rightX = cos(yaw) * strafeAmount * speed
        val rightZ = -sin(yaw) * strafeAmount * speed

        val desiredPos = _playerPos.value + Vector3(forwardX + rightX, 0f, forwardZ + rightZ)

        // Resolve collision against house perimeter
        val resolved = CollisionDetector.clampToHousePerimeter(
            _playerPos.value,
            desiredPos,
            _houseMesh.value.bounds
        )

        _playerPos.value = resolved
        camera.position = resolved

        // Detect current room
        val roomMap = _currentRooms.value.associate {
            it.id to com.example.engine3d.BoundingBox(
                Vector3(it.offsetX, 0f, it.offsetZ),
                Vector3(it.offsetX + it.widthMeters, it.heightMeters, it.offsetZ + it.lengthMeters)
            )
        }
        val insideRoomId = CollisionDetector.findCurrentRoomId(resolved, roomMap)
        val room = _currentRooms.value.find { it.id == insideRoomId }
        _currentRoomNameInExplore.value = room?.name ?: "Hallway"
    }

    fun jumpToWaypoint(index: Int) {
        val waypoints = _tourWaypoints.value
        if (waypoints.isEmpty()) return
        val safeIndex = index.coerceIn(0, waypoints.size - 1)
        _activeWaypointIndex.value = safeIndex
        val wp = waypoints[safeIndex]
        camera.position = Vector3(wp.x, wp.y, wp.z)
        camera.fpYaw = wp.focalHeadingYaw
        camera.fpPitch = 0f
    }

    fun nextWaypoint() {
        val total = _tourWaypoints.value.size
        if (total > 0) {
            val next = (_activeWaypointIndex.value + 1) % total
            jumpToWaypoint(next)
        }
    }

    fun prevWaypoint() {
        val total = _tourWaypoints.value.size
        if (total > 0) {
            val prev = if (_activeWaypointIndex.value - 1 < 0) total - 1 else _activeWaypointIndex.value - 1
            jumpToWaypoint(prev)
        }
    }

    // Scanning & Capture Flow
    fun prepareScanForRoom(room: RoomEntity) {
        _activeRoomForScan.value = room
        _capturedFrames.value = emptyList()
        _isCapturing.value = false
        navigateTo(AppScreen.SCAN_STUDIO)
    }

    fun updateSensors(yaw: Float, pitch: Float, roll: Float) {
        _sensorYaw.value = yaw
        _sensorPitch.value = pitch
        _sensorRoll.value = roll
    }

    fun toggleCaptureSession() {
        _isCapturing.value = !_isCapturing.value
    }

    fun recordKeyframe() {
        val index = _capturedFrames.value.size
        val estPose = Vector3(
            sin(_sensorYaw.value) * 1.5f,
            1.5f,
            cos(_sensorYaw.value) * 1.5f
        )
        val frame = FrameMetadata(
            frameIndex = index,
            timestamp = System.currentTimeMillis(),
            gyroYaw = _sensorYaw.value,
            gyroPitch = _sensorPitch.value,
            gyroRoll = _sensorRoll.value,
            estimatedPose = estPose,
            focalLengthMm = 26f,
            lightLux = 420f
        )
        _capturedFrames.value = _capturedFrames.value + frame
    }

    fun finishCaptureAndStartReconstruction() {
        val room = _activeRoomForScan.value ?: return
        val home = _selectedHome.value ?: return
        _isCapturing.value = false

        viewModelScope.launch {
            val sessionId = "sess-${UUID.randomUUID().toString().take(8)}"
            val capturePkg = CapturePackage(
                sessionId = sessionId,
                roomId = room.id,
                homeId = home.id,
                createdAt = System.currentTimeMillis(),
                frames = _capturedFrames.value,
                packageDirectoryPath = ""
            )

            // Persist capture session record
            repository.insertCaptureSession(
                CaptureSessionEntity(
                    id = sessionId,
                    roomId = room.id,
                    homeId = home.id,
                    frameCount = _capturedFrames.value.size,
                    status = "COMPLETED"
                )
            )

            // Create and launch reconstruction job
            val jobId = "job-${UUID.randomUUID().toString().take(8)}"
            val initialJob = ReconstructionJobEntity(
                id = jobId,
                captureSessionId = sessionId,
                roomId = room.id,
                homeId = home.id,
                currentStage = PipelineStage.VALIDATE_CAPTURE,
                progressPercent = 0,
                status = "PROCESSING",
                logOutput = ""
            )
            repository.insertJob(initialJob)
            _activeJob.value = initialJob
            navigateTo(AppScreen.RECONSTRUCTION_MONITOR)

            pipeline.processCapture(initialJob, room, capturePkg) { updatedJob ->
                _activeJob.value = updatedJob
            }
        }
    }

    // Home & Room Creation
    fun createNewHome(name: String, description: String, onDone: (HomeEntity) -> Unit) {
        viewModelScope.launch {
            val homeId = "home-${UUID.randomUUID().toString().take(8)}"
            val floorId = "fl-1-$homeId"
            val newHome = HomeEntity(
                id = homeId,
                name = name.ifEmpty { "My New Home" },
                description = description.ifEmpty { "Spatial scan digital project" },
                createdAt = System.currentTimeMillis(),
                totalFloors = 1,
                totalRooms = 0,
                isSample = false
            )
            repository.insertHome(newHome)

            val groundFloor = FloorEntity(
                id = floorId,
                homeId = homeId,
                floorNumber = 1,
                name = "Ground Floor",
                levelElevationMeters = 0f
            )
            repository.insertFloor(groundFloor)

            selectHome(newHome)
            onDone(newHome)
        }
    }

    fun addRoom(
        homeId: String,
        name: String,
        roomType: RoomType,
        width: Float,
        length: Float,
        height: Float = 2.8f
    ) {
        viewModelScope.launch {
            val roomId = "rm-${UUID.randomUUID().toString().take(8)}"
            val currentCount = _currentRooms.value.size
            // Arrange new room adjacent to existing layout
            val offsetX = (currentCount % 2) * 5.0f - 2.5f
            val offsetZ = (currentCount / 2) * 5.0f - 2.5f

            val newRoom = RoomEntity(
                id = roomId,
                floorId = _currentFloors.value.firstOrNull()?.id ?: "fl-1-$homeId",
                homeId = homeId,
                name = name.ifEmpty { roomType.displayName },
                roomType = roomType,
                widthMeters = width,
                lengthMeters = length,
                heightMeters = height,
                offsetX = offsetX,
                offsetZ = offsetZ,
                status = ReconstructionStatus.NOT_STARTED
            )
            repository.insertRoom(newRoom)
            prepareScanForRoom(newRoom)
        }
    }

    fun deleteHome(homeId: String) {
        viewModelScope.launch {
            repository.deleteHome(homeId)
            _selectedHome.value = null
            _currentRooms.value = emptyList()
            _houseMesh.value = Mesh3D.EMPTY
            navigateTo(AppScreen.DASHBOARD)
        }
    }

    fun deleteRoom(roomId: String) {
        viewModelScope.launch {
            repository.deleteRoom(roomId)
            _selectedHome.value?.let { selectHome(it) }
        }
    }
}
