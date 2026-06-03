package com.example.game

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.pow

object GameRenderer {

    // Helper to draw a pixel-art tile procedurally using colored sub-rects
    fun drawTile(
        scope: DrawScope,
        bType: BlockType,
        x: Float,
        y: Float,
        size: Float,
        timeSeconds: Float
    ) {
        with(scope) {
            val color = bType.color
            
            // Draw core background
            drawRect(color = color, topLeft = Offset(x, y), size = Size(size, size))

            // Procedural pixel-art textures
            val pixel = size / 8f
            when (bType) {
                BlockType.GRASS -> {
                    // Draw grass top green cap (2 pixel height)
                    drawRect(
                        color = Color(0xFF4B9032),
                        topLeft = Offset(x, y),
                        size = Size(size, pixel * 2.5f)
                    )
                    // Individual hanging grass blades
                    drawRect(color = Color(0xFF4B9032), topLeft = Offset(x + pixel, y + pixel * 2.5f), size = Size(pixel, pixel))
                    drawRect(color = Color(0xFF4B9032), topLeft = Offset(x + pixel * 4f, y + pixel * 2.5f), size = Size(pixel, pixel))
                    drawRect(color = Color(0xFF4B9032), topLeft = Offset(x + pixel * 6f, y + pixel * 2.5f), size = Size(pixel, pixel))
                    // Soil speckles
                    drawRect(color = Color(0xFF6F4E37), topLeft = Offset(x + pixel * 2f, y + pixel * 5f), size = Size(pixel * 1.5f, pixel))
                    drawRect(color = Color(0xFF6F4E37), topLeft = Offset(x + pixel * 5f, y + pixel * 6f), size = Size(pixel, pixel))
                }
                BlockType.DIRT -> {
                    // Clay/dirt specks
                    drawRect(color = Color(0xFF966F4F), topLeft = Offset(x + pixel * 2f, y + pixel * 1.5f), size = Size(pixel, pixel))
                    drawRect(color = Color(0xFF66462C), topLeft = Offset(x + pixel * 5f, y + pixel * 3f), size = Size(pixel * 1.5f, pixel))
                    drawRect(color = Color(0xFF966F4F), topLeft = Offset(x + pixel * 4f, y + pixel * 6f), size = Size(pixel, pixel))
                }
                BlockType.STONE -> {
                    // Stone fissures
                    drawRect(color = Color(0xFF636363), topLeft = Offset(x, y + pixel * 2f), size = Size(pixel * 4f, pixel))
                    drawRect(color = Color(0xFF636363), topLeft = Offset(x + pixel * 3f, y + pixel * 2f), size = Size(pixel, pixel * 3f))
                    drawRect(color = Color(0xFF9C9C9C), topLeft = Offset(x + pixel * 5f, y + pixel * 5f), size = Size(pixel * 2f, pixel))
                    drawRect(color = Color(0xFF636363), topLeft = Offset(x + pixel * 2f, y + pixel * 6f), size = Size(pixel * 3f, pixel))
                }
                BlockType.COAL_ORE -> {
                    // Stone base with coal specks
                    drawRect(color = Color(0xFF1E1E1E), topLeft = Offset(x + pixel, y + pixel * 2f), size = Size(pixel * 2f, pixel * 2f))
                    drawRect(color = Color(0xFF1E1E1E), topLeft = Offset(x + pixel * 5f, y + pixel * 1f), size = Size(pixel, pixel))
                    drawRect(color = Color(0xFF1E1E1E), topLeft = Offset(x + pixel * 4f, y + pixel * 5f), size = Size(pixel * 2f, pixel * 2f))
                }
                BlockType.IRON_ORE -> {
                    // Stone base with tan metallic specks
                    val ironColor = Color(0xFFC89C76)
                    drawRect(color = ironColor, topLeft = Offset(x + pixel * 2f, y + pixel), size = Size(pixel * 2f, pixel))
                    drawRect(color = ironColor, topLeft = Offset(x + pixel, y + pixel * 4f), size = Size(pixel, pixel * 2f))
                    drawRect(color = ironColor, topLeft = Offset(x + pixel * 5f, y + pixel * 3f), size = Size(pixel * 2f, pixel))
                    drawRect(color = ironColor, topLeft = Offset(x + pixel * 4f, y + pixel * 6f), size = Size(pixel, pixel))
                }
                BlockType.DIAMOND_ORE -> {
                    // Stone base with glowing cyan pixels
                    val diaColor = Color(0xFF00FFFF)
                    drawRect(color = diaColor, topLeft = Offset(x + pixel * 2f, y + pixel * 2f), size = Size(pixel, pixel))
                    drawRect(color = diaColor, topLeft = Offset(x + pixel * 5f, y + pixel * 1f), size = Size(pixel * 1.5f, pixel))
                    drawRect(color = diaColor, topLeft = Offset(x + pixel, y + pixel * 5f), size = Size(pixel, pixel))
                    drawRect(color = diaColor, topLeft = Offset(x + pixel * 5f, y + pixel * 5f), size = Size(pixel, pixel * 1.5f))
                }
                BlockType.WOOD_TRUNK -> {
                    // Ring bark lines
                    drawRect(color = Color(0xFF533B29), topLeft = Offset(x, y), size = Size(pixel, size))
                    drawRect(color = Color(0xFF533B29), topLeft = Offset(x + size - pixel, y), size = Size(pixel, size))
                    drawRect(color = Color(0xFF533B29), topLeft = Offset(x + pixel * 3f, y), size = Size(pixel, size))
                }
                BlockType.LEAVES -> {
                    // Darker leaf cluster circles
                    drawCircle(color = Color(0xFF225530), center = Offset(x + pixel * 2f, y + pixel * 3f), radius = pixel * 1.5f)
                    drawCircle(color = Color(0xFF225530), center = Offset(x + pixel * 6f, y + pixel * 4f), radius = pixel * 2f)
                    drawRect(color = Color(0xFF1E4627), topLeft = Offset(x + pixel, y + pixel * 5f), size = Size(pixel, pixel))
                }
                BlockType.PLANKS -> {
                    // Horizontal groove lines
                    drawRect(color = Color(0xFF785E3E), topLeft = Offset(x, y + pixel * 3f), size = Size(size, pixel))
                    drawRect(color = Color(0xFF785E3E), topLeft = Offset(x, y + pixel * 7f), size = Size(size, pixel))
                    // Vertical plank joints
                    drawRect(color = Color(0xFF785E3E), topLeft = Offset(x + pixel * 3f, y), size = Size(pixel, pixel * 3f))
                    drawRect(color = Color(0xFF785E3E), topLeft = Offset(x + pixel * 5f, y + pixel * 4f), size = Size(pixel, pixel * 3f))
                }
                BlockType.GLASS -> {
                    // Shading shine diagonal
                    val alphaShine = Color(0xAAFFFFFF)
                    drawLine(
                        color = alphaShine,
                        start = Offset(x + pixel * 2f, y + size - pixel),
                        end = Offset(x + size - pixel, y + pixel * 2f),
                        strokeWidth = pixel
                    )
                }
                BlockType.SAND -> {
                    // Sand dune waves
                    drawRect(color = Color(0xFFDFC566), topLeft = Offset(x + pixel, y + pixel * 2f), size = Size(pixel, pixel))
                    drawRect(color = Color(0xFFDFC566), topLeft = Offset(x + pixel * 4f, y + pixel * 5f), size = Size(pixel, pixel))
                    drawRect(color = Color(0xFFC1AA4F), topLeft = Offset(x + pixel * 6f, y + pixel * 3f), size = Size(pixel, pixel))
                }
                BlockType.TORCH -> {
                    // Clear background since it is a torch, then draw details
                    drawRect(color = Color(0x00FFFFFF), topLeft = Offset(x, y), size = Size(size, size)) // transparent air backing
                    // Torch post
                    drawRect(
                        color = Color(0xFF8B5A2B),
                        topLeft = Offset(x + pixel * 3.5f, y + pixel * 3f),
                        size = Size(pixel, pixel * 4f)
                    )
                    // Fire flame blinking based on time
                    val wave = (sin(timeSeconds * 12f) * 0.5f + 0.5f)
                    val flameColor = if (wave > 0.5f) Color(0xFFFF9900) else Color(0xFFFF4500)
                    drawRect(
                        color = flameColor,
                        topLeft = Offset(x + pixel * 3f, y + pixel),
                        size = Size(pixel * 2f, pixel * 2f)
                    )
                    drawRect(
                        color = Color(0xFFFFEA00),
                        topLeft = Offset(x + pixel * 3.5f, y + pixel * 1.5f),
                        size = Size(pixel, pixel)
                    )
                }
                BlockType.CRAFTING_TABLE -> {
                    // Bench top structure
                    drawRect(color = Color(0xFF5E3917), topLeft = Offset(x, y), size = Size(size, pixel * 1.5f))
                    // Diagonal table leg slots
                    drawRect(color = Color(0xFF5E3917), topLeft = Offset(x, y + pixel * 1.5f), size = Size(pixel * 1.5f, size))
                    drawRect(color = Color(0xFF5E3917), topLeft = Offset(x + size - pixel * 1.5f, y + pixel * 1.5f), size = Size(pixel * 1.5f, size))
                    // Grid hammer details on side
                    drawRect(color = Color(0xFFAAAAAA), topLeft = Offset(x + pixel * 3f, y + pixel * 3f), size = Size(pixel * 2f, pixel * 2f))
                    drawRect(color = Color(0xFF666666), topLeft = Offset(x + pixel * 4f, y + pixel * 5f), size = Size(pixel, pixel * 2f))
                }
                else -> {}
            }
        }
    }

