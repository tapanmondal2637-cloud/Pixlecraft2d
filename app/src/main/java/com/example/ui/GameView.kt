package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.game.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.floor
import kotlin.math.sqrt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GameView(
    engine: GameEngine,
    onPauseAndSave: () -> Unit,
    onQuit: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    // 1. Interactive States
    var showPauseMenu by remember { mutableStateOf(false) }
    var showInventoryMenu by remember { mutableStateOf(false) }
    var showChatWindow by remember { mutableStateOf(false) }
    var actionPlayMode by remember { mutableStateOf("MINE") } // "MINE" vs "PLACE"

    // Swing arm trigger
    var isAttacking by remember { mutableStateOf(false) }

    // Core Game Ticker loop (60 FPS approximate update ticks)
    LaunchedEffect(Unit) {
        var lastTime = System.currentTimeMillis()
        while (true) {
            val now = System.currentTimeMillis()
            val dt = (now - lastTime) / 1000f
            lastTime = now

            // Protect against massive lag spikes
            val clampedDt = dt.coerceAtMost(0.1f)
            engine.updateGame(clampedDt)

            delay(16) // tick every 16ms
        }
    }

    // Horizontal holding checks (D-pad movement loop)
    var moveLeftPressed by remember { mutableStateOf(false) }
    var moveRightPressed by remember { mutableStateOf(false) }

    LaunchedEffect(moveLeftPressed, moveRightPressed) {
        while (true) {
            when {
                moveLeftPressed -> {
                    engine.playerVx = -engine.runSpeed
                    engine.playerVx = -engine.runSpeed
                }
                moveRightPressed -> {
                    engine.playerVx = engine.runSpeed
                }
                else -> {
                    // Slide down to zero gently or instant stop
                    engine.playerVx = 0f
                }
            }
            delay(20)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // --- Core Game Canvas (Infinite procedural canvas map graphics) ---
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(actionPlayMode) {
                    // Capture tap holding or single clicks
                    detectTapGestures(
                        onPress = { offset ->
                            val gridPx = (size.width / 22f).coerceAtLeast(32f).coerceAtMost(64f)
                            val viewPivotX = size.width / 2f
                            val viewPivotY = size.height / 2f
                            
                            val cameraShiftX = viewPivotX - engine.playerX * gridPx - (engine.entityWidth / 2f) * gridPx
                            val cameraShiftY = viewPivotY - engine.playerY * gridPx - (engine.entityHeight / 2f) * gridPx

                            // Convert tap coordinates back to 2D block grid indexes
                            val targetWorldX = floor((offset.x - cameraShiftX) / gridPx).toInt()
                            val targetWorldY = floor((offset.y - cameraShiftY) / gridPx).toInt()

                            if (actionPlayMode == "MINE") {
                                // Start mining loop
                                try {
                                    var isHolding = true
                                    coroutineScope.launch {
                                        while (isHolding) {
                                            engine.handleMiningTick(0.1f, targetWorldX, targetWorldY)
                                            delay(100)
                                        }
                                        engine.clearMiningTarget()
                                    }
                                    tryAwaitRelease()
                                    isHolding = false
                                } catch (e: Exception) {
                                    engine.clearMiningTarget()
                                }
                            } else {
                                // PLACE block triggers instantly
                                engine.tryPlaceBlock(targetWorldX, targetWorldY)
                            }
                        }
                    )
                }
                .testTag("game_canvas")
        ) {
            GameRenderer.drawWorld(this, engine, size.width, size.height)
        }

        // --- Connection / Reconnect Notice banners ---
        val mp = engine.multiplayerManager
        val networkState = mp?.networkState?.collectAsState()?.value ?: NetworkState.OFFLINE
        if (networkState == NetworkState.RECONNECTING || networkState == NetworkState.ERROR) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (networkState == NetworkState.RECONNECTING) Color(0xFFD84315) else Color(0xFFC62828))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (networkState == NetworkState.RECONNECTING) "RECONNECTING TO LOBBY..." else "CONNECTION FAILURE! OFFLINE BACKUP ACTIVE.",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // --- Left-Top HUD (Vital stats overlays) ---
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .background(Color(0x7F0D1B2A), RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            // World Name Header
            Text(
                text = "${engine.worldName} (${engine.gameMode})",
                color = Color.White,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))

            // Health Bar Row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Favorite, contentDescription = "Health", tint = Color(0xFFEF5350), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .width(120.dp)
                        .height(8.dp)
                        .background(Color.DarkGray, RoundedCornerShape(4.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction = (engine.playerHealth / engine.maxHealth).coerceIn(0f, 1f))
                            .background(Color(0xFFE53935), RoundedCornerShape(4.dp))
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${engine.playerHealth.toInt()}",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Hunger Bar Row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Restaurant, contentDescription = "Hunger", tint = Color(0xFFFFB74D), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .width(120.dp)
                        .height(8.dp)
                        .background(Color.DarkGray, RoundedCornerShape(4.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction = (engine.playerHunger / engine.maxHunger).coerceIn(0f, 1f))
                            .background(Color(0xFFFFB300), RoundedCornerShape(4.dp))
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${engine.playerHunger.toInt()}",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Oxygen Bar (Swimming/Drowning)
            if (engine.oxygenLevel < engine.maxOxygen) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Opacity, contentDescription = "Oxygen Level bubbles", tint = Color(0xFF29B6F6), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .width(120.dp)
                            .height(8.dp)
                            .background(Color.DarkGray, RoundedCornerShape(4.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(fraction = (engine.oxygenLevel / engine.maxOxygen).coerceIn(0f, 1f))
                                .background(Color(0xFF03A9F4), RoundedCornerShape(4.dp))
                        )
                    }
                }
            }

            // Ambient sky time clock helper
            Spacer(modifier = Modifier.height(6.dp))
            val isRainingNote = if (engine.isRaining) "⛈️ RAINING" else "☀️ CLEAR"
            Text(
                text = "Time: ${ (engine.dayTimeSeconds).toInt() } s   $isRainingNote",
                fontSize = 10.sp,
                color = Color(0xFFA0C0D0),
                fontFamily = FontFamily.Monospace
            )
        }

        // --- Right-Top Overlay Controls (Pause & Saving) ---
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (mp != null) {
                IconButton(
                    onClick = { showChatWindow = true },
                    modifier = Modifier
                        .background(Color(0x7F0D1B2A), CircleShape)
                        .testTag("chat_btn")
                ) {
                    Icon(Icons.Default.Chat, contentDescription = "Online Multi Chat", tint = Color(0xFF00FFCC))
                }
            }

            IconButton(
                onClick = { showPauseMenu = true },
                modifier = Modifier
                    .background(Color(0x7F0D1B2A), CircleShape)
                    .testTag("pause_btn")
            ) {
                Icon(Icons.Default.Pause, contentDescription = "Pause and Save Menu", tint = Color.White)
            }

            IconButton(
                onClick = { showInventoryMenu = true },
                modifier = Modifier
                    .background(Color(0x7F0D1B2A), CircleShape)
                    .testTag("inventory_btn")
            ) {
                Icon(Icons.Default.Inventory, contentDescription = "Inventory Panel", tint = Color.White)
            }
        }

        // --- Center Banner Popups ---
        if (engine.activePopupMessage.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xCC000000))
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text(
                    text = engine.activePopupMessage,
                    color = Color.Yellow,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // --- Action Mode float selector overlay (Action toggle MINE vs PLACEMENT) ---
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(16.dp)
                .background(Color(0x991B263B), RoundedCornerShape(12.dp))
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = { actionPlayMode = "MINE" },
                    modifier = Modifier
                        .size(48.dp)
                        .background(if (actionPlayMode == "MINE") Color(0xFF00E676) else Color(0x33FFFFFF), RoundedCornerShape(8.dp))
                        .testTag("set_mine_mode")
                ) {
                    Icon(Icons.Default.FlashOn, contentDescription = "Activate Mining", tint = if (actionPlayMode == "MINE") Color.Black else Color.White)
                }
                Text("MINE", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = { actionPlayMode = "PLACE" },
                    modifier = Modifier
                        .size(48.dp)
                        .background(if (actionPlayMode == "PLACE") Color(0xFF00FFCC) else Color(0x33FFFFFF), RoundedCornerShape(8.dp))
                        .testTag("set_place_mode")
                ) {
                    Icon(Icons.Default.AddBox, contentDescription = "Activate Placement", tint = if (actionPlayMode == "PLACE") Color.Black else Color.White)
                }
                Text("PLACE", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }

            // Quick Eat button if selected slot has edible item
            val holdingItem = engine.inventory[engine.selectedHotbarIndex]
            if (holdingItem != null && holdingItem.itemType.foodValue > 0f) {
                Divider(color = Color.Gray, modifier = Modifier.width(36.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = { engine.eatSelectedFood() },
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color(0xFFFFB300), CircleShape)
                            .testTag("eat_btn")
                    ) {
                        Icon(Icons.Default.Restaurant, contentDescription = "Eat Food", tint = Color.Black)
                    }
                    Text("EAT", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // --- Bottom Horizontal Active Hotbar ---
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 96.dp)
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .background(Color(0xBB1B263B), RoundedCornerShape(12.dp))
                    .padding(6.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Hotbar slots: 0 to 7
                    for (i in 0..7) {
                        val isSelected = engine.selectedHotbarIndex == i
                        val slot = engine.inventory[i]
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) Color(0xFF00E676) else Color(0x44FFFFFF),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .background(if (isSelected) Color(0x7F2E7D32) else Color(0x33FFFFFF))
                                .clickable { engine.selectedHotbarIndex = i }
                                .testTag("hotbar_slot_$i"),
                            contentAlignment = Alignment.Center
                        ) {
                            if (slot != null) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    // Procedural icon preview block
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .background(
                                                if (slot.itemType.isBlock) BlockType.fromId(slot.itemType.blockId).color else Color(0xFFD7CCC8),
                                                RoundedCornerShape(3.dp)
                                            )
                                    )
                                    Text(
                                        text = "${slot.count}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- Mobile Play Touch D-pads (Movements on left-bottom. Attacks/Jumps on right-bottom) ---
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // MOVE LEFT
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(Color(0x991B263B), CircleShape)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    moveLeftPressed = true
                                    try {
                                        awaitRelease()
                                    } finally {
                                        moveLeftPressed = false
                                    }
                                }
                            )
                        }
                        .testTag("dpad_left"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Walk Left",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // MOVE RIGHT
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(Color(0x991B263B), CircleShape)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    moveRightPressed = true
                                    try {
                                        awaitRelease()
                                    } finally {
                                        moveRightPressed = false
                                    }
                                }
                            )
                        }
                        .testTag("dpad_right"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.ArrowForward,
                        contentDescription = "Walk Right",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ATTACK SWIPE ACTION
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(Color(0x99C62828), CircleShape)
                        .clickable {
                            engine.attackInDirection(facingLeft = engine.playerVx < 0f)
                        }
                        .testTag("action_attack"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.FlashOn,
                        contentDescription = "Swing weapon strike",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // JUMP JUMP ACTION
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(Color(0x991B263B), CircleShape)
                        .clickable { engine.tryJump() }
                        .testTag("action_jump"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.ArrowUpward,
                        contentDescription = "Jump Climb Climb",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }

        // --- PAUSE SAVING MENU ---
        if (showPauseMenu) {
            PauseMenuDialog(
                isMultiplayer = (mp != null),
                onDismiss = { showPauseMenu = false },
                onSave = {
                    showPauseMenu = false
                    onPauseAndSave()
                },
                onQuit = {
                    showPauseMenu = false
                    onQuit()
                }
            )
        }

        // --- SCREEN INVENTORY & CRAFTING OVERLAY ---
        if (showInventoryMenu) {
            InventoryAndCraftingDialog(
                engine = engine,
                onDismiss = { showInventoryMenu = false }
            )
        }

        // --- SCREEN ONLINE CHAT SYSTEM ---
        if (showChatWindow && mp != null) {
            InGameChatDialog(
                manager = mp,
                onDismiss = { showChatWindow = false }
            )
        }
    }
}

