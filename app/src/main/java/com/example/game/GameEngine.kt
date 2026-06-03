package com.example.game

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import com.example.data.SavedWorld
import java.util.UUID
import kotlin.math.*
import kotlin.random.Random

class GameEngine {

    // 1. Immutable Map Dimensions
    val width = 120
    val height = 60

    // 2. Playable Game Variables
    var worldBlocks by mutableStateOf(Array(width) { IntArray(height) { BlockType.AIR.id } })
    var playerX by mutableFloatStateOf(60f)
    var playerY by mutableFloatStateOf(15f)
    var playerVx by mutableFloatStateOf(0f)
    var playerVy by mutableFloatStateOf(0f)
    
    var playerHealth by mutableFloatStateOf(100f)
    val maxHealth = 100f
    var playerHunger by mutableFloatStateOf(100f)
    val maxHunger = 100f
    
    var oxygenLevel by mutableFloatStateOf(100f)
    val maxOxygen = 100f

    var dayTimeSeconds by mutableFloatStateOf(180f) // Day cycle length = 600s
    val dayCycleLength = 600f

    var isRaining by mutableStateOf(false)
    var rainTimerSeconds by mutableFloatStateOf(0f)

    var gameMode by mutableStateOf("SURVIVAL") // SURVIVAL or CREATIVE
    var currentSeed by mutableLongStateOf(0L)
    var worldName by mutableStateOf("New World")
    var worldId = 0 // 0 means not saved yet

    // Inventory: 30 slots (8 hotbar slots + 22 cargo slots)
    var inventory = mutableStateListOf<InventorySlot?>().apply {
        repeat(30) { add(null) }
    }
    var selectedHotbarIndex by mutableIntStateOf(0)

    // Entities
    val mobs = mutableStateListOf<Mob>()
    val particles = mutableStateListOf<Particle>()

    // Statistics & Achievements
    var stats by mutableStateOf(GameStats())
    val achievements = mutableStateListOf<Achievement>().apply {
        addAll(Achievement.DEFAULT_ACHIEVEMENTS)
    }

    // Interactive mining state
    var miningTargetX by mutableIntStateOf(-1)
    var miningTargetY by mutableIntStateOf(-1)
    var miningProgress by mutableFloatStateOf(0f)

    // Temporary variables for placement, messages, crafting near table
    var isNearCraftingTable by mutableStateOf(false)
    var activePopupMessage by mutableStateOf("")
    var popupTimer by mutableFloatStateOf(0f)

    // Physics Constants
    val blockPhysicalSize = 1.0f // Block is 1.0x1.0 units
    val entityWidth = 0.75f
    val entityHeight = 1.8f
    val gravity = 32f // Units/sec^2
    val swimGravity = 6f
    val runSpeed = 5.5f
    val jumpForce = -11.5f
    val swimUpForce = -4.5f