    // Centered camera drawing wrapper for standard block canvas
    fun drawWorld(
        scope: DrawScope,
        engine: GameEngine,
        canvasWidth: Float,
        canvasHeight: Float
    ) {
        with(scope) {
            // Day Night Sky Palette processing
            val dt = engine.dayTimeSeconds
            val dayFraction = dt / engine.dayCycleLength
            val skyColor = getSkyColor(dayFraction)

            // Fill Sky Colors
            drawRect(color = skyColor, size = Size(canvasWidth, canvasHeight))

            // Draw Stars at night
            if (dayFraction > 0.6f || dayFraction < 0.1f) {
                // simple deterministic star coordinate grid
                val starColor = Color(0xEFFFFFFF)
                for (star in 0..15) {
                    val phase = (sin(dt * 0.5f + star) * 0.5f + 0.5f)
                    val starSize = 2f + phase * 3f
                    val starX = (star * 137L) % canvasWidth.toLong()
                    val starY = (star * 411L) % (canvasHeight * 0.6f).toLong()
                    drawCircle(color = starColor, center = Offsets(starX.toFloat(), starY.toFloat()), radius = starSize * 0.5f)
                }
            }

            // Draw Sun & Moon
            // Circular rotation crossing screen
            val angle = (dayFraction * 2f * Math.PI) - (Math.PI / 2f)
            val pathRadius = canvasWidth * 0.45f
            val skyCenterX = canvasWidth * 0.5f
            val skyCenterY = canvasHeight * 0.7f

            val sunX = skyCenterX + cos(angle).toFloat() * pathRadius
            val sunY = skyCenterY + sin(angle).toFloat() * pathRadius

            val moonX = skyCenterX + cos(angle + Math.PI).toFloat() * pathRadius
            val moonY = skyCenterY + sin(angle + Math.PI).toFloat() * pathRadius

            // Sun: yellow glowing square
            drawRect(
                color = Color(0xFFFFF176),
                topLeft = Offset(sunX - 22f, sunY - 22f),
                size = Size(44f, 44f)
            )
            drawRect(
                color = Color(0x33FFF59D),
                topLeft = Offset(sunX - 35f, sunY - 35f),
                size = Size(70f, 70f)
            )

            // Moon: glowing white crescent-looking square
            drawRect(
                color = Color(0xFFEEEEEE),
                topLeft = Offset(moonX - 16f, moonY - 16f),
                size = Size(32f, 32f)
            )
            drawRect(
                color = Color(0x33FFFFFF),
                topLeft = Offset(moonX - 25f, moonY - 25f),
                size = Size(50f, 50f)
            )

            // Draw procedural background mountains
            drawBackgroundHills(scope, canvasWidth, canvasHeight, dayFraction)

            // Viewport configuration
            // Each camera block size scales beautifully to adapt to varying densities. On compact screens we show fewer blocks, tablets show larger area.
            val blockPx = (canvasWidth / 22f).coerceAtLeast(32f).coerceAtMost(64f)
            
            // Camera position: player in viewport center
            val viewPivotX = canvasWidth / 2f
            val viewPivotY = canvasHeight * 0.5f
            
            val cameraShiftX = viewPivotX - engine.playerX * blockPx - (engine.entityWidth / 2f) * blockPx
            val cameraShiftY = viewPivotY - engine.playerY * blockPx - (engine.entityHeight / 2f) * blockPx

            withTransform({
                translate(left = cameraShiftX, top = cameraShiftY)
            }) {
                // Calculate viewport limits to avoid drawing out-of-screen blocks
                val leftBlock = ((-cameraShiftX) / blockPx).toInt().coerceIn(0, engine.width - 1)
                val rightBlock = (((canvasWidth - cameraShiftX) / blockPx).toInt() + 1).coerceIn(0, engine.width - 1)
                val topBlock = ((-cameraShiftY) / blockPx).toInt().coerceIn(0, engine.height - 1)
                val bottomBlock = (((canvasHeight - cameraShiftY) / blockPx).toInt() + 1).coerceIn(0, engine.height - 1)

                // Render block tiles
                for (tx in leftBlock..rightBlock) {
                    for (ty in topBlock..bottomBlock) {
                        val blockId = engine.worldBlocks[tx][ty]
                        val bType = BlockType.fromId(blockId)
                        if (bType != BlockType.AIR) {
                            // Darken underground or night tile colors
                            val posDrawX = tx * blockPx
                            val posDrawY = ty * blockPx
                            drawTile(scope, bType, posDrawX, posDrawY, blockPx, engine.dayTimeSeconds)

                            // Overlay digging crack lines if active mining node
                            if (engine.miningTargetX == tx && engine.miningTargetY == ty) {
                                val crackAlpha = engine.miningProgress.coerceIn(0f, 1f)
                                drawRect(
                                    color = Color(0x00FFFFFF), // air base
                                    topLeft = Offset(posDrawX, posDrawY),
                                    size = Size(blockPx, blockPx)
                                )
                                // Drawing black cracks
                                val p = blockPx / 6f
                                drawLine(
                                    color = Color(0xCC000000),
                                    start = Offset(posDrawX, posDrawY),
                                    end = Offset(posDrawX + blockPx * crackAlpha, posDrawY + blockPx * crackAlpha),
                                    strokeWidth = 2.5f
                                )
                                drawLine(
                                    color = Color(0xCC000000),
                                    start = Offset(posDrawX + blockPx, posDrawY),
                                    end = Offset(posDrawX + blockPx - (blockPx * crackAlpha), posDrawY + blockPx * crackAlpha),
                                    strokeWidth = 2.5f
                                )
                            }

                            // Torch light bloom effect calculations
                            // Simple distance shaders simulated at night
                            if (dayFraction > 0.55f || dayFraction < 0.15f) {
                                // Black overlay on blocks based on distance to nearest torch or player
                                // To make it incredibly fast, we compute relative shading on the blocks
                                val shadeLevel = calculateLightLevel(tx, ty, engine)
                                if (shadeLevel > 0f) {
                                    drawRect(
                                        color = Color(0, 0, 0, (shadeLevel * 175).toInt().coerceIn(0, 255)),
                                        topLeft = Offset(posDrawX, posDrawY),
                                        size = Size(blockPx, blockPx)
                                    )
                                }
                            }
                        }
                    }
                }

                // Render Mobs
                engine.mobs.forEach { mob ->
                    drawMob(scope, mob, blockPx)
                }

                // Render Player
                drawPlayer(scope, engine, blockPx)

                // Render Particles
                engine.particles.forEach { p ->
                    drawCircle(
                        color = p.color.copy(alpha = p.life),
                        center = Offset(p.x * blockPx, p.y * blockPx),
                        radius = p.size
                    )
                }

                // Break/Mine reach indicator
                // Let's draw a subtle border around the tile currently hovered/mined
                if (engine.miningTargetX != -1 && engine.miningTargetY != -1) {
                    drawRect(
                        color = Color.White,
                        topLeft = Offset(engine.miningTargetX * blockPx, engine.miningTargetY * blockPx),
                        size = Size(blockPx, blockPx),
                        style = Stroke(width = 3f)
                    )
                }
            }
        }
    }