@Composable
fun PauseMenuDialog(
    isMultiplayer: Boolean = false,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onQuit: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1B29)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.padding(16.dp).fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "GAME PAUSED",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White
                )

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF415A77)),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Play")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("RESUME", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onSave,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                    modifier = Modifier.fillMaxWidth().testTag("save_world_btn"),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        if (isMultiplayer) Icons.Default.CloudUpload else Icons.Default.Save,
                        contentDescription = "Save DB"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (isMultiplayer) "SAVE WORLD WITH CLOUD" else "SAVE TO DATABASE",
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = onQuit,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE57373)),
                    modifier = Modifier.fillMaxWidth().testTag("quit_world_btn"),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.ExitToApp, contentDescription = "Quit")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("QUIT TO MAIN MENU", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun InventoryAndCraftingDialog(
    engine: GameEngine,
    onDismiss: () -> Unit
) {
    var selectedIndexForSwap by remember { mutableStateOf(-1) }
    var expandedRecipeTab by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1B29)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth(1.23f)
                .fillMaxHeight(0.9f)
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxSize()
            ) {
                // Header Tabs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (expandedRecipeTab) "🛠️ CRAFTING LOG" else "🎒 PLAYER BACKPACK",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )

                    TextButton(
                        onClick = { expandedRecipeTab = !expandedRecipeTab },
                        modifier = Modifier.testTag("toggle_crafting_tab")
                    ) {
                        Text(
                            text = if (expandedRecipeTab) "VIEW BACKPACK" else "OPEN FORGE RECIPES",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00FFCC)
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close overlay", tint = Color.LightGray)
                    }
                }

                Divider(color = Color.Gray, modifier = Modifier.padding(vertical = 8.dp))

                if (expandedRecipeTab) {
                    // Crafting side list
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (engine.isNearCraftingTable) "🟢 Workbench nearby (Advanced Recipes Unlocked)" else "🔴 Standing in wilderness (Basic Recipes Only)",
                            fontSize = 11.sp,
                            color = if (engine.isNearCraftingTable) Color.Green else Color.Yellow,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(1),
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val filteredRecipes = CraftingRecipe.RECIPES.filter {
                                !it.requiresCraftingTable || engine.isNearCraftingTable
                            }

                            itemsIndexed(filteredRecipes) { idx, recipe ->
                                val craftable = engine.hasIngredients(recipe)
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0x33FFFFFF)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("recipe_item_$idx")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Result Preview Block
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(
                                                    if (recipe.result.isBlock) BlockType.fromId(recipe.result.blockId).color else Color(0xFFD7CCC8),
                                                    RoundedCornerShape(6.dp)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("${recipe.resultCount}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(recipe.result.itemName, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                            // Requirements
                                            val reqText = recipe.ingredients.entries.joinToString(", ") { "${it.key.itemName} x${it.value}" }
                                            Text("Needs: $reqText", fontSize = 11.sp, color = Color.LightGray)
                                        }

                                        Button(
                                            onClick = { engine.craftItem(recipe) },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (craftable) Color(0xFF00E676) else Color.DarkGray
                                            ),
                                            enabled = craftable,
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.testTag("craft_action_btn_$idx")
                                        ) {
                                            Text("CRAFT", fontSize = 12.sp, color = if (craftable) Color.Black else Color.Gray, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Inventory 30-slots grid
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "First 8 slots correspond to your active hotbar keys at game bottom. Tap a slot to highlight, and tap another slot to SWAP coordinates instantly.",
                            fontSize = 11.sp,
                            color = Color(0xFFA0C0D0),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(5),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            itemsIndexed(engine.inventory) { index, slot ->
                                val isSelected = selectedIndexForSwap == index
                                Box(
                                    modifier = Modifier
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) Color(0xFF00E676) else Color(0x33FFFFFF),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .background(if (index < 8) Color(0x22FFFFFF) else Color(0x11FFFFFF))
                                        .clickable {
                                            if (selectedIndexForSwap == -1) {
                                                selectedIndexForSwap = index
                                            } else {
                                                // Swap triggers
                                                engine.swapInventorySlots(selectedIndexForSwap, index)
                                                selectedIndexForSwap = -1
                                            }
                                        }
                                        .testTag("inventory_slot_$index"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (slot != null) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .background(
                                                        if (slot.itemType.isBlock) BlockType.fromId(slot.itemType.blockId).color else Color(0xFFD7CCC8),
                                                        RoundedCornerShape(4.dp)
                                                    )
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "${slot.count}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    } else {
                                        // Empty slot label
                                        Text(text = "$index", fontSize = 10.sp, color = Color(0xFF555555))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InGameChatDialog(
    manager: MultiplayerManager,
    onDismiss: () -> Unit
) {
    var txtMsg by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Scroll to latest message
    LaunchedEffect(manager.chatMessages.size) {
        if (manager.chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(manager.chatMessages.size - 1)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1B29)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.75f)
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Chat, contentDescription = "Chat Logs", tint = Color(0xFF00FFCC))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ONLINE CHAT (Room: ${manager.currentRoomCode})",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.weight(1f)
                    )
                    
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close Chat", tint = Color.White)
                    }
                }

                Divider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 4.dp))

                // Chat Logs Scroll List
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color(0x33000000), RoundedCornerShape(6.dp))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(manager.chatMessages) { chat ->
                        if (chat.isSystem) {
                            Text(
                                text = "📣 ${chat.text}",
                                color = Color(0xFFFFB74D),
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            val isMe = chat.senderName == manager.playerName
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isMe) Color(0xFF1B4965) else Color(0xFF2B2D42))
                                        .padding(8.dp)
                                ) {
                                    Column {
                                        Text(
                                            text = chat.senderName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = if (isMe) Color(0xFF00FFCC) else Color(0xFFFF9E00)
                                        )
                                        Text(
                                            text = chat.text,
                                            color = Color.White,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Msg Input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextField(
                        value = txtMsg,
                        onValueChange = { txtMsg = it },
                        placeholder = { Text("Compose message...", color = Color.Gray, fontSize = 12.sp) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0x19FFFFFF),
                            unfocusedContainerColor = Color(0x0CFFFFFF),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .testTag("chat_input"),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            if (txtMsg.trim().isNotEmpty()) {
                                manager.sendChatMessage(txtMsg)
                                txtMsg = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .height(52.dp)
                            .testTag("send_chat_btn")
                    ) {
                        Text("SEND", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
