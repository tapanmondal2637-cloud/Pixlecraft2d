package com.example.game

import androidx.compose.ui.graphics.Color

// 1. Block Typology
enum class BlockType(
    val id: Int,
    val blockName: String,
    val hardness: Float, // Time in seconds under hand mining
    val isCollidable: Boolean,
    val isLiquid: Boolean = false,
    val color: Color
) {
    AIR(0, "Air", 0.0f, false, false, Color(0xFF87CEEB)), // Reset color dynamically based on time of day
    GRASS(1, "Grass Block", 0.5f, true, false, Color(0xFF557A2B)),
    DIRT(2, "Dirt", 0.4f, true, false, Color(0xFF866043)),
    STONE(3, "Stone", 1.5f, true, false, Color(0xFF7B7B7B)),
    WOOD_TRUNK(4, "Wood Trunk", 0.8f, false, false, Color(0xFF6B4C35)),
    LEAVES(5, "Leaves", 0.1f, false, false, Color(0xFF2E6F40)),
    COAL_ORE(6, "Coal Ore", 1.8f, true, false, Color(0xFF2C2C2C)),
    IRON_ORE(7, "Iron Ore", 2.2f, true, false, Color(0xFFB59376)),
    DIAMOND_ORE(8, "Diamond Ore", 3.5f, true, false, Color(0xFF50D6D6)),
    WATER(9, "Water", -1.0f, false, true, Color(0x992B65EC)),
    PLANKS(10, "Planks", 0.6f, true, false, Color(0xFF9E7E5A)),
    GLASS(11, "Glass", 0.2f, true, false, Color(0x55E0F2F1)),
    TORCH(12, "Torch", 0.05f, false, false, Color(0xFFFFCC00)),
    CRAFTING_TABLE(13, "Crafting Table", 0.8f, true, false, Color(0xFF815B38)),
    SAND(14, "Sand", 0.4f, true, false, Color(0xFFEEDC82)),
    BEDROCK(15, "Bedrock", -1.0f, true, false, Color(0xFF1E1E1E));

    companion object {
        fun fromId(id: Int): BlockType = values().find { it.id == id } ?: AIR
    }
}

// 2. Items & Tools
enum class ItemType(
    val id: Int,
    val itemName: String,
    val isBlock: Boolean,
    val blockId: Int = 0,
    val toolType: ToolType = ToolType.NONE,
    val toolTier: ToolTier = ToolTier.NONE,
    val foodValue: Float = 0.0f // Restores hunger if eaten
) {
    // Blocks representations
    ITEM_GRASS(1, "Grass Block", true, 1),
    ITEM_DIRT(2, "Dirt", true, 2),
    ITEM_STONE(3, "Stone", true, 3),
    ITEM_WOOD_TRUNK(4, "Wood Trunk", true, 4),
    ITEM_LEAVES(5, "Leaves", true, 5),
    ITEM_COAL_ORE(6, "Coal Ore", true, 6),
    ITEM_IRON_ORE(7, "Iron Ore", true, 7),
    ITEM_DIAMOND_ORE(8, "Diamond Ore", true, 8),
    ITEM_PLANKS(10, "Planks", true, 10),
    ITEM_GLASS(11, "Glass", true, 11),
    ITEM_TORCH(12, "Torch", true, 12),
    ITEM_CRAFTING_TABLE(13, "Crafting Table", true, 13),
    ITEM_SAND(14, "Sand", true, 14),

    // Raw Materials
    COAL(20, "Coal", false),
    IRON_INGOT(21, "Iron Ingot", false),
    DIAMOND(22, "Diamond", false),
    STICK(23, "Stick", false),

    // Tools
    WOODEN_PICKAXE(30, "Wooden Pickaxe", false, toolType = ToolType.PICKAXE, toolTier = ToolTier.WOOD),
    WOODEN_SWORD(31, "Wooden Sword", false, toolType = ToolType.SWORD, toolTier = ToolTier.WOOD),
    STONE_PICKAXE(32, "Stone Pickaxe", false, toolType = ToolType.PICKAXE, toolTier = ToolTier.STONE),
    STONE_SWORD(33, "Stone Sword", false, toolType = ToolType.SWORD, toolTier = ToolTier.STONE),
    IRON_PICKAXE(34, "Iron Pickaxe", false, toolType = ToolType.PICKAXE, toolTier = ToolTier.IRON),
    IRON_SWORD(35, "Iron Sword", false, toolType = ToolType.SWORD, toolTier = ToolTier.IRON),
    DIAMOND_PICKAXE(36, "Diamond Pickaxe", false, toolType = ToolType.PICKAXE, toolTier = ToolTier.DIAMOND),
    DIAMOND_SWORD(37, "Diamond Sword", false, toolType = ToolType.SWORD, toolTier = ToolTier.DIAMOND),

    // Foods
    APPLE(50, "Apple", false, foodValue = 20f),
    RAW_MEAT(51, "Raw Meat", false, foodValue = 15f),
    COOKED_MEAT(52, "Cooked Meat", false, foodValue = 40f),
    BREAD(53, "Bread", false, foodValue = 25f);

    companion object {
        fun fromId(id: Int): ItemType = values().find { it.id == id } ?: STICK
    }
}

