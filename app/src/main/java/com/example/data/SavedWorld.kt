package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_worlds")
data class SavedWorld(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val seed: Long,
    val gameMode: String, // "SURVIVAL", "CREATIVE"
    val dayTime: Float,
    val playerX: Float,
    val playerY: Float,
    val playerHp: Float,
    val playerHunger: Float,
    val worldWidth: Int,
    val worldHeight: Int,
    val worldBlocksText: String, // Comma-separated block IDs
    val inventoryText: String, // comma separated "itemId_count" for each hotbar/inventory slot
    val achievementsText: String, // split by逗号 (comma)
    val statsText: String, // blocksMined:blocksPlaced:mobsKilled:craftedCount:deathsCount:timePlayedSeconds
    val isRaining: Boolean,
    val lastSaved: Long = System.currentTimeMillis()
)
