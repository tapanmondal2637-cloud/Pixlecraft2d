package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.data.AppDatabase
import com.example.data.WorldRepository
import com.example.game.GameEngine
import com.example.ui.GameView
import com.example.ui.MainMenuScreen
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var database: AppDatabase
    private lateinit var repository: WorldRepository
    private val gameEngine = GameEngine()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Initialize local SQLite Database and Repository
        database = AppDatabase.getDatabase(this)
        repository = WorldRepository(database.worldDao())

        setContent {
            MyApplicationTheme {
                // Collect existing worlds reactively
                val savedWorlds by repository.allWorlds.collectAsStateWithLifecycle(initialValue = emptyList())
                var activeScreen by remember { mutableStateOf("MENU") } // "MENU" or "GAME"

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    when (activeScreen) {
                        "MENU" -> {
                            MainMenuScreen(
                                savedWorlds = savedWorlds,
                                onCreateWorld = { name, seed, mode ->
                                    gameEngine.generateNewWorld(name, seed, mode)
                                    activeScreen = "GAME"
                                },
                                onLoadWorld = { entity ->
                                    gameEngine.loadFromEntity(entity)
                                    activeScreen = "GAME"
                                },
                                onDeleteWorld = { entity ->
                                    lifecycleScope.launch {
                                        repository.deleteWorld(entity)
                                    }
                                }
                            )
                        }
                        "GAME" -> {
                            GameView(
                                engine = gameEngine,
                                onPauseAndSave = {
                                    lifecycleScope.launch {
                                        val saveEntity = gameEngine.saveToEntity()
                                        val worldId = repository.saveWorld(saveEntity)
                                        gameEngine.worldId = worldId
                                        gameEngine.triggerPopup("World Save Completed!")
                                    }
                                },
                                onQuit = {
                                    // Quit to Main Menu without hard interruption
                                    activeScreen = "MENU"
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
