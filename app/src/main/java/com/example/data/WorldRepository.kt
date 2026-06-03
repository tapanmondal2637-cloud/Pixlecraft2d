package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class WorldRepository(private val worldDao: WorldDao) {
    val allWorlds: Flow<List<SavedWorld>> = worldDao.getAllWorldsFlow()

    suspend fun loadWorld(id: Int): SavedWorld? = withContext(Dispatchers.IO) {
        worldDao.getWorldById(id)
    }

    suspend fun saveWorld(world: SavedWorld): Int = withContext(Dispatchers.IO) {
        val id = worldDao.insertWorld(world)
        id.toInt()
    }

    suspend fun updateWorld(world: SavedWorld) = withContext(Dispatchers.IO) {
        worldDao.updateWorld(world)
    }

    suspend fun deleteWorld(world: SavedWorld) = withContext(Dispatchers.IO) {
        worldDao.deleteWorld(world)
    }
}
