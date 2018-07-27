package io.dico.parcels2.storage

import io.dico.parcels2.Parcel
import io.dico.parcels2.ParcelWorld
import io.dico.parcels2.Worlds
import io.dico.parcels2.util.Vec2i
import org.bukkit.Bukkit
import org.bukkit.World
import java.util.*

data class SerializableWorld(val name: String? = null,
                             val uid: UUID? = null) {

    init {
        uid ?: name ?: throw IllegalArgumentException("uuid and/or name must be present")
    }

    val world: World? by lazy { uid?.let { Bukkit.getWorld(it) } ?: name?.let { Bukkit.getWorld(it) } }
    //val parcelWorld: ParcelWorld? by lazy { TODO() }
}

/**
 * Used by storage backing options to encompass the location of a parcel
 */
data class SerializableParcel(val world: SerializableWorld,
                              val pos: Vec2i) {

    //val parcel: Parcel? by lazy { TODO() }
}

fun Worlds.getWorldBySerializedValue(input: SerializableWorld): ParcelWorld? {
    return input.world?.let { getWorld(it) }
}

fun Worlds.getParcelBySerializedValue(input: SerializableParcel): Parcel? {
    return getWorldBySerializedValue(input.world)?.parcelByID(input.pos)
}