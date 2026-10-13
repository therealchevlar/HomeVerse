package com.example.engine3d

import androidx.compose.ui.graphics.Color
import kotlin.random.Random

/**
 * Procedural spatial geometry builder for rooms, architectural elements,
 * dollhouse cutaways, and furniture.
 */
object SpatialPrimitives {

    /**
     * Builds a full 3D room with floor, walls, and removable ceiling.
     */
    fun createRoomMesh(
        roomId: String,
        name: String,
        originX: Float,
        originZ: Float,
        width: Float,
        length: Float,
        height: Float = 2.8f,
        wallColor: Color = Color(0xFF1E293B),
        floorColor: Color = Color(0xFF0F172A),
        ceilingColor: Color = Color(0x33334155),
        hasDoorwayOnSouth: Boolean = false
    ): Mesh3D {
        val verts = mutableListOf<Vector3>()
        val faces = mutableListOf<Face3D>()

        val x0 = originX
        val x1 = originX + width
        val z0 = originZ
        val z1 = originZ + length
        val y0 = 0f
        val y1 = height

        // 8 corner vertices:
        // 0: (x0, y0, z0) - Bottom Near Left
        // 1: (x1, y0, z0) - Bottom Near Right
        // 2: (x1, y0, z1) - Bottom Far Right
        // 3: (x0, y0, z1) - Bottom Far Left
        // 4: (x0, y1, z0) - Top Near Left
        // 5: (x1, y1, z0) - Top Near Right
        // 6: (x1, y1, z1) - Top Far Right
        // 7: (x0, y1, z1) - Top Far Left

        val baseIndex = 0
        verts.add(Vector3(x0, y0, z0)) // 0
        verts.add(Vector3(x1, y0, z0)) // 1
        verts.add(Vector3(x1, y0, z1)) // 2
        verts.add(Vector3(x0, y0, z1)) // 3
        verts.add(Vector3(x0, y1, z0)) // 4
        verts.add(Vector3(x1, y1, z0)) // 5
        verts.add(Vector3(x1, y1, z1)) // 6
        verts.add(Vector3(x0, y1, z1)) // 7

        fun addQuad(v0: Int, v1: Int, v2: Int, v3: Int, normal: Vector3, color: Color, isCeiling: Boolean = false) {
            faces.add(Face3D(v0, v1, v2, normal, color, isCeiling, roomId))
            faces.add(Face3D(v0, v2, v3, normal, color, isCeiling, roomId))
        }

        // Floor (normal UP)
        addQuad(baseIndex + 0, baseIndex + 1, baseIndex + 2, baseIndex + 3, Vector3.UP, floorColor)

        // Ceiling (removable for dollhouse, normal DOWN)
        addQuad(baseIndex + 7, baseIndex + 6, baseIndex + 5, baseIndex + 4, Vector3(0f, -1f, 0f), ceilingColor, isCeiling = true)

        // North Wall (Z = z1, facing -Z)
        addQuad(baseIndex + 3, baseIndex + 2, baseIndex + 6, baseIndex + 7, Vector3(0f, 0f, -1f), wallColor)

        // West Wall (X = x0, facing +X)
        addQuad(baseIndex + 0, baseIndex + 3, baseIndex + 7, baseIndex + 4, Vector3(1f, 0f, 0f), wallColor)

        // East Wall (X = x1, facing -X)
        addQuad(baseIndex + 2, baseIndex + 1, baseIndex + 5, baseIndex + 6, Vector3(-1f, 0f, 0f), wallColor)

        // South Wall (Z = z0, facing +Z) - if hasDoorway, leave doorway opening
        if (hasDoorwayOnSouth) {
            // Cut south wall into two side pillars and top lintel
            val doorWidth = 1.0f
            val doorHeight = 2.1f
            val midX = (x0 + x1) / 2f
            val doorLeft = midX - doorWidth / 2f
            val doorRight = midX + doorWidth / 2f

            val idxStart = verts.size
            verts.add(Vector3(doorLeft, y0, z0))       // +0
            verts.add(Vector3(doorRight, y0, z0))      // +1
            verts.add(Vector3(doorLeft, doorHeight, z0)) // +2
            verts.add(Vector3(doorRight, doorHeight, z0))// +3

            // Left pillar
            addQuad(baseIndex + 1, idxStart + 1, idxStart + 3, baseIndex + 5, Vector3(0f, 0f, 1f), wallColor)
            // Right pillar
            addQuad(idxStart + 0, baseIndex + 0, baseIndex + 4, idxStart + 2, Vector3(0f, 0f, 1f), wallColor)
            // Lintel above door
            addQuad(idxStart + 2, idxStart + 3, baseIndex + 5, baseIndex + 4, Vector3(0f, 0f, 1f), wallColor)
        } else {
            addQuad(baseIndex + 1, baseIndex + 0, baseIndex + 4, baseIndex + 5, Vector3(0f, 0f, 1f), wallColor)
        }

        // Generate sparse point cloud to represent reconstruction features along the walls
        val pointCloud = mutableListOf<Point3D>()
        val rnd = Random(roomId.hashCode())
        for (i in 0..40) {
            val px = x0 + rnd.nextFloat() * width
            val py = y0 + 0.2f + rnd.nextFloat() * (height - 0.4f)
            val pz = z0 + rnd.nextFloat() * length
            pointCloud.add(Point3D(Vector3(px, py, pz), Color(0xFF00F0FF), size = 2.5f))
        }

        return Mesh3D(
            name = name,
            vertices = verts,
            faces = faces,
            pointCloud = pointCloud,
            bounds = BoundingBox(Vector3(x0, y0, z0), Vector3(x1, y1, z1))
        )
    }

