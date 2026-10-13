package com.example.reconstruction

import android.content.Context
import com.example.engine3d.Vector3
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class FrameMetadata(
    val frameIndex: Int,
    val timestamp: Long,
    val gyroYaw: Float,
    val gyroPitch: Float,
    val gyroRoll: Float,
    val estimatedPose: Vector3,
    val focalLengthMm: Float = 26f,
    val lightLux: Float = 450f
)

data class CapturePackage(
    val sessionId: String,
    val roomId: String,
    val homeId: String,
    val createdAt: Long,
    val frames: List<FrameMetadata>,
    val packageDirectoryPath: String
) {
    fun toJson(): String {
        val root = JSONObject()
        root.put("sessionId", sessionId)
        root.put("roomId", roomId)
        root.put("homeId", homeId)
        root.put("createdAt", createdAt)
        root.put("frameCount", frames.size)
        root.put("formatVersion", "1.0.0")

        val framesArray = JSONArray()
        for (f in frames) {
            val fObj = JSONObject()
            fObj.put("index", f.frameIndex)
            fObj.put("timestamp", f.timestamp)
            fObj.put("yaw", f.gyroYaw)
            fObj.put("pitch", f.gyroPitch)
            fObj.put("roll", f.gyroRoll)

            val poseObj = JSONObject()
            poseObj.put("x", f.estimatedPose.x)
            poseObj.put("y", f.estimatedPose.y)
            poseObj.put("z", f.estimatedPose.z)
            fObj.put("pose", poseObj)

            fObj.put("focalLengthMm", f.focalLengthMm)
            fObj.put("lightLux", f.lightLux)
            framesArray.put(fObj)
        }
        root.put("frames", framesArray)
        return root.toString(2)
    }

    companion object {
        fun saveToDisk(context: Context, capturePackage: CapturePackage): File {
            val dir = File(context.filesDir, "captures/${capturePackage.sessionId}")
            if (!dir.exists()) dir.mkdirs()
            val manifestFile = File(dir, "capture_manifest.json")
            manifestFile.writeText(capturePackage.toJson())
            return manifestFile
        }
    }
}