    private fun drawBackgroundHills(
        scope: DrawScope,
        width: Float,
        height: Float,
        dayFraction: Float
    ) {
        val hillColor = when {
            dayFraction < 0.1f -> Color(0xFF1E3A24) // Sunrise transition
            dayFraction in 0.1f..0.5f -> Color(0xFF385E38) // Daytime rich forest green
            dayFraction in 0.5f..0.6f -> Color(0xFF2C4C2C) // Sunset deep green
            else -> Color(0xFF0F1B0F) // Night shadow green
        }

        with(scope) {
            // Draw a quick double layered jagged outline
            // Hill level 1
            for (i in 0..15) {
                val stepX = width / 15f
                val h1 = HeightOffset(i, height)
                drawRect(
                    color = hillColor.copy(alpha = 0.45f),
                    topLeft = Offset(i * stepX, height - h1),
                    size = Size(stepX + 2f, h1)
                )
            }
        }
    }

    private fun HeightOffset(index: Int, totalHeight: Float): Float {
        // Deterministic jagged horizon
        return (totalHeight * 0.15f) + (sin(index * 1.7f) * 15f) + (cos(index * 0.9f) * 8f)
    }

    // Light shading values: 0f is full bright, 1.0f is full dark black shadow
    private fun calculateLightLevel(bx: Int, by: Int, engine: GameEngine): Float {
        // Calculate distance to nearest torch within look range, or player's light
        var minDistance = 999f

        // Torch proximity check (within 10 tiles on both axes for performance)
        val px = engine.playerX.toInt()
        val py = engine.playerY.toInt()

        // Distance to player (player glows a bit)
        val distToPlayer = sqrt((bx - engine.playerX).pow(2) + (by - engine.playerY).pow(2))
        if (distToPlayer < 4.0f) {
            minDistance = minDistance.coerceAtMost(distToPlayer * 1.5f)
        }

        // Search in a local box for placed torches
        val range = 6
        for (dx in -range..range) {
            for (dy in -range..range) {
                val tx = bx + dx
                val ty = by + dy
                if (tx in 0 until engine.width && ty in 0 until engine.height) {
                    if (engine.worldBlocks[tx][ty] == BlockType.TORCH.id) {
                        val d = sqrt((dx).toFloat().pow(2) + (dy).toFloat().pow(2))
                        minDistance = minDistance.coerceAtMost(d)
                    }
                }
            }
        }

        if (minDistance < 1f) return 0f // fully illuminated
        if (minDistance > 7.5f) return 0.85f // ambient underground shadows

        return ((minDistance - 1f) / 6.5f).coerceIn(0f, 0.85f)
    }