    /**
     * Builds an architectural box (e.g., table, sofa, bed, counter).
     */
    fun createBox(
        name: String,
        roomId: String,
        cx: Float,
        cy: Float,
        cz: Float,
        sx: Float,
        sy: Float,
        sz: Float,
        color: Color
    ): Mesh3D {
        val verts = mutableListOf<Vector3>()
        val faces = mutableListOf<Face3D>()

        val hx = sx / 2f
        val hy = sy / 2f
        val hz = sz / 2f

        verts.add(Vector3(cx - hx, cy - hy, cz - hz)) // 0
        verts.add(Vector3(cx + hx, cy - hy, cz - hz)) // 1
        verts.add(Vector3(cx + hx, cy - hy, cz + hz)) // 2
        verts.add(Vector3(cx - hx, cy - hy, cz + hz)) // 3
        verts.add(Vector3(cx - hx, cy + hy, cz - hz)) // 4
        verts.add(Vector3(cx + hx, cy + hy, cz - hz)) // 5
        verts.add(Vector3(cx + hx, cy + hy, cz + hz)) // 6
        verts.add(Vector3(cx - hx, cy + hy, cz + hz)) // 7

        fun quad(v0: Int, v1: Int, v2: Int, v3: Int, n: Vector3) {
            faces.add(Face3D(v0, v1, v2, n, color, false, roomId))
            faces.add(Face3D(v0, v2, v3, n, color, false, roomId))
        }

        // Top
        quad(4, 5, 6, 7, Vector3.UP)
        // Bottom
        quad(3, 2, 1, 0, Vector3(0f, -1f, 0f))
        // Front (+Z)
        quad(3, 7, 6, 2, Vector3(0f, 0f, 1f))
        // Back (-Z)
        quad(1, 5, 4, 0, Vector3(0f, 0f, -1f))
        // Left (-X)
        quad(0, 4, 7, 3, Vector3(-1f, 0f, 0f))
        // Right (+X)
        quad(2, 6, 5, 1, Vector3(1f, 0f, 0f))

        return Mesh3D(
            name = name,
            vertices = verts,
            faces = faces,
            bounds = BoundingBox(Vector3(cx - hx, cy - hy, cz - hz), Vector3(cx + hx, cy + hy, cz + hz))
        )
    }

    /**
     * Combines multiple meshes into a single unified spatial mesh.
     */
    fun combineMeshes(name: String, meshes: List<Mesh3D>): Mesh3D {
        val totalVerts = mutableListOf<Vector3>()
        val totalFaces = mutableListOf<Face3D>()
        val totalPoints = mutableListOf<Point3D>()

        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var minZ = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        var maxZ = -Float.MAX_VALUE

        for (mesh in meshes) {
            val offset = totalVerts.size
            totalVerts.addAll(mesh.vertices)

            for (face in mesh.faces) {
                totalFaces.add(
                    Face3D(
                        v0 = face.v0 + offset,
                        v1 = face.v1 + offset,
                        v2 = face.v2 + offset,
                        normal = face.normal,
                        color = face.color,
                        isCeiling = face.isCeiling,
                        roomId = face.roomId
                    )
                )
            }

            totalPoints.addAll(mesh.pointCloud)

            minX = minOf(minX, mesh.bounds.min.x)
            minY = minOf(minY, mesh.bounds.min.y)
            minZ = minOf(minZ, mesh.bounds.min.z)
            maxX = maxOf(maxX, mesh.bounds.max.x)
            maxY = maxOf(maxY, mesh.bounds.max.y)
            maxZ = maxOf(maxZ, mesh.bounds.max.z)
        }

        return Mesh3D(
            name = name,
            vertices = totalVerts,
            faces = totalFaces,
            pointCloud = totalPoints,
            bounds = BoundingBox(Vector3(minX, minY, minZ), Vector3(maxX, maxY, maxZ))
        )
    }
}
