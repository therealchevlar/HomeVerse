package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.ReconstructionMonitorScreen
import com.example.ui.screens.ScanStudioScreen
import com.example.ui.screens.ThreeModeViewerScreen
import com.example.ui.theme.HomeverseTheme
import com.example.ui.theme.ObsidianBg
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.HomeverseViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: HomeverseViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HomeverseTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = ObsidianBg
                ) {
                    HomeverseApp(viewModel)
                }
            }
        }
    }
}

@Composable
fun HomeverseApp(viewModel: HomeverseViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val homes by viewModel.homes.collectAsState()
    val selectedHome by viewModel.selectedHome.collectAsState()
    val rooms by viewModel.currentRooms.collectAsState()
    val houseMesh by viewModel.houseMesh.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    val tourWaypoints by viewModel.tourWaypoints.collectAsState()
    val activeWaypointIndex by viewModel.activeWaypointIndex.collectAsState()
    val renderCeilings by viewModel.renderCeilings.collectAsState()
    val showWireframe by viewModel.showWireframe.collectAsState()
    val showPointCloud by viewModel.showPointCloud.collectAsState()
    val focusedRoomId by viewModel.focusedRoomId.collectAsState()
    val currentRoomNameInExplore by viewModel.currentRoomNameInExplore.collectAsState()

    // Scanning state
    val activeRoomForScan by viewModel.activeRoomForScan.collectAsState()
    val capturedFrames by viewModel.capturedFrames.collectAsState()
    val sensorYaw by viewModel.sensorYaw.collectAsState()
    val sensorPitch by viewModel.sensorPitch.collectAsState()
    val sensorRoll by viewModel.sensorRoll.collectAsState()
    val isAutoCapturing by viewModel.isCapturing.collectAsState()

    // Reconstruction Job state
    val activeJob by viewModel.activeJob.collectAsState()

    when (currentScreen) {
        AppScreen.ONBOARDING -> {
            OnboardingScreen(
                onExploreShowcase = {
                    val sample = homes.find { it.isSample } ?: homes.firstOrNull()
                    if (sample != null) {
                        viewModel.openViewerForHome(sample)
                    } else {
                        viewModel.navigateTo(AppScreen.DASHBOARD)
                    }
                },
                onStartScan = {
                    viewModel.navigateTo(AppScreen.DASHBOARD)
                }
            )
        }

        AppScreen.DASHBOARD -> {
            DashboardScreen(
                homes = homes,
                onSelectHomeToView = { home ->
                    viewModel.openViewerForHome(home)
                },
                onCreateNewHome = { name, desc ->
                    viewModel.createNewHome(name, desc) { createdHome ->
                        viewModel.openViewerForHome(createdHome)
                    }
                },
                onDeleteHome = { id ->
                    viewModel.deleteHome(id)
                }
            )
        }

        AppScreen.SCAN_STUDIO -> {
            BackHandler {
                viewModel.navigateTo(
                    if (selectedHome != null) AppScreen.VIEWER_3D else AppScreen.DASHBOARD
                )
            }
            ScanStudioScreen(
                room = activeRoomForScan,
                capturedFrames = capturedFrames,
                sensorYaw = sensorYaw,
                sensorPitch = sensorPitch,
                sensorRoll = sensorRoll,
                isAutoCapturing = isAutoCapturing,
                onUpdateSensors = { y, p, r -> viewModel.updateSensors(y, p, r) },
                onRecordKeyframe = { viewModel.recordKeyframe() },
                onToggleAutoCapture = { viewModel.toggleCaptureSession() },
                onFinishAndReconstruct = { viewModel.finishCaptureAndStartReconstruction() },
                onBack = {
                    viewModel.navigateTo(
                        if (selectedHome != null) AppScreen.VIEWER_3D else AppScreen.DASHBOARD
                    )
                }
            )
        }

        AppScreen.RECONSTRUCTION_MONITOR -> {
            BackHandler {
                viewModel.navigateTo(AppScreen.DASHBOARD)
            }
            ReconstructionMonitorScreen(
                job = activeJob,
                onLaunchViewer = {
                    selectedHome?.let { viewModel.openViewerForHome(it) }
                        ?: viewModel.navigateTo(AppScreen.DASHBOARD)
                }
            )
        }

        AppScreen.VIEWER_3D -> {
            BackHandler {
                viewModel.navigateTo(AppScreen.DASHBOARD)
            }
            ThreeModeViewerScreen(
                home = selectedHome,
                rooms = rooms,
                mesh = houseMesh,
                camera = viewModel.camera,
                viewMode = viewMode,
                waypoints = tourWaypoints,
                activeWaypointIndex = activeWaypointIndex,
                renderCeilings = renderCeilings,
                showWireframe = showWireframe,
                showPointCloud = showPointCloud,
                focusedRoomId = focusedRoomId,
                currentRoomNameInExplore = currentRoomNameInExplore,
                onSetViewMode = { mode -> viewModel.setViewMode(mode) },
                onOrbitChange = { dy, dp, z -> viewModel.updateOrbit(dy, dp, z) },
                onFirstPersonMove = { f, s -> viewModel.updateFirstPersonMove(f, s) },
                onFirstPersonLook = { dy, dp -> viewModel.updateFirstPersonLook(dy, dp) },
                onJumpWaypoint = { idx -> viewModel.jumpToWaypoint(idx) },
                onNextWaypoint = { viewModel.nextWaypoint() },
                onPrevWaypoint = { viewModel.prevWaypoint() },
                onToggleCeilings = { viewModel.toggleCeilings() },
                onToggleWireframe = { viewModel.toggleWireframe() },
                onTogglePointCloud = { viewModel.togglePointCloud() },
                onSelectRoomFocus = { rId -> viewModel.setFocusedRoom(rId) },
                onAddRoom = { name, type, w, l ->
                    selectedHome?.let { h ->
                        viewModel.addRoom(h.id, name, type, w, l)
                    }
                },
                onBack = {
                    viewModel.navigateTo(AppScreen.DASHBOARD)
                }
            )
        }
    }
}
