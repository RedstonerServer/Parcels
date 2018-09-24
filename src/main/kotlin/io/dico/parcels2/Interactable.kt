package io.dico.parcels2

import io.dico.parcels2.util.ext.ceilDiv
import io.dico.parcels2.util.ext.getMaterialsWithWoodTypePrefix
import org.bukkit.Material
import java.util.EnumMap

class Interactables
private constructor(
    val id: Int,
    val name: String,
    val interactableByDefault: Boolean,
    vararg val materials: Material
) {

    companion object {
        val classesById: List<Interactables>
        val classesByName: Map<String, Interactables>
        val listedMaterials: Map<Material, Int>

        init {
            val array = getClassesArray()
            classesById = array.asList()
            classesByName = mapOf(*array.map { it.name to it }.toTypedArray())
            listedMaterials = EnumMap(mapOf(*array.flatMap { clazz -> clazz.materials.map { it to clazz.id } }.toTypedArray()))
        }

        operator fun get(material: Material): Interactables? {
            val id = listedMaterials[material] ?: return null
            return classesById[id]
        }

        operator fun get(name: String): Interactables {
            return classesByName[name] ?: throw IllegalArgumentException("Interactables class does not exist: $name")
        }

        operator fun get(id: Int): Interactables {
            return classesById[id]
        }

        private fun getClassesArray() = run {
            var id = 0
            @Suppress("UNUSED_CHANGED_VALUE")
            arrayOf(
                Interactables(
                    id++, "buttons", true,
                    Material.STONE_BUTTON,
                    *getMaterialsWithWoodTypePrefix("BUTTON")
                ),

                Interactables(
                    id++, "levers", true,
                    Material.LEVER
                ),

                Interactables(
                    id++, "pressure_plates", true,
                    Material.STONE_PRESSURE_PLATE,
                    *getMaterialsWithWoodTypePrefix("PRESSURE_PLATE"),
                    Material.HEAVY_WEIGHTED_PRESSURE_PLATE,
                    Material.LIGHT_WEIGHTED_PRESSURE_PLATE
                ),

                Interactables(
                    id++, "redstone", false,
                    Material.COMPARATOR,
                    Material.REPEATER
                ),

                Interactables(
                    id++, "containers", false,
                    Material.CHEST,
                    Material.TRAPPED_CHEST,
                    Material.DISPENSER,
                    Material.DROPPER,
                    Material.HOPPER,
                    Material.FURNACE
                ),

                Interactables(
                    id++, "gates", true,
                    *getMaterialsWithWoodTypePrefix("DOOR"),
                    *getMaterialsWithWoodTypePrefix("TRAPDOOR"),
                    *getMaterialsWithWoodTypePrefix("FENCE_GATE")
                )
            )
        }

    }

}

val Parcel?.effectiveInteractableConfig: InteractableConfiguration
    get() = this?.interactableConfig ?: pathInteractableConfig

val pathInteractableConfig: InteractableConfiguration = run {
    val data = BitmaskInteractableConfiguration().apply {
        Interactables.classesById.forEach {
            setInteractable(it, false)
        }
    }
    object : InteractableConfiguration by data {
        override fun setInteractable(clazz: Interactables, interactable: Boolean) =
            throw IllegalStateException("pathInteractableConfig is immutable")

        override fun clear() =
            throw IllegalStateException("pathInteractableConfig is immutable")

        override fun copyFrom(other: InteractableConfiguration) =
            throw IllegalStateException("pathInteractableConfig is immutable")
    }
}

interface InteractableConfiguration {
    val interactableClasses: List<Interactables> get() = Interactables.classesById.filter { isInteractable(it) }

    fun isInteractable(material: Material): Boolean
    fun isInteractable(clazz: Interactables): Boolean
    fun isDefault(): Boolean

    fun setInteractable(clazz: Interactables, interactable: Boolean): Boolean
    fun clear(): Boolean
    fun copyFrom(other: InteractableConfiguration) =
        Interactables.classesById.fold(false) { cur, elem -> setInteractable(elem, other.isInteractable(elem) || cur) }

    operator fun invoke(material: Material) = isInteractable(material)
    operator fun invoke(className: String) = isInteractable(Interactables[className])
}

fun InteractableConfiguration.isInteractable(clazz: Interactables?) = clazz != null && isInteractable(clazz)

class BitmaskInteractableConfiguration : InteractableConfiguration {
    val bitmaskArray = IntArray(Interactables.classesById.size ceilDiv Int.SIZE_BITS)

    private fun isBitSet(classId: Int): Boolean {
        val idx = classId.ushr(5)
        return idx < bitmaskArray.size && bitmaskArray[idx].and(0x1.shl(classId.and(0x1F))) != 0
    }

    override fun isInteractable(material: Material): Boolean {
        val classId = Interactables.listedMaterials[material] ?: return false
        return isBitSet(classId) != Interactables.classesById[classId].interactableByDefault
    }

    override fun isInteractable(clazz: Interactables): Boolean {
        return isBitSet(clazz.id) != clazz.interactableByDefault
    }

    override fun isDefault(): Boolean {
        for (x in bitmaskArray) {
            if (x != 0) return false
        }
        return true
    }

    override fun setInteractable(clazz: Interactables, interactable: Boolean): Boolean {
        val idx = clazz.id.ushr(5)
        if (idx >= bitmaskArray.size) return false
        val bit = 0x1.shl(clazz.id.and(0x1F))
        val oldBitmask = bitmaskArray[idx]
        bitmaskArray[idx] = if (interactable != clazz.interactableByDefault) oldBitmask.or(bit) else oldBitmask.and(bit.inv())
        return bitmaskArray[idx] != oldBitmask
    }

    override fun clear(): Boolean {
        var change = false
        for (i in bitmaskArray.indices) {
            if (!change && bitmaskArray[i] != 0) change = true
            bitmaskArray[i] = 0
        }
        return change
    }

}