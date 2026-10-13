package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FilterCenterFocus
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Roofing
import androidx.compose.material.icons.filled.Tour
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.HomeEntity
import com.example.data.model.RoomEntity
import com.example.data.model.RoomType
import com.example.data.model.WaypointNode
import com.example.engine3d.Camera3D
import com.example.engine3d.Mesh3D
import com.example.engine3d.Renderer3D
import com.example.engine3d.Vector3
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SlateDim
import com.example.ui.theme.SlateMuted
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceDarkElevated
import com.example.ui.viewmodel.ViewMode
import kotlin.math.roundToInt

@Composable
fun ThreeModeViewerScreen(
    home: HomeEntity?,
    rooms: List<RoomEntity>,
    mesh: Mesh3D,
    camera: Camera3D,
    viewMode: ViewMode,
    waypoints: List<WaypointNode>,
    activeWaypointIndex: Int,
    renderCeilings: Boolean,
    showWireframe: Boolean,
    showPointCloud: Boolean,
    focusedRoomId: String?,
    currentRoomNameInExplore: String,
    onSetViewMode: (ViewMode) -> Unit,
    onOrbitChange: (deltaYaw: Float, deltaPitch: Float, zoomFactor: Float) -> Unit,
    onFirstPersonMove: (forward: Float, strafe: Float) -> Unit,
    onFirstPersonLook: (deltaYaw: Float, deltaPitch: Float) -> Unit,
    onJumpWaypoint: (Int) -> Unit,
    onNextWaypoint: () -> Unit,
    onPrevWaypoint: () -> Unit,
    onToggleCeilings: () -> Unit,
    onToggleWireframe: () -> Unit,
    onTogglePointCloud: () -> Unit,
    onSelectRoomFocus: (String?) -> Unit,
    onAddRoom: (name: String, type: RoomType, width: Float, length: Float) -> Unit,
    onBack: () -> Unit
) {
    var showAddRoomDialog by remember { mutableStateOf(false) }
    var roomDropdownExpanded by remember { mutableStateOf(false) }

    // Joystick touch drag state for Explore Mode
    var joystickOffsetX by remember { mutableFloatStateOf(0f) }
    var joystickOffsetY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
    ) {
        // 1. Interactive 3D Canvas
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(viewMode) {
                    when (viewMode) {
                        ViewMode.DIGITAL_TWIN -> {
                            detectTransformGestures { _, pan, zoom, _ ->
                                onOrbitChange(pan.x * 0.008f, -pan.y * 0.008f, 1f / zoom)
                            }
                        }
                        ViewMode.EXPLORE_MODE -> {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                // Right-side drag controls look
                                onFirstPersonLook(dragAmount.x * 0.006f, -dragAmount.y * 0.006f)
                            }
                        }
                        ViewMode.VIRTUAL_TOUR -> {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                onFirstPersonLook(dragAmount.x * 0.007f, -dragAmount.y * 0.007f)
                            }
                        }
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                Renderer3D.render(
                    drawScope = this,
                    mesh = mesh,
                    camera = camera,
                    renderCeilings = renderCeilings,
                    selectedRoomId = focusedRoomId,
                    showWireframe = showWireframe,
                    showPointCloud = showPointCloud
                )

                // If in Tour Mode, render waypoints
                if (viewMode == ViewMode.VIRTUAL_TOUR) {
                    waypoints.forEachIndexed { i, wp ->
                        Renderer3D.drawWaypointPin(
                            drawScope = this,
                            pos = Vector3(wp.x, wp.y, wp.z),
                            camera = camera,
                            label = wp.name,
                            isActive = i == activeWaypointIndex
                        )
                    }
                }
            }
        }

        // 2. Top Header Bar & Mode Selector Tabs
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xEE080C14), Color(0xAA080C14), Color.Transparent)
                    )
                )
                .padding(top = 28.dp, start = 16.dp, end = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .background(SurfaceDarkElevated, CircleShape)
                            .testTag("viewer_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = home?.name ?: "Digital Home",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${rooms.size} Reconstructed Rooms",
                            fontSize = 11.sp,
                            color = CyberCyan
                        )
                    }
                }

                // Add Room Action Button
                Button(
                    onClick = { showAddRoomDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceDarkElevated),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = Brush.horizontalGradient(listOf(CyberCyan, ElectricBlue))
                    ),
                    modifier = Modifier.testTag("add_room_scan_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = CyberCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "SCAN ROOM", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // The Three Viewing Modes Tabs
            TabRow(
                selectedTabIndex = viewMode.ordinal,
                containerColor = SurfaceDark,
                contentColor = CyberCyan,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[viewMode.ordinal]),
                        color = CyberCyan
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SurfaceBorder, RoundedCornerShape(10.dp))
            ) {
                Tab(
                    selected = viewMode == ViewMode.DIGITAL_TWIN,
                    onClick = { onSetViewMode(ViewMode.DIGITAL_TWIN) },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ViewInAr, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("DIGITAL TWIN", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    },
                    modifier = Modifier.testTag("tab_digital_twin")
                )
                Tab(
                    selected = viewMode == ViewMode.EXPLORE_MODE,
                    onClick = { onSetViewMode(ViewMode.EXPLORE_MODE) },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Explore, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("EXPLORE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    },
                    modifier = Modifier.testTag("tab_explore_mode")
                )
                Tab(
                    selected = viewMode == ViewMode.VIRTUAL_TOUR,
                    onClick = { onSetViewMode(ViewMode.VIRTUAL_TOUR) },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Tour, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("TOUR", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    },
                    modifier = Modifier.testTag("tab_virtual_tour")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Mode-Specific Toolchips Row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (viewMode == ViewMode.DIGITAL_TWIN) {
                    item {
                        FilterChip(
                            selected = !renderCeilings,
                            onClick = onToggleCeilings,
                            label = { Text("Dollhouse Roof Cutaway", fontSize = 11.sp) },
                            leadingIcon = { Icon(Icons.Default.Roofing, contentDescription = null, modifier = Modifier.size(14.dp)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0x3300F0FF),
                                selectedLabelColor = CyberCyan
                            )
                        )
                    }

                    item {
                        FilterChip(
                            selected = showWireframe,
                            onClick = onToggleWireframe,
                            label = { Text("Wireframe Grid", fontSize = 11.sp) },
                            leadingIcon = { Icon(Icons.Default.GridOn, contentDescription = null, modifier = Modifier.size(14.dp)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0x3300F0FF),
                                selectedLabelColor = CyberCyan
                            )
                        )
                    }

                    item {
                        FilterChip(
                            selected = showPointCloud,
                            onClick = onTogglePointCloud,
                            label = { Text("Point Cloud", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0x3300F0FF),
                                selectedLabelColor = CyberCyan
                            )
                        )
                    }

                    item {
                        Box {
                            FilterChip(
                                selected = focusedRoomId != null,
                                onClick = { roomDropdownExpanded = true },
                                label = {
                                    val sel = rooms.find { it.id == focusedRoomId }
                                    Text(sel?.name ?: "All Rooms", fontSize = 11.sp)
                                },
                                leadingIcon = { Icon(Icons.Default.FilterCenterFocus, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            )

                            DropdownMenu(
                                expanded = roomDropdownExpanded,
                                onDismissRequest = { roomDropdownExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("All Rooms (Overview)") },
                                    onClick = {
                                        onSelectRoomFocus(null)
                                        roomDropdownExpanded = false
                                    }
                                )
                                rooms.forEach { rm ->
                                    DropdownMenuItem(
                                        text = { Text(rm.name) },
                                        onClick = {
                                            onSelectRoomFocus(rm.id)
                                            roomDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Mode-Specific Bottom Floating HUDs
        when (viewMode) {
            ViewMode.DIGITAL_TWIN -> {
                // Focused Room Info Card (if a room is selected)
                val focused = rooms.find { it.id == focusedRoomId }
                if (focused != null) {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xEE111827)),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 24.dp, start = 20.dp, end = 20.dp)
                            .fillMaxWidth()
                            .border(1.dp, CyberCyan, RoundedCornerShape(14.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = focused.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text(
                                    text = "${focused.widthMeters}m × ${focused.lengthMeters}m • ${(focused.widthMeters * focused.lengthMeters).toInt()} m²",
                                    fontSize = 12.sp,
                                    color = CyberCyan
                                )
                                if (focused.notes.isNotBlank()) {
                                    Text(text = focused.notes, fontSize = 11.sp, color = SlateMuted)
                                }
                            }

                            Button(
                                onClick = { onSetViewMode(ViewMode.EXPLORE_MODE) },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan)
                            ) {
                                Text("WALK IN", color = ObsidianBg, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            ViewMode.EXPLORE_MODE -> {
                // First Person Joystick & Telemetry
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 28.dp, start = 24.dp, end = 24.dp)
                ) {
                    // Room Location Badge at top of bottom controls
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 160.dp)
                            .background(Color(0xDD080C14), RoundedCornerShape(20.dp))
                            .border(1.dp, CyberCyan, RoundedCornerShape(20.dp))
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "LOCATION: $currentRoomNameInExplore",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberCyan,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Left On-Screen Virtual Touch Joystick
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .size(130.dp)
                            .background(Color(0x3300F0FF), CircleShape)
                            .border(1.5.dp, CyberCyan, CircleShape)
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragEnd = {
                                        joystickOffsetX = 0f
                                        joystickOffsetY = 0f
                                    },
                                    onDragCancel = {
                                        joystickOffsetX = 0f
                                        joystickOffsetY = 0f
                                    }
                                ) { change, dragAmount ->
                                    change.consume()
                                    val radius = 55f
                                    val newX = (joystickOffsetX + dragAmount.x).coerceIn(-radius, radius)
                                    val newY = (joystickOffsetY + dragAmount.y).coerceIn(-radius, radius)
                                    joystickOffsetX = newX
                                    joystickOffsetY = newY

                                    // -Y is forward, +X is strafe right
                                    val forwardNorm = -newY / radius
                                    val strafeNorm = newX / radius
                                    onFirstPersonMove(forwardNorm, strafeNorm)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        // Inner thumb indicator
                        Box(
                            modifier = Modifier
                                .offset { IntOffset(joystickOffsetX.roundToInt(), joystickOffsetY.roundToInt()) }
                                .size(50.dp)
                                .background(CyberCyan, CircleShape)
                        )
                    }

                    // Right Instructions Note
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xBB111827)),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(bottom = 12.dp)
                            .border(0.5.dp, SurfaceBorder, RoundedCornerShape(12.dp))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(text = "SWIPE RIGHT TO LOOK", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(text = "USE JOYSTICK TO WALK", fontSize = 10.sp, color = CyberCyan)
                        }
                    }
                }
            }

            ViewMode.VIRTUAL_TOUR -> {
                // Waypoint Tour Carousel & Navigation Bar
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color(0xEE080C14))
                            )
                        )
                        .padding(bottom = 24.dp, start = 16.dp, end = 16.dp)
                ) {
                    // Waypoint node cards carousel
                    if (waypoints.isNotEmpty()) {
                        val activeWp = waypoints.getOrNull(activeWaypointIndex)
                        if (activeWp != null) {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = SurfaceDarkElevated),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, CyberCyan, RoundedCornerShape(12.dp))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "VIEWPOINT ${activeWaypointIndex + 1}/${waypoints.size}: ${activeWp.name}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CyberCyan
                                        )
                                        Text(
                                            text = "360° WAYPOINT",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = NeonEmerald
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = activeWp.description,
                                        fontSize = 11.sp,
                                        color = SlateMuted
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        // Navigation arrows and waypoint pills
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = onPrevWaypoint,
                                modifier = Modifier
                                    .background(SurfaceDarkElevated, CircleShape)
                                    .testTag("tour_prev_waypoint")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Previous Waypoint",
                                    tint = Color.White
                                )
                            }

                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                itemsIndexed(waypoints) { idx, wp ->
                                    val isSelected = idx == activeWaypointIndex
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                if (isSelected) CyberCyan else SurfaceDark,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .border(
                                                1.dp,
                                                if (isSelected) CyberCyan else SurfaceBorder,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = wp.name.take(14),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) ObsidianBg else Color.White
                                        )
                                    }
                                }
                            }

                            IconButton(
                                onClick = onNextWaypoint,
                                modifier = Modifier
                                    .background(SurfaceDarkElevated, CircleShape)
                                    .testTag("tour_next_waypoint")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Next Waypoint",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddRoomDialog) {
        AddRoomDialog(
            onDismiss = { showAddRoomDialog = false },
            onConfirm = { name, type, w, l ->
                showAddRoomDialog = false
                onAddRoom(name, type, w, l)
            }
        )
    }
}

@Composable
fun AddRoomDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, type: RoomType, width: Float, length: Float) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(RoomType.LIVING_ROOM) }
    var widthStr by remember { mutableStateOf("4.5") }
    var lengthStr by remember { mutableStateOf("4.0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDarkElevated,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(text = "Add Room to Scan", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
            Column {
                Text(
                    text = "Configure the room profile and launch the spatial scanner.",
                    color = SlateMuted,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Room Name") },
                    placeholder = { Text("e.g. Master Suite") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = SurfaceBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_room_name_input")
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = widthStr,
                        onValueChange = { widthStr = it },
                        label = { Text("Width (m)") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = SurfaceBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    OutlinedTextField(
                        value = lengthStr,
                        onValueChange = { lengthStr = it },
                        label = { Text("Length (m)") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = SurfaceBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val w = widthStr.toFloatOrNull() ?: 4.0f
                    val l = lengthStr.toFloatOrNull() ?: 4.0f
                    onConfirm(name, selectedType, w, l)
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                modifier = Modifier.testTag("confirm_add_room_button")
            ) {
                Text("LAUNCH SCAN STUDIO", color = ObsidianBg, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = SlateMuted)
            }
        }
    )
}