enum class ToolType {
    NONE, PICKAXE, SWORD
}

enum class ToolTier(val speedMultiplier: Float, val attackDamage: Float) {
    NONE(1.0f, 2.0f),
    WOOD(2.0f, 4.0f),
    STONE(4.0f, 6.0f),
    IRON(6.0f, 9.0f),
    DIAMOND(10.0f, 15.0f)
}

// 3. Inventory Slot representation
data class InventorySlot(
    val itemType: ItemType,
    var count: Int
)

// 4. Recipe model
data class CraftingRecipe(
    val result: ItemType,
    val resultCount: Int,
    val ingredients: Map<ItemType, Int>,
    val requiresCraftingTable: Boolean = false
) {
    companion object {
        val RECIPES = listOf(
            CraftingRecipe(ItemType.ITEM_PLANKS, 4, mapOf(ItemType.ITEM_WOOD_TRUNK to 1)),
            CraftingRecipe(ItemType.STICK, 4, mapOf(ItemType.ITEM_PLANKS to 2)),
            CraftingRecipe(ItemType.ITEM_CRAFTING_TABLE, 1, mapOf(ItemType.ITEM_PLANKS to 4)),
            CraftingRecipe(ItemType.ITEM_TORCH, 4, mapOf(ItemType.COAL to 1, ItemType.STICK to 1)),
            CraftingRecipe(ItemType.COOKED_MEAT, 1, mapOf(ItemType.RAW_MEAT to 1, ItemType.COAL to 1)), // Smelting with Coal
            CraftingRecipe(ItemType.BREAD, 1, mapOf(ItemType.ITEM_GRASS to 3)), // Grass (seeds analog) to bread

            // Wooden tools
            CraftingRecipe(ItemType.WOODEN_PICKAXE, 1, mapOf(ItemType.ITEM_PLANKS to 3, ItemType.STICK to 2)),
            CraftingRecipe(ItemType.WOODEN_SWORD, 1, mapOf(ItemType.ITEM_PLANKS to 2, ItemType.STICK to 1)),

            // Stone tools
            CraftingRecipe(ItemType.STONE_PICKAXE, 1, mapOf(ItemType.ITEM_STONE to 3, ItemType.STICK to 2), requiresCraftingTable = true),
            CraftingRecipe(ItemType.STONE_SWORD, 1, mapOf(ItemType.ITEM_STONE to 2, ItemType.STICK to 1), requiresCraftingTable = true),

            // Iron tools
            CraftingRecipe(ItemType.IRON_PICKAXE, 1, mapOf(ItemType.ITEM_IRON_ORE to 3, ItemType.STICK to 2), requiresCraftingTable = true),
            CraftingRecipe(ItemType.IRON_SWORD, 1, mapOf(ItemType.ITEM_IRON_ORE to 2, ItemType.STICK to 1), requiresCraftingTable = true),

            // Diamond tools
            CraftingRecipe(ItemType.DIAMOND_PICKAXE, 1, mapOf(ItemType.DIAMOND to 3, ItemType.STICK to 2), requiresCraftingTable = true),
            CraftingRecipe(ItemType.DIAMOND_SWORD, 1, mapOf(ItemType.DIAMOND to 2, ItemType.STICK to 1), requiresCraftingTable = true)
        )
    }
}

// 5. Game Entities (Mobs)
enum class MobType {
    ZOMBIE, SKELETON, SLIME, COW, SHEEP, CHICKEN
}

data class Mob(
    val id: String,
    val type: MobType,
    var x: Float,
    var y: Float,
    var vx: Float = 0f,
    var vy: Float = 0f,
    var health: Float,
    var maxHealth: Float,
    var facingLeft: Boolean = false,
    var stateTime: Float = 0f,
    var lastAttackTime: Long = 0L,
    var jumpCooldown: Float = 0f
)

// 6. Particle system (Weather / Mining)
data class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val color: Color,
    val size: Float,
    var life: Float, // from 1.0 down to 0.0
    val decay: Float
)

// 7. Core Statistics & Achievements
data class GameStats(
    var blocksMined: Int = 0,
    var blocksPlaced: Int = 0,
    var mobsKilled: Int = 0,
    var craftedCount: Int = 0,
    var deathsCount: Int = 0,
    var timePlayedSeconds: Long = 0
)

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val isUnlocked: Boolean = false
) {
    companion object {
        val DEFAULT_ACHIEVEMENTS = listOf(
            Achievement("miner_first", "Getting Wood", "Chop down your first block of trunk."),
            Achievement("craft_table", "Benchmarking", "Craft a workbench table."),
            Achievement("pick_wood", "Time to Mine!", "Craft a wooden pickaxe."),
            Achievement("mine_coal", "Power Source", "Find and extract coal ore."),
            Achievement("mine_iron", "Heavy Metal", "Find and extract iron ore."),
            Achievement("mine_diamond", "DAZZLING!", "Acquire a diamond from deep underground!"),
            Achievement("slay_monster", "Monster Hunter", "Defeat your first night-time monster."),
            Achievement("survive_night", "I Survived!", "Survive one entire full night in Survival.")
        )
    }
}