    // Simple mathematical vector generator instead of deprecated Offset
    private fun Offsets(x: Float, y: Float): Offset {
        return Offset(x, y)
    }

    private fun getSkyColor(fraction: Float): Color {
        return when {
            fraction < 0.08f -> {
                // Pre-Sunrise: Purple/Blue
                interpolateColor(Color(0xFF0F0F1A), Color(0xFFE25822), fraction / 0.08f)
            }
            fraction < 0.15f -> {
                // Sunrise: Orange/Amber
                interpolateColor(Color(0xFFE25822), Color(0xFF87CEEB), (fraction - 0.08f) / 0.07f)
            }
            fraction < 0.50f -> {
                // Day: Blue Sky
                Color(0xFF87CEEB)
            }
            fraction < 0.55f -> {
                // Sunset: Blue -> orange purple
                interpolateColor(Color(0xFF87CEEB), Color(0xFFE25822), (fraction - 0.50f) / 0.05f)
            }
            fraction < 0.62f -> {
                // Night fallback transition
                interpolateColor(Color(0xFFE25822), Color(0xFF0F0F1A), (fraction - 0.55f) / 0.07f)
            }
            else -> {
                // Midnight
                Color(0xFF0F0F1A)
            }
        }
    }

    private fun interpolateColor(from: Color, to: Color, ratio: Float): Color {
        val r = ratio.coerceIn(0f, 1f)
        val red = from.red + (to.red - from.red) * r
        val green = from.green + (to.green - from.green) * r
        val blue = from.blue + (to.blue - from.blue) * r
        return Color(red, green, blue, 1f)
    }