    // 3. Initiate World Generation
    fun generateNewWorld(name: String, seed: Long, mode: String) {
        worldName = name
        currentSeed = seed
        gameMode = mode
        worldId = 0
        playerHealth = 100f
        playerHunger = 100f
        oxygenLevel = 100f
        dayTimeSeconds = 150f // start during mid-day
        isRaining = false
        rainTimerSeconds = 0f
        
        mobs.clear()
        particles.clear()
        miningTargetX = -1
        miningTargetY = -1
        miningProgress = 0f

        // Set up fresh inventory
        inventory.clear()
        repeat(30) { inventory.add(null) }

        if (mode == "CREATIVE") {
            // Give creative items
            inventory[0] = InventorySlot(ItemType.ITEM_GRASS, 999)
            inventory[1] = InventorySlot(ItemType.ITEM_STONE, 999)
            inventory[2] = InventorySlot(ItemType.ITEM_WOOD_TRUNK, 999)
            inventory[3] = InventorySlot(ItemType.ITEM_CRAFTING_TABLE, 999)
            inventory[4] = InventorySlot(ItemType.ITEM_TORCH, 999)
            inventory[5] = InventorySlot(ItemType.ITEM_GLASS, 999)
            inventory[6] = InventorySlot(ItemType.DIAMOND_PICKAXE, 1)
            inventory[7] = InventorySlot(ItemType.APPLE, 999)
        } else {
            // Survival starter pack
            inventory[0] = InventorySlot(ItemType.APPLE, 5)
            inventory[1] = InventorySlot(ItemType.STICK, 4)
        }
        selectedHotbarIndex = 0

        // Reset Achievements & Stats
        stats = GameStats()
        achievements.clear()
        achievements.addAll(Achievement.DEFAULT_ACHIEVEMENTS)

        val rand = Random(seed)
        val seaLevel = 30

        // World building logic: Heightmap + layers
        for (x in 0 until width) {
            // Custom sine terrain curve
            val heightVariation = sin(x * 0.08f) * 4f + cos(x * 0.03f) * 2f + sin(x * 0.25f) * 0.8f
            val surfaceSubY = (seaLevel + heightVariation).toInt()

            for (y in 0 until height) {
                when {
                    y < surfaceSubY -> {
                        // Atmosphere or water basins
                        if (y >= seaLevel + 2) {
                            worldBlocks[x][y] = BlockType.WATER.id // ponds
                        } else {
                            worldBlocks[x][y] = BlockType.AIR.id
                        }
                    }
                    y == surfaceSubY -> {
                        // Top soil
                        if (y >= seaLevel + 2) {
                            worldBlocks[x][y] = BlockType.SAND.id
                        } else {
                            worldBlocks[x][y] = BlockType.GRASS.id
                        }
                    }
                    y in (surfaceSubY + 1)..(surfaceSubY + 4) -> {
                        worldBlocks[x][y] = BlockType.DIRT.id
                    }
                    y > surfaceSubY + 4 && y < height - 1 -> {
                        // Underground Stone & Ores
                        val oreRoll = rand.nextFloat()
                        when {
                            oreRoll < 0.015f && y >= 45 -> worldBlocks[x][y] = BlockType.DIAMOND_ORE.id
                            oreRoll < 0.04f && y >= 36 -> worldBlocks[x][y] = BlockType.IRON_ORE.id
                            oreRoll < 0.07f && y >= 32 -> worldBlocks[x][y] = BlockType.COAL_ORE.id
                            else -> worldBlocks[x][y] = BlockType.STONE.id
                        }
                    }
                    y == height - 1 -> {
                        // Bedrock limit
                        worldBlocks[x][y] = BlockType.BEDROCK.id
                    }
                }
            }
        }

        // Generate Caves (Simulated cellular pockets or walk tunnels)
        for (tunnel in 0..5) {
            var currX = rand.nextInt(5, width - 5)
            var currY = rand.nextInt(38, height - 5)
            repeat(15) {
                val caveWidth = rand.nextInt(2, 4)
                val caveHeight = rand.nextInt(2, 4)
                for (dx in -caveWidth/2..caveWidth/2) {
                    for (dy in -caveHeight/2..caveHeight/2) {
                        val tx = currX + dx
                        val ty = currY + dy
                        if (tx in 2 until width - 2 && ty in 15 until height - 2) {
                            if (worldBlocks[tx][ty] != BlockType.BEDROCK.id) {
                                worldBlocks[tx][ty] = BlockType.AIR.id
                            }
                        }
                    }
                }
                currX += rand.nextInt(-2, 3)
                currY += rand.nextInt(-2, 3)
            }
        }

        // Natural Trees Generation
        for (x in 3 until width - 3 step 6) {
            val treeRoll = rand.nextFloat()
            if (treeRoll < 0.45f) {
                // Find grass block
                var surfaceY = -1
                for (y in 0 until height) {
                    if (worldBlocks[x][y] == BlockType.GRASS.id) {
                        surfaceY = y
                        break
                    }
                }
                if (surfaceY != -1 && worldBlocks[x][surfaceY - 1] == BlockType.AIR.id) {
                    val trunkHeight = rand.nextInt(3, 6)
                    // Place Trunk
                    for (ty in 1..trunkHeight) {
                        val trunkY = surfaceY - ty
                        if (trunkY > 0) {
                            worldBlocks[x][trunkY] = BlockType.WOOD_TRUNK.id
                        }
                    }
                    // Place Leaves
                    val leavesCenterY = surfaceY - trunkHeight - 1
                    for (lx in -2..2) {
                        for (ly in -2..1) {
                            val lex = x + lx
                            val ley = leavesCenterY + ly
                            if (lex in 0 until width && ley in 0 until height) {
                                // Don't overwrite wood trunk
                                if (worldBlocks[lex][ley] == BlockType.AIR.id) {
                                    // 3D round tree cap approximation
                                    if (abs(lx) + abs(ly) <= 3) {
                                        worldBlocks[lex][ley] = BlockType.LEAVES.id
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Set stable starting player coordinate
        var spawnX = 60f
        var spawnY = 15f
        for (y in 0 until height) {
            if (worldBlocks[60][y] != BlockType.AIR.id && worldBlocks[60][y] != BlockType.WATER.id) {
                spawnY = (y - 3).toFloat()
                break
            }
        }
        playerX = spawnX
        playerY = spawnY
        playerVx = 0f
        playerVy = 0f

        triggerPopup("World Generated!")
    }

    // 4. Update Game State Tick
    fun updateGame(deltaTime: Float) {
        if (deltaTime <= 0) return

        // Tick game clock
        dayTimeSeconds = (dayTimeSeconds + deltaTime) % dayCycleLength
        stats.timePlayedSeconds += (deltaTime).toLong()

        // Handle Weather
        rainTimerSeconds -= deltaTime
        if (rainTimerSeconds <= 0f) {
            isRaining = !isRaining
            rainTimerSeconds = if (isRaining) Random.nextFloat() * 80f + 40f else Random.nextFloat() * 180f + 120f
        }

        // Trigger Rain Particles
        if (isRaining && Random.nextFloat() < 0.25f) {
            repeat(3) {
                particles.add(
                    Particle(
                        x = playerX + Random.nextFloat() * 24f - 12f,
                        y = max(0f, playerY - 8f),
                        vx = -1.5f,
                        vy = 9f + Random.nextFloat() * 3f,
                        color = Color(0x7F5DADE2),
                        size = 2f,
                        life = 1f,
                        decay = 0.6f
                    )
                )
            }
        }

        // Update popup timer
        if (popupTimer > 0f) {
            popupTimer -= deltaTime
            if (popupTimer <= 0f) activePopupMessage = ""
        }

        // Survival core mechanics (Hunger & Health drain)
        if (gameMode == "SURVIVAL") {
            // Standard decay
            playerHunger = max(0f, playerHunger - 0.05f * deltaTime)

            // Oxygen checking
            val centerGridX = (playerX + entityWidth/2).toInt()
            val mouthGridY = (playerY + 0.3f).toInt()
            if (centerGridX in 0 until width && mouthGridY in 0 until height && worldBlocks[centerGridX][mouthGridY] == BlockType.WATER.id) {
                oxygenLevel = max(0f, oxygenLevel - 15f * deltaTime)
                if (oxygenLevel <= 0f) {
                    playerHealth = max(0f, playerHealth - 8f * deltaTime) // Drowning damage
                }
            } else {
                oxygenLevel = min(maxOxygen, oxygenLevel + 40f * deltaTime)
            }

            // Starvation
            if (playerHunger <= 0f) {
                playerHealth = max(0f, playerHealth - 3f * deltaTime)
            } else if (playerHunger > 75f && playerHealth < maxHealth) {
                playerHealth = min(maxHealth, playerHealth + 2f * deltaTime) // heal naturally
            }

            // Handle death respawn
            if (playerHealth <= 0f) {
                performRespawn()
            }
        } else {
            // Creative keeps parameters filled
            playerHealth = maxHealth
            playerHunger = maxHunger
            oxygenLevel = maxOxygen
        }

        // Toggle adjacent crafting table checker
        updateCraftingAvailability()

        // Apply Player physics
        processPlayerPhysics(deltaTime)

        // Process Wandering Mob spawns & AI physics
        processMobsAI(deltaTime)

        // Process Particles lifecycle
        processParticles(deltaTime)

        // Check Night Survival Achievement trigger
        if (gameMode == "SURVIVAL" && dayTimeSeconds >= 450f && dayTimeSeconds < 452f) {
            unlockAchievement("survive_night")
        }
    }

    // 5. Physics and Collision Systems
    private fun processPlayerPhysics(dt: Float) {
        val inLiquid = isPlayerInWater()

        // Gravity
        if (inLiquid) {
            playerVy += swimGravity * dt
            playerVy = playerVy.coerceIn(-runSpeed, runSpeed)
        } else {
            playerVy += gravity * dt
            playerVy = playerVy.coerceIn(-24f, 24f)
        }

        // X movement
        playerX += playerVx * dt
        resolveCollisions(horizontal = true)

        // Y movement
        playerY += playerVy * dt
        resolveCollisions(horizontal = false)
    }

    private fun resolveCollisions(horizontal: Boolean) {
        val pxLeft = playerX
        val pxRight = playerX + entityWidth
        val pyTop = playerY
        val pyBottom = playerY + entityHeight

        // Bounds check
        if (playerX < 0f) { playerX = 0f; playerVx = 0f }
        if (playerX + entityWidth > width) { playerX = width - entityWidth; playerVx = 0f }
        if (playerY < 0f) { playerY = 0f; playerVy = 0f }
        if (playerY + entityHeight > height) { performRespawn(); return }

        // Find intersecting tiles
        val minX = floor(pxLeft).toInt().coerceIn(0, width - 1)
        val maxX = floor(pxRight).toInt().coerceIn(0, width - 1)
        val minY = floor(pyTop).toInt().coerceIn(0, height - 1)
        val maxY = floor(pyBottom).toInt().coerceIn(0, height - 1)

        for (tx in minX..maxX) {
            for (ty in minY..maxY) {
                val blockId = worldBlocks[tx][ty]
                val bType = BlockType.fromId(blockId)
                if (bType.isCollidable) {
                    // Check intersection
                    val bxLeft = tx.toFloat()
                    val bxRight = tx.toFloat() + 1f
                    val byTop = ty.toFloat()
                    val byBottom = ty.toFloat() + 1f

                    if (pxRight > bxLeft && pxLeft < bxRight && pyBottom > byTop && pyTop < byBottom) {
                        // Collision detected! Resolve based on travel axis
                        if (horizontal) {
                            if (playerVx > 0) {
                                // Moving right, hit left of block
                                playerX = bxLeft - entityWidth - 0.001f
                            } else if (playerVx < 0) {
                                // Moving left, hit right of block
                                playerX = bxRight + 0.001f
                            }
                            playerVx = 0f
                        } else {
                            if (playerVy > 0) {
                                // Falling down, stand on top
                                playerY = byTop - entityHeight - 0.001f
                                playerVy = 0f
                            } else if (playerVy < 0) {
                                // Jumping up, hit bottom of block
                                playerY = byBottom + 0.001f
                                playerVy = 0f
                            }
                        }
                    }
                }
            }
        }
    }

    private fun isPlayerInWater(): Boolean {
        val cx = (playerX + entityWidth / 2).toInt().coerceIn(0, width - 1)
        val cy = (playerY + entityHeight - 0.2f).toInt().coerceIn(0, height - 1)
        return worldBlocks[cx][cy] == BlockType.WATER.id
    }

    private fun performRespawn() {
        stats.deathsCount++
        playerHealth = 100f
        playerHunger = 100f
        oxygenLevel = 100f
        
        // Find safe spawn near middle of grid
        playerX = 60f
        playerY = 10f
        for (y in 0 until height) {
            if (worldBlocks[60][y] != BlockType.AIR.id && worldBlocks[60][y] != BlockType.WATER.id) {
                playerY = (y - 3).toFloat()
                break
            }
        }
        playerVx = 0f
        playerVy = 0f
        triggerPopup("You Respawned!")
    }

    // 6. Action Triggers
    fun tryJump() {
        val isOnGround = checkIfOnGround(playerX, playerY, entityWidth, entityHeight)
        val inWater = isPlayerInWater()
        
        if (inWater) {
            playerVy = swimUpForce
        } else if (isOnGround) {
            playerVy = jumpForce
        }
    }

    private fun checkIfOnGround(x: Float, y: Float, w: Float, h: Float): Boolean {
        val testY = y + h + 0.05f
        val leftX = floor(x).toInt().coerceIn(0, width - 1)
        val rightX = floor(x + w).toInt().coerceIn(0, width - 1)
        val checkerY = floor(testY).toInt().coerceIn(0, height - 1)

        for (tx in leftX..rightX) {
            if (BlockType.fromId(worldBlocks[tx][checkerY]).isCollidable) {
                return true
            }
        }
        return false
    }

    // 7. Mining and Placement Actions
    fun handleMiningTick(dt: Float, gridX: Int, gridY: Int) {
        if (gridX !in 0 until width || gridY !in 0 until height) {
            clearMiningTarget()
            return
        }

        val bId = worldBlocks[gridX][gridY]
        val block = BlockType.fromId(bId)

        // Bedrock and water cannot be mined
        if (block == BlockType.AIR || block == BlockType.BEDROCK || block.isLiquid) {
            clearMiningTarget()
            return
        }

        // Reach checker (limit mining capability to standard radial range)
        val dist = sqrt((playerX + entityWidth/2 - gridX - 0.5f).pow(2) + (playerY + entityHeight/2 - gridY - 0.5f).pow(2))
        if (dist > 5.0f) {
            clearMiningTarget()
            return
        }

        // Set mining target
        if (miningTargetX != gridX || miningTargetY != gridY) {
            miningTargetX = gridX
            miningTargetY = gridY
            miningProgress = 0f
        }

        val multiplier = getActiveToolMultiplier(block)
        miningProgress += (1f / block.hardness) * multiplier * dt

        // Mining completes
        if (miningProgress >= 1.0f) {
            worldBlocks[gridX][gridY] = BlockType.AIR.id
            clearMiningTarget()

            stats.blocksMined++

            // Drops directly to inventory
            val dropItem = getBlockDrop(block)
            if (dropItem != null) {
                addItemToInventory(dropItem, 1)
                triggerPopup("Mined: ${dropItem.itemName}")
            }

            // Achievements checks
            if (block == BlockType.WOOD_TRUNK) unlockAchievement("miner_first")
            if (block == BlockType.COAL_ORE) unlockAchievement("mine_coal")
            if (block == BlockType.IRON_ORE) unlockAchievement("mine_iron")
            if (block == BlockType.DIAMOND_ORE) unlockAchievement("mine_diamond")

            // Particle sparks
            spawnMiningSparks(gridX.toFloat() + 0.5f, gridY.toFloat() + 0.5f, block.color)
        }
    }

    fun clearMiningTarget() {
        miningTargetX = -1
        miningTargetY = -1
        miningProgress = 0f
    }

    fun tryPlaceBlock(gridX: Int, gridY: Int): Boolean {
        if (gridX !in 0 until width || gridY !in 0 until height) return false

        // Check if there is already a block
        val existing = BlockType.fromId(worldBlocks[gridX][gridY])
        if (existing != BlockType.AIR && existing != BlockType.WATER) return false

        // Distance check
        val dist = sqrt((playerX + entityWidth/2 - gridX - 0.5f).pow(2) + (playerY + entityHeight/2 - gridY - 0.5f).pow(2))
        if (dist > 5.0f) return false

        // Collide check against player body bounds
        val pLeft = playerX
        val pRight = playerX + entityWidth
        val pTop = playerY
        val pBottom = playerY + entityHeight
        val bLeft = gridX.toFloat()
        val bRight = gridX.toFloat() + 1f
        val bTop = gridY.toFloat()
        val bBottom = gridY.toFloat() + 1f

        val activeHotbar = inventory[selectedHotbarIndex] ?: return false
        if (!activeHotbar.itemType.isBlock) return false

        val newBlock = BlockType.fromId(activeHotbar.itemType.blockId)
        if (newBlock.isCollidable) {
            // Cannot place solid block intersecting player
            if (pRight > bLeft && pLeft < bRight && pBottom > bTop && pTop < bBottom) return false
        }

        // Place block
        worldBlocks[gridX][gridY] = newBlock.id
        stats.blocksPlaced++

        if (gameMode != "CREATIVE") {
            activeHotbar.count--
            if (activeHotbar.count <= 0) {
                inventory[selectedHotbarIndex] = null
            }
        }
        
        spawnMiningSparks(gridX.toFloat() + 0.5f, gridY.toFloat() + 0.5f, newBlock.color)
        return true
    }

    private fun getActiveToolMultiplier(block: BlockType): Float {
        val activeItem = inventory[selectedHotbarIndex] ?: return 1.0f
        if (activeItem.itemType.toolType == ToolType.PICKAXE) {
            val tier = activeItem.itemType.toolTier
            // Pickaxes work well on stone and ores
            if (block == BlockType.STONE || block == BlockType.COAL_ORE || block == BlockType.IRON_ORE || block == BlockType.DIAMOND_ORE) {
                return tier.speedMultiplier
            }
        }
        return 1.0f
    }

    private fun getBlockDrop(block: BlockType): ItemType? {
        return when (block) {
            BlockType.GRASS -> ItemType.ITEM_DIRT
            BlockType.DIRT -> ItemType.ITEM_DIRT
            BlockType.STONE -> ItemType.ITEM_STONE
            BlockType.WOOD_TRUNK -> ItemType.ITEM_WOOD_TRUNK
            BlockType.LEAVES -> if (Random.nextFloat() < 0.15f) ItemType.APPLE else null
            BlockType.COAL_ORE -> ItemType.COAL
            BlockType.IRON_ORE -> ItemType.ITEM_IRON_ORE
            BlockType.DIAMOND_ORE -> ItemType.DIAMOND
            BlockType.PLANKS -> ItemType.ITEM_PLANKS
            BlockType.GLASS -> null
            BlockType.SAND -> ItemType.ITEM_SAND
            BlockType.TORCH -> ItemType.ITEM_TORCH
            BlockType.CRAFTING_TABLE -> ItemType.ITEM_CRAFTING_TABLE
            else -> null
        }
    }

    // 8. Inventory Ops
    fun addItemToInventory(item: ItemType, quantity: Int): Boolean {
        // 1. Stack on existing slot
        for (i in 0 until 30) {
            val slot = inventory[i]
            if (slot != null && slot.itemType == item && slot.count < 64) {
                val sum = slot.count + quantity
                if (sum <= 64) {
                    slot.count = sum
                    return true
                } else {
                    slot.count = 64
                    return addItemToInventory(item, sum - 64)
                }
            }
        }
        // 2. Insert into empty slot
        for (i in 0 until 30) {
            if (inventory[i] == null) {
                inventory[i] = InventorySlot(item, quantity)
                return true
            }
        }
        return false
    }

    fun hasIngredients(recipe: CraftingRecipe): Boolean {
        if (gameMode == "CREATIVE") return true
        
        for ((ingredient, needed) in recipe.ingredients) {
            var found = 0
            for (i in 0 until 30) {
                val slot = inventory[i]
                if (slot != null && slot.itemType == ingredient) {
                    found += slot.count
                }
            }
            if (found < needed) return false
        }
        return true
    }

    fun craftItem(recipe: CraftingRecipe) {
        if (!hasIngredients(recipe)) {
            triggerPopup("Missing Materials!")
            return
        }

        if (recipe.requiresCraftingTable && !isNearCraftingTable) {
            triggerPopup("Requires a Crafting Table nearby!")
            return
        }

        // Consume ingredients in Survival
        if (gameMode != "CREATIVE") {
            for ((ingredient, needed) in recipe.ingredients) {
                var remaining = needed
                for (i in 0 until 30) {
                    val slot = inventory[i]
                    if (slot != null && slot.itemType == ingredient) {
                        if (slot.count >= remaining) {
                            slot.count -= remaining
                            if (slot.count <= 0) inventory[i] = null
                            break
                        } else {
                            remaining -= slot.count
                            inventory[i] = null
                        }
                    }
                }
            }
        }

        addItemToInventory(recipe.result, recipe.resultCount)
        stats.craftedCount++
        triggerPopup("Crafted ${recipe.result.itemName}!")

        // Achievement checks
        if (recipe.result == ItemType.ITEM_CRAFTING_TABLE) unlockAchievement("craft_table")
        if (recipe.result == ItemType.WOODEN_PICKAXE) unlockAchievement("pick_wood")
    }

    fun eatSelectedFood() {
        val activeItem = inventory[selectedHotbarIndex] ?: return
        if (activeItem.itemType.foodValue > 0f) {
            playerHunger = (playerHunger + activeItem.itemType.foodValue).coerceAtMost(maxHunger)
            playerHealth = (playerHealth + activeItem.itemType.foodValue * 0.2f).coerceAtMost(maxHealth)
            
            if (gameMode != "CREATIVE") {
                activeItem.count--
                if (activeItem.count <= 0) inventory[selectedHotbarIndex] = null
            }
            triggerPopup("Yum! Hunger restored.")
        }
    }

    private fun updateCraftingAvailability() {
        var flagTable = false
        val px = playerX.toInt()
        val py = playerY.toInt()
        for (dx in -3..3) {
            for (dy in -3..3) {
                val tx = px + dx
                val ty = py + dy
                if (tx in 0 until width && ty in 0 until height) {
                    if (worldBlocks[tx][ty] == BlockType.CRAFTING_TABLE.id) {
                        flagTable = true
                        break
                    }
                }
            }
        }
        isNearCraftingTable = flagTable
    }

    // Swaps slots in grid
    fun swapInventorySlots(slotA: Int, slotB: Int) {
        if (slotA in 0..29 && slotB in 0..29) {
            val temp = inventory[slotA]
            inventory[slotA] = inventory[slotB]
            inventory[slotB] = temp
        }
    }

    // 9. Mob System Spawning and Updates
    private fun processMobsAI(dt: Float) {
        // Tick/spawn mobs
        val maxMobs = 10
        val isNight = dayTimeSeconds > 320f && dayTimeSeconds < 560f

        if (mobs.size < maxMobs && Random.nextFloat() < 0.02f) {
            // Spawn a random entity outside of instant sight
            val rx = (playerX + if (Random.nextBoolean()) Random.nextInt(12, 18) else -Random.nextInt(12, 18)).coerceIn(2f, (width-3).toFloat())
            // find ground node below rx
            var ry = -1f
            for (y in 0 until height) {
                if (worldBlocks[rx.toInt()][y] != BlockType.AIR.id && worldBlocks[rx.toInt()][y] != BlockType.WATER.id) {
                    ry = (y - 3).toFloat()
                    break
                }
            }

            if (ry > 0f) {
                val type = if (isNight) {
                    // Spawn monster (Zombie, Skeleton, Slime)
                    when (Random.nextInt(3)) {
                        0 -> MobType.ZOMBIE
                        1 -> MobType.SKELETON
                        else -> MobType.SLIME
                    }
                } else {
                    // Spawn animal (Cow, Sheep, Chicken)
                    when (Random.nextInt(3)) {
                        0 -> MobType.COW
                        1 -> MobType.SHEEP
                        else -> MobType.CHICKEN
                    }
                }

                mobs.add(
                    Mob(
                        id = UUID.randomUUID().toString(),
                        type = type,
                        x = rx,
                        y = ry,
                        health = if (isNight) 40f else 25f,
                        maxHealth = if (isNight) 40f else 25f
                    )
                )
            }
        }

        // Process Mobs logic
        val iterator = mobs.iterator()
        while (iterator.hasNext()) {
            val mob = iterator.next()
            mob.stateTime += dt

            // Check if mob of bounds
            if (mob.y > height) {
                iterator.remove()
                continue
            }

            // AI movement
            val targetX = playerX
            val distToPlayer = sqrt((mob.x - playerX).pow(2) + (mob.y - playerY).pow(2))

            val isHostile = mob.type == MobType.ZOMBIE || mob.type == MobType.SKELETON || mob.type == MobType.SLIME

            if (isHostile && distToPlayer < 12f) {
                // Seek player
                mob.facingLeft = mob.x > targetX
                val moveDir = if (mob.facingLeft) -1f else 1f
                mob.vx = moveDir * (if (mob.type == MobType.SLIME) 1.2f else 2.0f)
            } else {
                // Passive wander
                if (mob.stateTime > 3f) {
                    mob.stateTime = 0f
                    if (Random.nextFloat() < 0.4f) {
                        mob.vx = if (Random.nextBoolean()) 1f else -1f
                    } else {
                        mob.vx = 0f
                    }
                    mob.facingLeft = mob.vx < 0f
                }
            }

            // Slime jump/bounce
            if (mob.type == MobType.SLIME) {
                mob.jumpCooldown -= dt
                if (mob.jumpCooldown <= 0f && checkIfOnGround(mob.x, mob.y, 0.8f, 0.8f)) {
                    mob.vy = -6.5f
                    mob.jumpCooldown = Random.nextFloat() * 1.5f + 1f
                }
            }

            // Gravity for mob
            val mInWater = worldBlocks[mob.x.toInt().coerceIn(0, width-1)][(mob.y + 1f).toInt().coerceIn(0, height-1)] == BlockType.WATER.id
            if (mInWater) {
                mob.vy += swimGravity * dt
                if (mob.vx != 0f && Random.nextFloat() < 0.1f) mob.vy = -2f // swim up
            } else {
                mob.vy += gravity * dt
            }

            // Move X
            mob.x += mob.vx * dt
            resolveMobCollisions(mob, true)

            // Jump if face obstacle
            val facedObstacle = checkIfCollidingSide(mob.x, mob.y, mob.vx > 0f)
            if (facedObstacle && checkIfOnGround(mob.x, mob.y, 0.8f, 1.2f) && mob.type != MobType.SLIME) {
                mob.vy = -7.5f
            }

            // Move Y
            mob.y += mob.vy * dt
            resolveMobCollisions(mob, false)

            // Attack player
            if (isHostile && distToPlayer < 1.3f && gameMode == "SURVIVAL") {
                val now = System.currentTimeMillis()
                if (now - mob.lastAttackTime > 1200L) {
                    mob.lastAttackTime = now
                    playerHealth = max(0f, playerHealth - (if (mob.type == MobType.SKELETON) 12f else 8f))
                    // knock back player
                    playerVx = (if (playerX < mob.x) -3f else 3f)
                    playerVy = -3f
                    triggerPopup("Ouch!")
                }
            }
        }
    }

    private fun checkIfCollidingSide(mx: Float, my: Float, facingRight: Boolean): Boolean {
        val testX = if (facingRight) mx + 0.9f else mx - 0.1f
        val tx = floor(testX).toInt().coerceIn(0, width - 1)
        val ty1 = floor(my + 0.1f).toInt().coerceIn(0, height - 1)
        val ty2 = floor(my + 1.0f).toInt().coerceIn(0, height - 1)

        return BlockType.fromId(worldBlocks[tx][ty1]).isCollidable || BlockType.fromId(worldBlocks[tx][ty2]).isCollidable
    }

    private fun resolveMobCollisions(mob: Mob, horizontal: Boolean) {
        val mw = 0.8f
        val mh = if (mob.type == MobType.SLIME) 0.8f else 1.7f
        
        val mxLeft = mob.x
        val mxRight = mob.x + mw
        val myTop = mob.y
        val myBottom = mob.y + mh

        // bounds push back
        if (mob.x < 0) { mob.x = 0f; mob.vx = 0f }
        if (mob.x + mw > width) { mob.x = width - mw; mob.vx = 0f }

        val minX = floor(mxLeft).toInt().coerceIn(0, width - 1)
        val maxX = floor(mxRight).toInt().coerceIn(0, width - 1)
        val minY = floor(myTop).toInt().coerceIn(0, height - 1)
        val maxY = floor(myBottom).toInt().coerceIn(0, height - 1)

        for (tx in minX..maxX) {
            for (ty in minY..maxY) {
                if (BlockType.fromId(worldBlocks[tx][ty]).isCollidable) {
                    val bxLeft = tx.toFloat()
                    val bxRight = tx.toFloat() + 1f
                    val byTop = ty.toFloat()
                    val byBottom = ty.toFloat() + 1f

                    if (mxRight > bxLeft && mxLeft < bxRight && myBottom > byTop && myTop < byBottom) {
                        if (horizontal) {
                            if (mob.vx > 0) mob.x = bxLeft - mw - 0.001f
                            else if (mob.vx < 0) mob.x = bxRight + 0.001f
                            mob.vx = 0f
                        } else {
                            if (mob.vy > 0) {
                                mob.y = byTop - mh - 0.001f
                                mob.vy = 0f
                            } else if (mob.vy < 0) {
                                mob.y = byBottom + 0.001f
                                mob.vy = 0f
                            }
                        }
                    }
                }
            }
        }
    }

    fun attackInDirection(facingLeft: Boolean) {
        // Search mobs inside direct strike swipe reach
        val strikeReach = 2.5f
        val attackBoxLeft = if (facingLeft) playerX - strikeReach else playerX
        val attackBoxRight = if (facingLeft) playerX + entityWidth else playerX + entityWidth + strikeReach
        val attackBoxTop = playerY - 0.5f
        val attackBoxBottom = playerY + entityHeight + 0.5f

        val activeItem = inventory[selectedHotbarIndex]
        val damage = activeItem?.itemType?.toolTier?.attackDamage ?: 4f

        val iterator = mobs.iterator()
        while (iterator.hasNext()) {
            val m = iterator.next()
            if (m.x >= attackBoxLeft && m.x <= attackBoxRight && m.y >= attackBoxTop && m.y <= attackBoxBottom) {
                m.health -= damage
                
                // knockback mob
                m.vx = if (facingLeft) -4.5f else 4.5f
                m.vy = -3.5f

                // Sparks particle spray
                spawnMiningSparks(m.x + 0.4f, m.y + 0.8f, Color.Red)

                if (m.health <= 0f) {
                    stats.mobsKilled++
                    iterator.remove()

                    // Drops meat/wood on defeat
                    val animalDrops = when (m.type) {
                        MobType.COW -> ItemType.RAW_MEAT to Random.nextInt(1, 3)
                        MobType.SHEEP -> ItemType.RAW_MEAT to 1
                        MobType.CHICKEN -> ItemType.APPLE to 1 // generic raw bird analog
                        MobType.ZOMBIE -> ItemType.COAL to 1
                        MobType.SKELETON -> ItemType.STICK to 2
                        MobType.SLIME -> ItemType.ITEM_GLASS to 1 // jelly drop Analog
                    }
                    addItemToInventory(animalDrops.first, animalDrops.second)
                    triggerPopup("Mob Slayed! Loot dropped.")
                    unlockAchievement("slay_monster")
                }
                break // Attack one mob per tap
            }
        }
    }

    // 10. Spark effects & particle decay
    private fun processParticles(dt: Float) {
        val iterator = particles.iterator()
        while (iterator.hasNext()) {
            val p = iterator.next()
            p.life -= p.decay * dt
            if (p.life <= 0f) {
                iterator.remove()
                continue
            }
            p.x += p.vx * dt
            p.y += p.vy * dt
        }
    }

    private fun spawnMiningSparks(x: Float, y: Float, color: Color) {
        repeat(8) {
            val angle = Random.nextFloat() * 2f * PI.toFloat()
            val speed = Random.nextFloat() * 4f + 1f
            particles.add(
                Particle(
                    x = x,
                    y = y,
                    vx = cos(angle) * speed,
                    vy = sin(angle) * speed - 2f, // upward sparks path
                    color = color,
                    size = Random.nextFloat() * 4f + 3f,
                    life = 1f,
                    decay = Random.nextFloat() * 1.5f + 1.2f
                )
            )
        }
    }

    // 11. Offline Saving & loading helpers
    fun saveToEntity(): SavedWorld {
        val blocksBuilder = StringBuilder()
        for (x in 0 until width) {
            for (y in 0 until height) {
                blocksBuilder.append(worldBlocks[x][y]).append(",")
            }
        }

        val inventoryBuilder = StringBuilder()
        for (i in 0 until 30) {
            val item = inventory[i]
            if (item != null) {
                inventoryBuilder.append("${item.itemType.id}_${item.count}_$i,")
            }
        }

        val achievementsBuilder = StringBuilder()
        achievements.forEach {
            if (it.isUnlocked) {
                achievementsBuilder.append(it.id).append(",")
            }
        }

        val statsT = "${stats.blocksMined}:${stats.blocksPlaced}:${stats.mobsKilled}:${stats.craftedCount}:${stats.deathsCount}:${stats.timePlayedSeconds}"

        return SavedWorld(
            id = worldId,
            name = worldName,
            seed = currentSeed,
            gameMode = gameMode,
            dayTime = dayTimeSeconds,
            playerX = playerX,
            playerY = playerY,
            playerHp = playerHealth,
            playerHunger = playerHunger,
            worldWidth = width,
            worldHeight = height,
            worldBlocksText = blocksBuilder.toString(),
            inventoryText = inventoryBuilder.toString(),
            achievementsText = achievementsBuilder.toString(),
            statsText = statsT,
            isRaining = isRaining
        )
    }

    fun loadFromEntity(entity: SavedWorld) {
        worldId = entity.id
        worldName = entity.name
        currentSeed = entity.seed
        gameMode = entity.gameMode
        dayTimeSeconds = entity.dayTime
        playerX = entity.playerX
        playerY = entity.playerY
        playerHealth = entity.playerHp
        playerHunger = entity.playerHunger
        isRaining = entity.isRaining

        mobs.clear()
        particles.clear()
        clearMiningTarget()

        // Read blocks text
        val ids = entity.worldBlocksText.split(",")
        var index = 0
        for (x in 0 until width) {
            for (y in 0 until height) {
                if (index < ids.size - 1) {
                    val idVal = ids[index].toIntOrNull() ?: BlockType.AIR.id
                    worldBlocks[x][y] = idVal
                }
                index++
            }
        }

        // Read inventory text
        inventory.clear()
        repeat(30) { inventory.add(null) }
        
        if (entity.inventoryText.isNotEmpty()) {
            val slots = entity.inventoryText.split(",")
            for (slotStr in slots) {
                if (slotStr.isEmpty()) continue
                val parts = slotStr.split("_")
                if (parts.size >= 3) {
                    val itemId = parts[0].toIntOrNull() ?: continue
                    val count = parts[1].toIntOrNull() ?: continue
                    val slotIndex = parts[2].toIntOrNull() ?: continue
                    if (slotIndex in 0..29) {
                        inventory[slotIndex] = InventorySlot(ItemType.fromId(itemId), count)
                    }
                }
            }
        }

        // Achievements Text
        val unIds = entity.achievementsText.split(",").toSet()
        for (i in 0 until achievements.size) {
            val ach = achievements[i]
            if (unIds.contains(ach.id)) {
                achievements[i] = ach.copy(isUnlocked = true)
            }
        }

        // Stats Text
        val statsParts = entity.statsText.split(":")
        if (statsParts.size >= 6) {
            stats = GameStats(
                blocksMined = statsParts[0].toIntOrNull() ?: 0,
                blocksPlaced = statsParts[1].toIntOrNull() ?: 0,
                mobsKilled = statsParts[2].toIntOrNull() ?: 0,
                craftedCount = statsParts[3].toIntOrNull() ?: 0,
                deathsCount = statsParts[4].toIntOrNull() ?: 0,
                timePlayedSeconds = statsParts[5].toLongOrNull() ?: 0L
            )
        }

        triggerPopup("World Save Loaded!")
    }

    private fun unlockAchievement(id: String) {
        val i = achievements.indexOfFirst { it.id == id }
        if (i != -1 && !achievements[i].isUnlocked) {
            achievements[i] = achievements[i].copy(isUnlocked = true)
            triggerPopup("Achievement: ${achievements[i].title}")
        }
    }

    fun triggerPopup(message: String) {
        activePopupMessage = message
        popupTimer = 3.0f
    }
}
