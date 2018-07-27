package io.dico.parcels2.listener

import io.dico.parcels2.Parcel
import org.bukkit.entity.Entity

class ParcelEntityTracker {
    val map = mutableMapOf<Entity, Parcel>()

    fun untrack(entity: Entity) {
        map.remove(entity)
    }


}