package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.CaptureSessionEntity
import com.example.data.model.FloorEntity
import com.example.data.model.HomeEntity
import com.example.data.model.ReconstructionJobEntity
import com.example.data.model.RoomEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HomeDao {
    @Query("SELECT * FROM homes ORDER BY createdAt DESC")
    fun getAllHomes(): Flow<List<HomeEntity>>

    @Query("SELECT * FROM homes WHERE id = :id")
    suspend fun getHomeById(id: String): HomeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHome(home: HomeEntity)

    @Update
    suspend fun updateHome(home: HomeEntity)

    @Query("DELETE FROM homes WHERE id = :id")
    suspend fun deleteHome(id: String)
}

@Dao
interface FloorDao {
    @Query("SELECT * FROM floors WHERE homeId = :homeId ORDER BY floorNumber ASC")
    fun getFloorsForHome(homeId: String): Flow<List<FloorEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFloor(floor: FloorEntity)

    @Query("DELETE FROM floors WHERE homeId = :homeId")
    suspend fun deleteFloorsForHome(homeId: String)
}

@Dao
interface RoomDao {
    @Query("SELECT * FROM rooms WHERE homeId = :homeId")
    fun getRoomsForHome(homeId: String): Flow<List<RoomEntity>>

    @Query("SELECT * FROM rooms WHERE id = :id")
    suspend fun getRoomById(id: String): RoomEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoom(room: RoomEntity)

    @Update
    suspend fun updateRoom(room: RoomEntity)

    @Query("DELETE FROM rooms WHERE id = :id")
    suspend fun deleteRoom(id: String)

    @Query("DELETE FROM rooms WHERE homeId = :homeId")
    suspend fun deleteRoomsForHome(homeId: String)
}

@Dao
interface CaptureDao {
    @Query("SELECT * FROM capture_sessions WHERE homeId = :homeId ORDER BY startedAt DESC")
    fun getSessionsForHome(homeId: String): Flow<List<CaptureSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: CaptureSessionEntity)
}

@Dao
interface ReconstructionJobDao {
    @Query("SELECT * FROM reconstruction_jobs WHERE homeId = :homeId ORDER BY startedAt DESC")
    fun getJobsForHome(homeId: String): Flow<List<ReconstructionJobEntity>>

    @Query("SELECT * FROM reconstruction_jobs WHERE id = :id")
    suspend fun getJobById(id: String): ReconstructionJobEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJob(job: ReconstructionJobEntity)

    @Update
    suspend fun updateJob(job: ReconstructionJobEntity)
}
