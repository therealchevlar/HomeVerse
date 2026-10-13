package com.example.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PipelineStage
import com.example.data.model.ReconstructionJobEntity
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SlateDim
import com.example.ui.theme.SlateMuted
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceDarkElevated

@Composable
fun ReconstructionMonitorScreen(
    job: ReconstructionJobEntity?,
    onLaunchViewer: () -> Unit
) {
    val scrollState = rememberScrollState()

    // Auto scroll logs as they update
    LaunchedEffect(job?.logOutput) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Spacer(modifier = Modifier.height(24.dp))
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0x2200F0FF), CircleShape)
                                .border(1.dp, CyberCyan, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Memory,
                                contentDescription = null,
                                tint = CyberCyan,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "RECONSTRUCTION PIPELINE",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                color = Color.White
                            )
                            Text(
                                text = "Job ID: ${job?.id ?: "N/A"}",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = SlateMuted
                            )
                        }
                    }

                    val isDone = job?.currentStage == PipelineStage.COMPLETED
                    Box(
                        modifier = Modifier
                            .background(
                                if (isDone) Color(0x3310B981) else Color(0x3300F0FF),
                                RoundedCornerShape(100.dp)
                            )
                            .border(
                                1.dp,
                                if (isDone) NeonEmerald else CyberCyan,
                                RoundedCornerShape(100.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isDone) "RECONSTRUCTED" else "PROCESSING",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDone) NeonEmerald else CyberCyan
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Progress Bar Card
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDarkElevated),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SurfaceBorder, RoundedCornerShape(14.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = job?.currentStage?.stageName ?: "Initializing...",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            Text(
                                text = "${job?.progressPercent ?: 0}%",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyberCyan
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        LinearProgressIndicator(
                            progress = { (job?.progressPercent ?: 0) / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            color = if (job?.currentStage == PipelineStage.COMPLETED) NeonEmerald else CyberCyan,
                            trackColor = Color(0xFF1E293B)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "DIAGNOSTIC PIPELINE LOGS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = SlateDim
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Terminal Output Window
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp)
                            .verticalScroll(scrollState)
                    ) {
                        Text(
                            text = job?.logOutput?.ifEmpty { "Awaiting pipeline worker spawn..." }
                                ?: "No logs.",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = CyberCyan.copy(alpha = 0.9f),
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // Bottom CTA
            Column(modifier = Modifier.fillMaxWidth()) {
                val isDone = job?.currentStage == PipelineStage.COMPLETED
                Button(
                    onClick = onLaunchViewer,
                    enabled = isDone,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDone) NeonEmerald else SlateDim
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("launch_reconstructed_viewer_button")
                ) {
                    Icon(
                        imageVector = if (isDone) Icons.Default.CheckCircle else Icons.Default.ViewInAr,
                        contentDescription = null,
                        tint = if (isDone) ObsidianBg else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isDone) "EXPLORE RECONSTRUCTED ROOM" else "COMPUTING GEOMETRY...",
                        fontWeight = FontWeight.Bold,
                        color = if (isDone) ObsidianBg else SlateMuted,
                        letterSpacing = 0.5.sp
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
