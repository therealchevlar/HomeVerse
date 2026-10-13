package com.example.engine3d

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke

data class ScreenPoint(
    val x: Float,
    val y: Float,
    val depth: Float,
    val visible: Boolean
)

data class RenderableFace(
    val p0: ScreenPoint,
    val p1: ScreenPoint,
    val p2: ScreenPoint,
    val avgDepth: Float,
    val shadedColor: Color,
    val strokeColor: Color?,
    val isSelectedRoom: Boolean
)

object Renderer3D {

    private val lightDir = Vector3(0.35f, 0.85f, 0.4f).normalize()
    private val ambientIntensity = 0.35f

    /**
     * Renders a 3D Mesh onto a Compose DrawScope.
     */
    fun render(
        drawScope: DrawScope,
        mesh: Mesh3D,
        camera: Camera3D,
        renderCeilings: Boolean = false,
        selectedRoomId: String? = null,
        showWireframe: Boolean = true,
        showPointCloud: Boolean = true
    ) {
        val width = drawScope.size.width
        val height = drawScope.size.height
        if (width <= 0 || height <= 0 || mesh.vertices.isEmpty()) return

        val aspect = width / height
        val viewMatrix = camera.getViewMatrix()
        val projMatrix = camera.getProjectionMatrix(aspect)
        val vpMatrix = projMatrix * viewMatrix

        // 1. Project vertices to screen space
        val screenVerts = Array(mesh.vertices.size) { i ->
            val v = mesh.vertices[i]
            // Project via 4x4 matrix
            val m = vpMatrix.m
            val x = v.x * m[0] + v.y * m[4] + v.z * m[8] + m[12]
            val y = v.x * m[1] + v.y * m[5] + v.z * m[9] + m[13]
            val z = v.x * m[2] + v.y * m[6] + v.z * m[10] + m[14]
            val w = v.x * m[3] + v.y * m[7] + v.z * m[11] + m[15]

            if (w > 0.05f) {
                val ndcX = x / w
                val ndcY = y / w
                val screenX = (ndcX + 1f) * 0.5f * width
                val screenY = (1f - ndcY) * 0.5f * height
                ScreenPoint(screenX, screenY, w, true)
            } else {
                ScreenPoint(0f, 0f, -1f, false)
            }
        }

        // 2. Filter & shade faces
        val renderableFaces = mutableListOf<RenderableFace>()

        for (face in mesh.faces) {
            // Check ceiling cutaway toggle for dollhouse
            if (face.isCeiling && !renderCeilings) continue

            val p0 = screenVerts[face.v0]
            val p1 = screenVerts[face.v1]
            val p2 = screenVerts[face.v2]

            if (!p0.visible || !p1.visible || !p2.visible) continue

            // Backface culling check in 2D screen space (signed area)
            val cross2D = (p1.x - p0.x) * (p2.y - p0.y) - (p1.y - p0.y) * (p2.x - p0.x)
            // Render double-sided for thin architectural interior walls so user inside room can see them
            val isSelected = selectedRoomId != null && face.roomId == selectedRoomId

            // Lighting computation
            val diff = (face.normal.dot(lightDir)).coerceIn(0f, 1f)
            val factor = ambientIntensity + (1f - ambientIntensity) * diff

            val baseCol = face.color
            val shaded = if (isSelected) {
                Color(
                    red = (baseCol.red * 0.4f + 0.6f * 0f).coerceIn(0f, 1f),
                    green = (baseCol.green * 0.4f + 0.6f * 0.94f).coerceIn(0f, 1f),
                    blue = (baseCol.blue * 0.4f + 0.6f * 1f).coerceIn(0f, 1f),
                    alpha = 0.85f
                )
            } else {
                Color(
                    red = (baseCol.red * factor).coerceIn(0f, 1f),
                    green = (baseCol.green * factor).coerceIn(0f, 1f),
                    blue = (baseCol.blue * factor).coerceIn(0f, 1f),
                    alpha = baseCol.alpha
                )
            }

            val stroke = if (isSelected) {
                Color(0xFF00F0FF)
            } else if (showWireframe) {
                Color(0x2800F0FF)
            } else null

            val avgDepth = (p0.depth + p1.depth + p2.depth) / 3f
            renderableFaces.add(RenderableFace(p0, p1, p2, avgDepth, shaded, stroke, isSelected))
        }

        // 3. Painter's algorithm: sort back-to-front
        renderableFaces.sortByDescending { it.avgDepth }

        // 4. Draw faces
        val facePath = Path()
        for (rf in renderableFaces) {
            facePath.reset()
            facePath.moveTo(rf.p0.x, rf.p0.y)
            facePath.lineTo(rf.p1.x, rf.p1.y)
            facePath.lineTo(rf.p2.x, rf.p2.y)
            facePath.close()

            drawScope.drawPath(path = facePath, color = rf.shadedColor, style = Fill)
            if (rf.strokeColor != null) {
                drawScope.drawPath(
                    path = facePath,
                    color = rf.strokeColor,
                    style = Stroke(width = if (rf.isSelectedRoom) 2f else 1f)
                )
            }
        }

        // 5. Render sparse point cloud particles
        if (showPointCloud && mesh.pointCloud.isNotEmpty()) {
            for (pt in mesh.pointCloud) {
                val v = pt.position
                val m = vpMatrix.m
                val x = v.x * m[0] + v.y * m[4] + v.z * m[8] + m[12]
                val y = v.x * m[1] + v.y * m[5] + v.z * m[9] + m[13]
                val w = v.x * m[3] + v.y * m[7] + v.z * m[11] + m[15]

                if (w > 0.05f) {
                    val ndcX = x / w
                    val ndcY = y / w
                    val sx = (ndcX + 1f) * 0.5f * width
                    val sy = (1f - ndcY) * 0.5f * height

                    drawScope.drawCircle(
                        color = pt.color,
                        radius = (pt.size * (15f / w)).coerceIn(1.5f, 5.5f),
                        center = Offset(sx, sy)
                    )
                }
            }
        }
    }

    /**
     * Draws 3D Waypoint pin markers for guided tour mode.
     */
    fun drawWaypointPin(
        drawScope: DrawScope,
        pos: Vector3,
        camera: Camera3D,
        label: String,
        isActive: Boolean
    ) {
        val width = drawScope.size.width
        val height = drawScope.size.height
        val vp = camera.getProjectionMatrix(width / height) * camera.getViewMatrix()

        val m = vp.m
        val x = pos.x * m[0] + pos.y * m[4] + pos.z * m[8] + m[12]
        val y = pos.x * m[1] + pos.y * m[5] + pos.z * m[9] + m[13]
        val w = pos.x * m[3] + pos.y * m[7] + pos.z * m[11] + m[15]

        if (w > 0.05f) {
            val sx = (x / w + 1f) * 0.5f * width
            val sy = (1f - y / w) * 0.5f * height

            val pinColor = if (isActive) Color(0xFF00F0FF) else Color(0xFF3B82F6)
            drawScope.drawCircle(
                color = pinColor,
                radius = if (isActive) 12f else 8f,
                center = Offset(sx, sy)
            )
            drawScope.drawCircle(
                color = Color.White,
                radius = if (isActive) 5f else 3f,
                center = Offset(sx, sy)
            )
        }
    }
}
