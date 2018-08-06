package io.dico.parcels2

import io.dico.parcels2.util.Vec2i
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

    companion object {
        operator fun invoke(worldName: String, worldUid: UUID?): ParcelWorldId = ParcelWorldIdImpl(worldName, worldUid)
        operator fun invoke(worldName: String): ParcelWorldId = ParcelWorldIdImpl(worldName, null)
    }
}

/**
 * Used by storage backing options to encompass the location of a parcel
 * Does NOT support equality operator.
 */
interface ParcelId {
    val worldId: ParcelWorldId
    val x: Int
    val z: Int
    val pos: Vec2i get() = Vec2i(x, z)
    fun equals(id: ParcelId): Boolean = x == id.x && z == id.z && worldId.equals(id.worldId)

    companion object {
        operator fun invoke(worldId: ParcelWorldId, pos: Vec2i) = invoke(worldId, pos.x, pos.z)
        operator fun invoke(worldName: String, worldUid: UUID?, pos: Vec2i) = invoke(worldName, worldUid, pos.x, pos.z)
        operator fun invoke(worldName: String, worldUid: UUID?, x: Int, z: Int) = invoke(ParcelWorldId(worldName, worldUid), x, z)
        operator fun invoke(worldId: ParcelWorldId, x: Int, z: Int): ParcelId = ParcelIdImpl(worldId, x, z)
    }
}

private class ParcelWorldIdImpl(override val name: String,
                                override val uid: UUID?) : ParcelWorldId

private class ParcelIdImpl(override val worldId: ParcelWorldId,
                           override val x: Int,
                           override val z: Int) : ParcelId
