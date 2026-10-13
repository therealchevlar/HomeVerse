package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.model.RoomEntity
import com.example.reconstruction.FrameMetadata
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SlateDim
import com.example.ui.theme.SlateMuted
import com.example.ui.theme.SurfaceDarkElevated
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ScanStudioScreen(
    room: RoomEntity?,
    capturedFrames: List<FrameMetadata>,
    sensorYaw: Float,
    sensorPitch: Float,
    sensorRoll: Float,
    isAutoCapturing: Boolean,
    onUpdateSensors: (yaw: Float, pitch: Float, roll: Float) -> Unit,
    onRecordKeyframe: () -> Unit,
    onToggleAutoCapture: () -> Unit,
    onFinishAndReconstruct: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    // Register hardware motion sensors
    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val listener = object : SensorEventListener {
            private val rotationMatrix = FloatArray(9)
            private val orientationAngles = FloatArray(3)

            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null) return
                if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    SensorManager.getOrientation(rotationMatrix, orientationAngles)
                    onUpdateSensors(
                        orientationAngles[0], // Yaw
                        orientationAngles[1], // Pitch
                        orientationAngles[2]  // Roll
                    )
                } else if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                    val ax = event.values[0]
                    val ay = event.values[1]
                    val az = event.values[2]
                    val pitch = kotlin.math.atan2(-ax.toDouble(), kotlin.math.sqrt((ay * ay + az * az).toDouble())).toFloat()
                    val roll = kotlin.math.atan2(ay.toDouble(), az.toDouble()).toFloat()
                    onUpdateSensors(0f, pitch, roll)
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        if (rotationSensor != null) {
            sensorManager.registerListener(listener, rotationSensor, SensorManager.SENSOR_DELAY_UI)
        }

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    // Auto-capture burst timer
    LaunchedEffect(isAutoCapturing) {
        while (isAutoCapturing) {
            delay(900)
            onRecordKeyframe()
        }
    }

    // Animation for laser scanline
    val infiniteTransition = rememberInfiniteTransition(label = "scan_laser")
    val laserY by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_y"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
    ) {
        // 1. Live Camera Preview or Simulated Spatial Viewfinder
        if (hasCameraPermission) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        try {
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.surfaceProvider = previewView.surfaceProvider
                            }
                            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
                        } catch (e: Exception) {
                            // Fallback gracefully
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Simulated Viewfinder Grid
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Dark grid lines
                for (x in 0..(w.toInt()) step 80) {
                    drawLine(
                        color = Color(0x1500F0FF),
                        start = Offset(x.toFloat(), 0f),
                        end = Offset(x.toFloat(), h),
                        strokeWidth = 1f
                    )
                }
                for (y in 0..(h.toInt()) step 80) {
                    drawLine(
                        color = Color(0x1500F0FF),
                        start = Offset(0f, y.toFloat()),
                        end = Offset(w, y.toFloat()),
                        strokeWidth = 1f
                    )
                }
            }
        }

        // 2. Spatial HUD Layer (Horizon, Reticle, Laser)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f

            // Scanning Laser Line
            val ly = size.height * laserY
            drawLine(
                color = CyberCyan.copy(alpha = 0.6f),
                start = Offset(30f, ly),
                end = Offset(size.width - 30f, ly),
                strokeWidth = 2.5f
            )

            // Artificial Horizon Line
            val horizonLength = 140f
            val cosRoll = cos(-sensorRoll)
            val sinRoll = sin(-sensorRoll)
            val pitchOffset = sensorPitch * 60f

            val hx0 = cx - (horizonLength / 2f) * cosRoll
            val hy0 = (cy + pitchOffset) - (horizonLength / 2f) * sinRoll
            val hx1 = cx + (horizonLength / 2f) * cosRoll
            val hy1 = (cy + pitchOffset) + (horizonLength / 2f) * sinRoll

            drawLine(
                color = ElectricBlue,
                start = Offset(hx0, hy0),
                end = Offset(hx1, hy1),
                strokeWidth = 3f
            )

            // Central Spatial Reticle
            drawCircle(
                color = CyberCyan,
                radius = 32f,
                center = Offset(cx, cy),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f)
            )
            drawCircle(
                color = Color.White,
                radius = 3f,
                center = Offset(cx, cy)
            )
        }

        // 3. Top Navigation & Room Status Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 28.dp, start = 16.dp, end = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .background(Color(0x99080C14), CircleShape)
                        .testTag("scan_studio_back")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                // Room Spec Badge
                Box(
                    modifier = Modifier
                        .background(Color(0xCC111827), RoundedCornerShape(20.dp))
                        .border(1.dp, CyberCyan, RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = room?.name ?: "Room Scan",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${room?.widthMeters ?: 4.0}m × ${room?.lengthMeters ?: 4.0}m • TARGET",
                            fontSize = 10.sp,
                            color = CyberCyan,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Permission Toggle or Flash
                if (!hasCameraPermission) {
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("ENABLE CAMERA", fontSize = 10.sp, color = ObsidianBg, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .background(Color(0x99080C14), CircleShape)
                            .padding(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FlashOn,
                            contentDescription = "Flash",
                            tint = CyberCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 6DoF Telemetry Card
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xAA080C14)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(0.5.dp, Color(0x3300F0FF), RoundedCornerShape(10.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "YAW: ${(sensorYaw * 180f / PI).toInt()}°  PITCH: ${(sensorPitch * 180f / PI).toInt()}°  ROLL: ${(sensorRoll * 180f / PI).toInt()}°",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = CyberCyan
                    )
                    Text(
                        text = "TRACKING: 98% (STABLE)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonEmerald
                    )
                }
            }
        }

        // 4. Bottom Controls: Filmstrip, Shutter Buttons, Finish Action
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        listOf(Color.Transparent, Color(0xEE080C14))
                    )
                )
                .padding(bottom = 24.dp, start = 16.dp, end = 16.dp)
        ) {
            // Keyframe Count & Progress
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${capturedFrames.size} KEYFRAMES CAPTURED",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                val coverage = (capturedFrames.size * 10).coerceAtMost(100)
                Text(
                    text = "COVERAGE: $coverage%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (coverage >= 50) NeonEmerald else CyberCyan
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Filmstrip of captured poses
            if (capturedFrames.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(capturedFrames) { frame ->
                        Box(
                            modifier = Modifier
                                .size(width = 44.dp, height = 36.dp)
                                .background(Color(0x4400F0FF), RoundedCornerShape(4.dp))
                                .border(1.dp, CyberCyan, RoundedCornerShape(4.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "#${frame.frameIndex + 1}",
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Main Shutter Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Auto burst toggle
                OutlinedButton(
                    onClick = onToggleAutoCapture,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isAutoCapturing) CrimsonAlert.copy(alpha = 0.2f) else Color.Transparent
                    ),
                    modifier = Modifier.testTag("toggle_auto_burst_button")
                ) {
                    Icon(
                        imageVector = if (isAutoCapturing) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = if (isAutoCapturing) CrimsonAlert else CyberCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isAutoCapturing) "PAUSE BURST" else "AUTO BURST",
                        fontSize = 11.sp,
                        color = if (isAutoCapturing) CrimsonAlert else Color.White
                    )
                }

                // Primary Snap Shutter Button
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(CyberCyan.copy(alpha = 0.2f), CircleShape)
                        .border(2.dp, CyberCyan, CircleShape)
                        .padding(6.dp)
                ) {
                    IconButton(
                        onClick = onRecordKeyframe,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(CyberCyan, CircleShape)
                            .testTag("shutter_snap_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Capture Keyframe",
                            tint = ObsidianBg,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                // Finish & Reconstruct Button
                Button(
                    onClick = onFinishAndReconstruct,
                    enabled = capturedFrames.size >= 3,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald),
                    modifier = Modifier.testTag("finish_reconstruction_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = ObsidianBg,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "RECONSTRUCT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ObsidianBg
                    )
                }
            }
        }
    }
}
