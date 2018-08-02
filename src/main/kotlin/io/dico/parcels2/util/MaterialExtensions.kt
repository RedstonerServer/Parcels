package io.dico.parcels2.util

import org.bukkit.Material
import org.bukkit.Material.*

/* 
colors:
WHITE_$, ORANGE_$, MAGENTA_$, LIGHT_BLUE_$, YELLOW_$, LIME_$, PINK_$, GRAY_$, LIGHT_GRAY_$, CYAN_$, PURPLE_$, BLUE_$, BROWN_$, GREEN_$, RED_$, BLACK_$, 
wood:
OAK_$, BIRCH_$, SPRUCE_$, JUNGLE_$, ACACIA_$, DARK_OAK_$,
 */

val Material.isBed
    get() = when (this) {
        WHITE_BED,
        ORANGE_BED,
        MAGENTA_BED,
        LIGHT_BLUE_BED,
        YELLOW_BED,
        LIME_BED,
        PINK_BED,
        GRAY_BED,
        LIGHT_GRAY_BED,
        CYAN_BED,
        PURPLE_BED,
        BLUE_BED,
        BROWN_BED,
        GREEN_BED,
        RED_BED,
        BLACK_BED -> true
        else -> false
    }

val Material.isWoodDoor
    get() = when (this) {
        OAK_DOOR,
        BIRCH_DOOR,
        SPRUCE_DOOR,
        JUNGLE_DOOR,
        ACACIA_DOOR,
        DARK_OAK_DOOR -> true
        else -> false
    }

val Material.isWoodTrapdoor
    get() = when (this) {
        OAK_TRAPDOOR,
        BIRCH_TRAPDOOR,
        SPRUCE_TRAPDOOR,
        JUNGLE_TRAPDOOR,
        ACACIA_TRAPDOOR,
        DARK_OAK_TRAPDOOR -> true
        else -> false
    }

val Material.isWoodFenceGate
    get() = when (this) {
        OAK_FENCE_GATE,
        BIRCH_FENCE_GATE,
        SPRUCE_FENCE_GATE,
        JUNGLE_FENCE_GATE,
        ACACIA_FENCE_GATE,
        DARK_OAK_FENCE_GATE -> true
        else -> false
    }

val Material.isWoodButton
    get() = when (this) {
        OAK_BUTTON,
        BIRCH_BUTTON,
        SPRUCE_BUTTON,
        JUNGLE_BUTTON,
        ACACIA_BUTTON,
        DARK_OAK_BUTTON -> true
        else -> false
    }
