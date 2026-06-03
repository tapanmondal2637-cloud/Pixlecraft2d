package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WorldDao {
    @Query("SELECT * FROM saved_worlds ORDER BY lastSaved DESC")
    fun getAllWorldsFlow(): Flow<List<SavedWorld>>

    @Query("SELECT * FROM saved_worlds WHERE id = :worldId LIMIT 1")
    suspend fun getWorldById(worldId: Int): SavedWorld?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorld(world: SavedWorld): Long

    @Update
    suspend fun updateWorld(world: SavedWorld)

    @Delete
    suspend fun deleteWorld(world: SavedWorld)
}
