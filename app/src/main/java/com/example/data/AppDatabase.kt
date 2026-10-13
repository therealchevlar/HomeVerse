package com.example.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.data.dao.CaptureDao
import com.example.data.dao.FloorDao
import com.example.data.dao.HomeDao
import com.example.data.dao.ReconstructionJobDao
import com.example.data.dao.RoomDao
import com.example.data.model.CaptureSessionEntity
import com.example.data.model.FloorEntity
import com.example.data.model.HomeEntity
import com.example.data.model.PipelineStage
import com.example.data.model.ReconstructionJobEntity
import com.example.data.model.ReconstructionStatus
import com.example.data.model.RoomEntity
import com.example.data.model.RoomType

class Converters {
    @TypeConverter
    fun fromRoomType(value: RoomType): String = value.name

    @TypeConverter
    fun toRoomType(value: String): RoomType = try {
        RoomType.valueOf(value)
    } catch (_: Exception) {
        RoomType.CUSTOM
    }

    @TypeConverter
    fun fromReconstructionStatus(value: ReconstructionStatus): String = value.name

    @TypeConverter
    fun toReconstructionStatus(value: String): ReconstructionStatus = try {
        ReconstructionStatus.valueOf(value)
    } catch (_: Exception) {
        ReconstructionStatus.NOT_STARTED
    }

    @TypeConverter
    fun fromPipelineStage(value: PipelineStage): String = value.name

    @TypeConverter
    fun toPipelineStage(value: String): PipelineStage = try {
        PipelineStage.valueOf(value)
    } catch (_: Exception) {
        PipelineStage.VALIDATE_CAPTURE
    }
}

@Database(
    entities = [
        HomeEntity::class,
        FloorEntity::class,
        RoomEntity::class,
        CaptureSessionEntity::class,
        ReconstructionJobEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun homeDao(): HomeDao
    abstract fun floorDao(): FloorDao
    abstract fun roomDao(): RoomDao
    abstract fun captureDao(): CaptureDao
    abstract fun reconstructionJobDao(): ReconstructionJobDao
}