    private fun drawPlayer(scope: DrawScope, engine: GameEngine, blockPx: Float) {
        with(scope) {
            val pw = engine.entityWidth * blockPx
            val ph = engine.entityHeight * blockPx
            val px = engine.playerX * blockPx
            val py = engine.playerY * blockPx

            val isFacingLeft = engine.playerVx < 0f
            // If mining target is active, simulate hand swinging action based on time
            val swingAngle = if (engine.miningTargetX != -1 && engine.miningTargetY != -1) {
                (sin(engine.dayTimeSeconds * 20f) * 45f)
            } else 0f

            // Character skin/hair/shirt elements
            // Head
            val hdSize = blockPx * 0.5f // head size
            val hdX = if (isFacingLeft) px + pw * 0.1f else px + pw * 0.4f
            val hdY = py

            drawRect(
                color = Color(0xFFFFD1A9), // skin color
                topLeft = Offset(hdX, hdY),
                size = Size(hdSize, hdSize)
            )

            // Red hair cap
            drawRect(
                color = Color(0xFFA62A2A),
                topLeft = Offset(hdX, hdY),
                size = Size(hdSize, hdSize * 0.35f)
            )

            // Dark Eyes looking facing direction
            val eyeX = if (isFacingLeft) hdX + hdSize * 0.15f else hdX + hdSize * 0.65f
            drawRect(
                color = Color.Black,
                topLeft = Offset(eyeX, hdY + hdSize * 0.4f),
                size = Size(blockPx * 0.08f, blockPx * 0.08f)
            )

            // Torso (Shirt)
            val trY = py + hdSize
            val trH = ph * 0.45f
            drawRect(
                color = Color(0xFF2196F3), // turquoise survival shirt
                topLeft = Offset(px, trY),
                size = Size(pw, trH)
            )

            // Legs (pants)
            val legY = trY + trH
            val legH = ph - hdSize - trH
            drawRect(
                color = Color(0xFF1A237E), // blue denim pants
                topLeft = Offset(px, legY),
                size = Size(pw, legH)
            )

            // Double brown shoes
            drawRect(color = Color(0xFF5D4037), topLeft = Offset(px, legY + legH - blockPx * 0.15f), size = Size(pw * 0.45f, blockPx * 0.15f))
            drawRect(color = Color(0xFF5D4037), topLeft = Offset(px + pw * 0.55f, legY + legH - blockPx * 0.15f), size = Size(pw * 0.45f, blockPx * 0.15f))

            // Swinging Arm tool
            val wristX = if (isFacingLeft) px + pw * 0.2f else px + pw * 0.8f
            val wristY = trY + trH * 0.4f
            withTransform({
                rotate(degrees = if (isFacingLeft) -swingAngle else swingAngle, pivot = Offset(wristX, wristY))
            }) {
                // Draw arm extending
                drawRect(
                    color = Color(0xFFFFD1A9), // arm skin
                    topLeft = Offset(wristX - blockPx * 0.1f, wristY),
                    size = Size(blockPx * 0.2f, blockPx * 0.5f)
                )

                // Render active weapon or tool representation at the end of the hand!
                val activeHotbar = engine.inventory[engine.selectedHotbarIndex]
                if (activeHotbar != null) {
                    val holdingItem = activeHotbar.itemType
                    val toolColor = when {
                        holdingItem.toolTier == ToolTier.DIAMOND -> Color(0xFF00FFCC)
                        holdingItem.toolTier == ToolTier.IRON -> Color(0xFFE0E0E0)
                        holdingItem.toolTier == ToolTier.STONE -> Color(0xFF9E9E9E)
                        holdingItem.toolTier == ToolTier.WOOD -> Color(0xFF8B5A2B)
                        holdingItem == ItemType.ITEM_TORCH -> Color(0xFFFFCC00)
                        else -> Color(0xFFFFD54F) // other items yellow stick
                    }

                    if (holdingItem.toolType == ToolType.SWORD) {
                        // draw beautiful sword blade pointing up
                        drawRect(
                            color = toolColor,
                            topLeft = Offset(wristX - blockPx * 0.08f, wristY + blockPx * 0.4f - blockPx * 0.8f),
                            size = Size(blockPx * 0.15f, blockPx * 0.8f)
                        )
                        drawRect(
                            color = Color(0xFF5D4037), // guard handle
                            topLeft = Offset(wristX - blockPx * 0.2f, wristY + blockPx * 0.4f),
                            size = Size(blockPx * 0.4f, blockPx * 0.08f)
                        )
                    } else if (holdingItem.toolType == ToolType.PICKAXE) {
                        // pickaxe stick handle
                        drawLine(
                            color = Color(0xFF8B5A2B),
                            start = Offset(wristX, wristY + blockPx * 0.1f),
                            end = Offset(wristX + (if (isFacingLeft) -blockPx * 0.4f else blockPx * 0.4f), wristY + blockPx * 0.6f),
                            strokeWidth = 3f
                        )
                        // pickaxe curved head metal
                        drawLine(
                            color = toolColor,
                            start = Offset(wristX + (if (isFacingLeft) -blockPx * 0.5f else blockPx * 0.5f), wristY + blockPx * 0.3f),
                            end = Offset(wristX + (if (isFacingLeft) -blockPx * 0.3f else blockPx * 0.3f), wristY + blockPx * 0.9f),
                            strokeWidth = 5f
                        )
                    }
                }
            }
        }
    }

