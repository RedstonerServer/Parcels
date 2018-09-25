package io.dico.parcels2.blockvisitor

import io.dico.parcels2.util.Vec3i
import io.dico.parcels2.util.ext.getMaterialsWithWoodTypePrefix
import io.dico.parcels2.util.ext.getMaterialsWithWoolColorPrefix
import org.bukkit.Material
import org.bukkit.Material.*
import org.bukkit.block.BlockFace
import org.bukkit.block.data.BlockData
import org.bukkit.block.data.Directional
import java.util.EnumSet

private val attachables = EnumSet.of(
    REPEATER, COMPARATOR,
    *getMaterialsWithWoodTypePrefix("PRESSURE_PLATE"),
    STONE_PRESSURE_PLATE, LIGHT_WEIGHTED_PRESSURE_PLATE, HEAVY_WEIGHTED_PRESSURE_PLATE,
    *getMaterialsWithWoodTypePrefix("BUTTON"),
    STONE_BUTTON, LEVER,
    *getMaterialsWithWoodTypePrefix("DOOR"), IRON_DOOR,
    ACTIVATOR_RAIL, POWERED_RAIL, DETECTOR_RAIL, RAIL,
    PISTON, STICKY_PISTON,
    REDSTONE_TORCH, REDSTONE_WALL_TORCH, REDSTONE_WIRE,
    TRIPWIRE, TRIPWIRE_HOOK,

    BROWN_MUSHROOM, RED_MUSHROOM, CACTUS, CARROT, COCOA,
    WHEAT, DEAD_BUSH, CHORUS_FLOWER, DANDELION, SUGAR_CANE,
    TALL_GRASS, TALL_SEAGRASS, NETHER_WART, MELON_STEM,
    PUMPKIN_STEM, SUNFLOWER, POTATO, LILY_PAD, VINE,
    *getMaterialsWithWoodTypePrefix("SAPLING"),

    SAND, RED_SAND, DRAGON_EGG, ANVIL,
    *getMaterialsWithWoolColorPrefix("CONCRETE_POWDER"),

    *getMaterialsWithWoolColorPrefix("CARPET"),
    CAKE, FIRE,
    FLOWER_POT,
    LADDER,
    // NETHER_PORTAL, fuck nether portals
    FLOWER_POT,
    SNOW,
    TORCH, WALL_TORCH,
    *getMaterialsWithWoolColorPrefix("BANNER"),
    *getMaterialsWithWoolColorPrefix("WALL_BANNER"),
    SIGN, WALL_SIGN
)

fun isAttachable(type: Material) = attachables.contains(type)

fun supportingBlock(data: BlockData): Vec3i = when (data) {
    //is MultipleFacing -> // fuck it xD this is good enough

    is Directional -> Vec3i.convert(when (data.material) {
        // exceptions
        COCOA -> data.facing
        OAK_DOOR, BIRCH_DOOR, SPRUCE_DOOR, JUNGLE_DOOR, ACACIA_DOOR, DARK_OAK_DOOR, IRON_DOOR -> BlockFace.DOWN

        else -> data.facing.oppositeFace
    })

    else -> Vec3i.down
}
