package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.SavedWorld
import com.example.game.Achievement
import com.example.game.GameStats
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainMenuScreen(
    savedWorlds: List<SavedWorld>,
    onCreateWorld: (name: String, seed: Long, mode: String) -> Unit,
    onLoadWorld: (SavedWorld) -> Unit,
    onDeleteWorld: (SavedWorld) -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var showStatsDialog by remember { mutableStateOf(false) }

    // Aggregate statistics across all saves
    val totalMined = savedWorlds.sumOf { it.statsText.split(":").firstOrNull()?.toIntOrNull() ?: 0 }
    val totalPlaced = savedWorlds.sumOf { it.statsText.split(":").getOrNull(1)?.toIntOrNull() ?: 0 }
    val totalKilled = savedWorlds.sumOf { it.statsText.split(":").getOrNull(2)?.toIntOrNull() ?: 0 }
    val totalCrafted = savedWorlds.sumOf { it.statsText.split(":").getOrNull(3)?.toIntOrNull() ?: 0 }
    val totalDeaths = savedWorlds.sumOf { it.statsText.split(":").getOrNull(4)?.toIntOrNull() ?: 0 }
    val totalTime = savedWorlds.sumOf { it.statsText.split(":").getOrNull(5)?.toLongOrNull() ?: 0L }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0D1B2A), Color(0xFF1B263B), Color(0xFF415A77))
                )
            )
            .padding(20.dp)
    ) {
        // Pixel starry background simulated
        Column(
            modifier = Modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // Epic Title Banner
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xAA1B263B)),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageIcons = Icons.Default.Terrain,
                        contentDescription = "Terrain Land Logo",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "BLOCK SANDBOX 2D",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFFE2E2E2),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Real-time gravity physics, daytime cycles, weather & animals!",
                        fontSize = 12.sp,
                        color = Color(0xFFA0C0D0),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons Panel
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = { showCreateDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .testTag("new_world_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Icon", tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("NEW WORLD", fontWeight = FontWeight.Bold, color = Color.White)
                }

                Button(
                    onClick = { showStatsDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .testTag("stats_achievements_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F4C81)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.EmojiEvents, contentDescription = "Achievements Icon", tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("STATS / TROPHIES", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Saved Worlds Header
            Text(
                text = "SAVED WORLD ARCHIVE (${savedWorlds.size})",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFA0C0D0),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                textAlign = TextAlign.Start
            )

            if (savedWorlds.isEmpty()) {
                Surface(
                    color = Color(0x33FFFFFF),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Save,
                            contentDescription = "No Saved worlds icon",
                            tint = Color(0xFF7E8D9B),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No saved worlds. Tap 'NEW WORLD' to generate a physical seed and begin survival!",
                            fontSize = 13.sp,
                            color = Color(0xFF7E8D9B),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(savedWorlds) { world ->
                        WorldSaveCard(
                            world = world,
                            onPlay = { onLoadWorld(world) },
                            onDelete = { onDeleteWorld(world) }
                        )
                    }
                }
            }
        }

        // --- Dialogs ---
        if (showCreateDialog) {
            CreateWorldDialog(
                onDismiss = { showCreateDialog = false },
                onCreate = { name, seedInput, mode ->
                    showCreateDialog = false
                    val seed = if (seedInput.isEmpty()) {
                        Random.nextLong()
                    } else {
                        seedInput.toLongOrNull() ?: seedInput.hashCode().toLong()
                    }
                    onCreateWorld(name.ifEmpty { "My World" }, seed, mode)
                }
            )
        }

        if (showStatsDialog) {
            StatsDialog(
                totalMined = totalMined,
                totalPlaced = totalPlaced,
                totalKilled = totalKilled,
                totalCrafted = totalCrafted,
                totalDeaths = totalDeaths,
                totalTime = totalTime,
                recentWorldCount = savedWorlds.size,
                onDismiss = { showStatsDialog = false }
            )
        }
    }
}

