@file:Suppress("FunctionName")

package io.dico.parcels2

import io.dico.parcels2.util.math.Vec2i
import org.bukkit.Bukkit
import org.bukkit.World
import java.util.UUID

/**
 * Used by storage backing options to encompass the identity of a world
 * Does NOT support equality operator.
 */
interface ParcelWorldId {
    val name: String
    val uid: UUID?
    fun equals(id: ParcelWorldId): Boolean = name == id.name || (uid != null && uid == id.uid)

    val bukkitWorld: World? get() = Bukkit.getWorld(name) ?: uid?.let { Bukkit.getWorld(it) }
}

fun ParcelWorldId.parcelWorldIdToString() = "ParcelWorld($name)"

fun ParcelWorldId(worldName: String, worldUid: UUID? = null): ParcelWorldId = ParcelWorldIdImpl(worldName, worldUid)
fun ParcelWorldId(world: World) = ParcelWorldId(world.name, world.uid)

/**
 * Used by storage backing options to encompass the location of a parcel
 * Does NOT support equality operator.
 */
interface ParcelId {
    val worldId: ParcelWorldId
    val x: Int
    val z: Int
    val pos: Vec2i get() = Vec2i(x, z)
    val idString get() = "$x,$z"
    fun equals(id: ParcelId): Boolean = x == id.x && z == id.z && worldId.equals(id.worldId)
}

fun ParcelId.parcelIdToString() = "Parcel(${worldId.name},$idString)"

fun ParcelId(worldId: ParcelWorldId, pos: Vec2i) = ParcelId(worldId, pos.x, pos.z)
fun ParcelId(worldName: String, worldUid: UUID?, pos: Vec2i) = ParcelId(worldName, worldUid, pos.x, pos.z)
fun ParcelId(worldName: String, worldUid: UUID?, x: Int, z: Int) = ParcelId(ParcelWorldId(worldName, worldUid), x, z)
fun ParcelId(worldId: ParcelWorldId, x: Int, z: Int): ParcelId = ParcelIdImpl(worldId, x, z)

private class ParcelWorldIdImpl(override val name: String,
                                override val uid: UUID?) : ParcelWorldId {
    override fun toString() = parcelWorldIdToString()
}

private class ParcelIdImpl(override val worldId: ParcelWorldId,
                           override val x: Int,
                           override val z: Int) : ParcelId {
    override fun toString() = parcelIdToString()
}
