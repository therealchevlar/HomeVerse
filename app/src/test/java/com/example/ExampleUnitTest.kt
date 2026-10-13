package com.example

import com.example.engine3d.BoundingBox
import com.example.engine3d.CollisionDetector
import com.example.engine3d.Matrix4x4
import com.example.engine3d.SpatialPrimitives
import com.example.engine3d.Vector3
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

  @Test
  fun testVector3Operations() {
    val a = Vector3(1f, 2f, 3f)
    val b = Vector3(4f, 5f, 6f)
    val c = a + b
    assertEquals(5f, c.x, 0.001f)
    assertEquals(7f, c.y, 0.001f)
    assertEquals(9f, c.z, 0.001f)

    val dot = a.dot(b) // 1*4 + 2*5 + 3*6 = 4 + 10 + 18 = 32
    assertEquals(32f, dot, 0.001f)

    val norm = Vector3(3f, 0f, 0f).normalize()
    assertEquals(1f, norm.x, 0.001f)
    assertEquals(1f, norm.length(), 0.001f)
  }

  @Test
  fun testMatrix4x4Transforms() {
    val t = Matrix4x4.translation(5f, 10f, -2f)
    val p = Vector3(0f, 0f, 0f)
    val transformed = t.transformPoint(p)
    assertEquals(5f, transformed.x, 0.001f)
    assertEquals(10f, transformed.y, 0.001f)
    assertEquals(-2f, transformed.z, 0.001f)
  }

  @Test
  fun testCollisionClamping() {
    val bounds = BoundingBox(Vector3(-5f, 0f, -5f), Vector3(5f, 3f, 5f))
    val outsidePos = Vector3(10f, 1.6f, -10f)
    val clamped = CollisionDetector.clampToHousePerimeter(Vector3.ZERO, outsidePos, bounds, 0.5f)
    assertTrue(clamped.x <= 4.5f)
    assertTrue(clamped.z >= -4.5f)
    assertEquals(1.6f, clamped.y, 0.001f)
  }

  @Test
  fun testRoomMeshGeneration() {
    val roomMesh = SpatialPrimitives.createRoomMesh(
      roomId = "test-room",
      name = "Living Room",
      originX = 0f,
      originZ = 0f,
      width = 5f,
      length = 4f,
      height = 2.8f
    )
    assertTrue(roomMesh.vertices.isNotEmpty())
    assertTrue(roomMesh.faces.isNotEmpty())
    assertNotNull(roomMesh.bounds)
  }
}