@Composable
fun WorldSaveCard(
    world: SavedWorld,
    onPlay: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B263B)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = world.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    val isCreative = world.gameMode == "CREATIVE"
                    SuggestionChip(
                        onClick = {},
                        label = { Text(world.gameMode, fontSize = 10.sp, color = if (isCreative) Color.Cyan else Color.Green) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = if (isCreative) Color(0x2200E5FF) else Color(0x2200E676)
                        )
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Seed: ${world.seed}",
                    fontSize = 11.sp,
                    color = Color(0xFFA0C0D0),
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Saved: ${SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(world.lastSaved))}",
                    fontSize = 11.sp,
                    color = Color(0xFF7E8D9B)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                // Delete button
                IconButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.testTag("delete_world_btn")
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Save", tint = Color(0xFFE57373))
                }

                // Play Button
                Button(
                    onClick = onPlay,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("play_world_btn")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Play Icon", tint = Color.DarkGray)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("PLAY", fontWeight = FontWeight.Bold, color = Color.DarkGray)
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete World?") },
            text = { Text("Are you absolutely sure you want to delete '${world.name}'? This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun CreateWorldDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, seed: String, mode: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var seed by remember { mutableStateOf("") }
    var selectedMode by remember { mutableStateOf("SURVIVAL") } // SURVIVAL vs CREATIVE

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1B263B)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "GENERATE WORLD",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("World Name", color = Color(0xFFA0C0D0)) },
                    placeholder = { Text("E.g. Survival Land") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF4CAF50),
                        unfocusedBorderColor = Color(0xFF7E8D9B)
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("world_name_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = seed,
                    onValueChange = { seed = it },
                    label = { Text("World Seed (Optional)", color = Color(0xFFA0C0D0)) },
                    placeholder = { Text("E.g. 159432 or leave blank") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF4CAF50),
                        unfocusedBorderColor = Color(0xFF7E8D9B)
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("world_seed_input")
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Game Mode selection row
                Text(text = "Game Mode:", fontSize = 14.sp, color = Color(0xFFA0C0D0), modifier = Modifier.align(Alignment.Start))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { selectedMode = "SURVIVAL" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedMode == "SURVIVAL") Color(0xFF00E676) else Color(0xFF2C3E50)
                        ),
                        modifier = Modifier.weight(1f).testTag("select_survival"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("SURVIVAL", color = if (selectedMode == "SURVIVAL") Color.Black else Color.White, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { selectedMode = "CREATIVE" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedMode == "CREATIVE") Color(0xFF00E5FF) else Color(0xFF2C3E50)
                        ),
                        modifier = Modifier.weight(1f).testTag("select_creative"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("CREATIVE", color = if (selectedMode == "CREATIVE") Color.Black else Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("CANCEL", color = Color(0xFFE57373))
                    }

                    Button(
                        onClick = { onCreate(name, seed, selectedMode) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                        modifier = Modifier.weight(1.5f).testTag("confirm_create_world_btn"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("GENERATE", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun StatsDialog(
    totalMined: Int,
    totalPlaced: Int,
    totalKilled: Int,
    totalCrafted: Int,
    totalDeaths: Int,
    totalTime: Long,
    recentWorldCount: Int,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1B2A)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "STATISTICS & RECORDS",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Stats stats
                StatLine("Total Worlds Saved", recentWorldCount.toString(), Icons.Default.Save, Color(0xFF42A5F5))
                StatLine("Blocks Extracted", totalMined.toString(), Icons.Default.Terrain, Color(0xFFFFA726))
                StatLine("Blocks Placed", totalPlaced.toString(), Icons.Default.AddBox, Color(0xFF66BB6A))
                StatLine("Monsters Slayed", totalKilled.toString(), Icons.Default.FlashOn, Color(0xFFEF5350))
                StatLine("Items Crafted", totalCrafted.toString(), Icons.Default.Build, Color(0xFFAB47BC))
                StatLine("Survival Deaths", totalDeaths.toString(), Icons.Default.SentimentVeryDissatisfied, Color(0xFFE57373))
                
                val fmtTime = if (totalTime < 60) "$totalTime s" else "${totalTime / 60}m ${totalTime % 60}s"
                StatLine("Total Duration Played", fmtTime, Icons.Default.Timer, Color(0xFF26A69A))

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF415A77)),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("BACK", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun StatLine(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Text(text = label, color = Color(0xFFA0C0D0), fontSize = 13.sp)
        Spacer(modifier = Modifier.weight(1f))
        Text(text = value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}

// ImageIcons custom alias for older Material icons compiler checks
@Composable
fun Icon(
    imageIcons: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    tint: Color,
    modifier: Modifier
) {
    Icon(imageVector = imageIcons, contentDescription = contentDescription, tint = tint, modifier = modifier)
}