    private fun drawMob(scope: DrawScope, mob: Mob, blockPx: Float) {
        with(scope) {
            val mw = 0.8f * blockPx
            val mh = (if (mob.type == MobType.SLIME) 0.8f else 1.7f) * blockPx
            val mx = mob.x * blockPx
            val my = mob.y * blockPx

            val dirFact = if (mob.facingLeft) -1f else 1f

            when (mob.type) {
                MobType.ZOMBIE -> {
                    // Decay Green zombie!
                    // Head
                    drawRect(color = Color(0xFF2E7D32), topLeft = Offset(mx + mw * 0.15f, my), size = Size(mw * 0.7f, mw * 0.7f))
                    // Dark eyes
                    val ex = if (mob.facingLeft) mx + mw * 0.25f else mx + mw * 0.65f
                    drawRect(color = Color.Red, topLeft = Offset(ex, my + mw * 0.3f), size = Size(4f, 4f))
                    // Shirt
                    drawRect(color = Color(0xFF006064), topLeft = Offset(mx, my + mw * 0.7f), size = Size(mw, mh * 0.5f))
                    // Pants
                    drawRect(color = Color(0xFF4E342E), topLeft = Offset(mx, my + mw * 0.7f + mh * 0.5f), size = Size(mw, mh * 0.5f - mw * 0.7f))
                }
                MobType.SKELETON -> {
                    // Bleached bones skeleton
                    // Head skull
                    drawRect(color = Color(0xFFE2E2E2), topLeft = Offset(mx + mw * 0.2f, my), size = Size(mw * 0.6f, mw * 0.6f))
                    val ex = if (mob.facingLeft) mx + mw * 0.3f else mx + mw * 0.6f
                    drawRect(color = Color.Black, topLeft = Offset(ex, my + mw * 0.25f), size = Size(4f, 4f))

                    // Ribs center post
                    drawRect(color = Color(0xFFE2E2E2), topLeft = Offset(mx + mw * 0.4f, my + mw * 0.6f), size = Size(mw * 0.2f, mh * 0.45f))
                    // horizontal rib lines
                    drawRect(color = Color(0xFFE2E2E2), topLeft = Offset(mx + mw * 0.15f, my + mw * 0.8f), size = Size(mw * 0.7f, 4f))
                    drawRect(color = Color(0xFFE2E2E2), topLeft = Offset(mx + mw * 0.15f, my + mw * 1.1f), size = Size(mw * 0.7f, 4f))

                    // Legs
                    drawRect(color = Color(0xFFCCCCCC), topLeft = Offset(mx + mw * 0.2f, my + mw * 0.6f + mh * 0.45f), size = Size(4f, mh * 0.35f))
                    drawRect(color = Color(0xFFCCCCCC), topLeft = Offset(mx + mw * 0.6f, my + mw * 0.6f + mh * 0.45f), size = Size(4f, mh * 0.35f))
                }
                MobType.SLIME -> {
                    // Jelly bouncing bouncy cube
                    val phase = (sin(mob.stateTime * 8f) * 0.15f) // vertical squash stretch factor
                    val activeH = mh * (1f - phase)
                    val activeW = mw * (1f + phase * 0.5f)
                    val drawX = mx + (mw - activeW) / 2f
                    val drawY = my + (mh - activeH)

                    drawRect(
                        color = Color(0xBB4CAF50), // translucent jelly green
                        topLeft = Offset(drawX, drawY),
                        size = Size(activeW, activeH)
                    )
                    // Core dark nucleus cube inside
                    drawRect(
                        color = Color(0xFF2E7132),
                        topLeft = Offset(drawX + activeW * 0.3f, drawY + activeH * 0.3f),
                        size = Size(activeW * 0.4f, activeH * 0.4f)
                    )
                    // Little black eyes
                    val eyePos = if (mob.facingLeft) drawX + activeW * 0.22f else drawX + activeW * 0.62f
                    drawRect(color = Color.Black, topLeft = Offset(eyePos, drawY + activeH * 0.25f), size = Size(4f, 4f))
                }
                MobType.COW -> {
                    // Spotty brown dairy animal
                    drawRect(color = Color(0xFF6D4C41), topLeft = Offset(mx, my + mh * 0.25f), size = Size(mw, mh * 0.75f))
                    // white spot patches
                    drawRect(color = Color.White, topLeft = Offset(mx + mw * 0.2f, my + mh * 0.35f), size = Size(mw * 0.3f, mh * 0.3f))
                    drawRect(color = Color.White, topLeft = Offset(mx + mw * 0.6f, my + mh * 0.5f), size = Size(mw * 0.25f, mh * 0.2f))
                    // Cow Head
                    val chx = if (mob.facingLeft) mx - mw * 0.15f else mx + mw * 0.65f
                    drawRect(color = Color(0xFF4E342E), topLeft = Offset(chx, my + mh * 0.1f), size = Size(mw * 0.5f, mh * 0.4f))
                    // nose pink wattle
                    val nx = if (mob.facingLeft) chx else chx + mw * 0.3f
                    drawRect(color = Color(0xFFF8BBD0), topLeft = Offset(nx, my + mh * 0.25f), size = Size(mw * 0.2f, mh * 0.15f))
                }
                MobType.SHEEP -> {
                    // Fluffy white sheep
                    // Body block is fluffy white
                    drawRect(color = Color(0xFFEFEFEF), topLeft = Offset(mx, my + mh * 0.25f), size = Size(mw, mh * 0.75f))
                    // legs
                    drawRect(color = Color.DarkGray, topLeft = Offset(mx + mw * 0.15f, my + mh * 0.75f), size = Size(4f, mh * 0.25f))
                    drawRect(color = Color.DarkGray, topLeft = Offset(mx + mw * 0.75f, my + mh * 0.75f), size = Size(4f, mh * 0.25f))
                    // sheep Head
                    val shx = if (mob.facingLeft) mx - mw * 0.1f else mx + mw * 0.7f
                    drawRect(color = Color(0xFFCCCCCC), topLeft = Offset(shx, my + mh * 0.25f), size = Size(mw * 0.4f, mh * 0.3f))
                }
                MobType.CHICKEN -> {
                    // Little blocky white poultry bird
                    val cw = mw * 0.65f
                    val ch = mh * 0.45f
                    drawRect(color = Color.White, topLeft = Offset(mx + (mw - cw)/2, my + mh * 0.35f), size = Size(cw, ch))
                    // yellow beak
                    val bx = if (mob.facingLeft) mx else mx + mw - 6f
                    drawRect(color = Color(0xFFFFB300), topLeft = Offset(bx, my + mh * 0.35f), size = Size(6f, 4f))
                    // little red beard
                    drawRect(color = Color.Red, topLeft = Offset(bx, my + mh * 0.35f + 4f), size = Size(4f, 4f))
                    // chicken tiny legs
                    drawRect(color = Color(0xFFFFB300), topLeft = Offset(mx + mw * 0.35f, my + mh * 0.7f), size = Size(2f, mh * 0.25f))
                    drawRect(color = Color(0xFFFFB300), topLeft = Offset(mx + mw * 0.65f, my + mh * 0.7f), size = Size(2f, mh * 0.25f))
                }
            }
        }
    }
}
