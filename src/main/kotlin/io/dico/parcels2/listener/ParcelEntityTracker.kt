package io.dico.parcels2.listener

import io.dico.parcels2.Parcel
import io.dico.parcels2.Worlds
import io.dico.parcels2.util.editLoop
import io.dico.parcels2.util.isPresentAnd
import org.bukkit.entity.Entity

class ParcelEntityTracker(val worlds: Worlds) {
    val map = mutableMapOf<Entity, Parcel?>()

    fun untrack(entity: Entity) {
        map.remove(entity)
    }

    fun track(entity: Entity, parcel: Parcel?) {
        map[entity] = parcel
    }

    /*
     * Tracks entities. If the entity is dead, they are removed from the collection.
     * If the entity is found to have left the parcel it was created in, it will be removed from the world and from the list.
     * If it is still in the parcel it was created in, and it is on the ground, it is removed from the list.
     *
     * Start after 5 seconds, run every 0.25 seconds
     */
    fun tick() {
        map.editLoop { entity, parcel ->
            if (entity.isDead || entity.isOnGround) {
                remove(); return@editLoop
            }
            if (parcel.isPresentAnd { hasBlockVisitors }) {
                remove()
            }
            val newParcel = worlds.getParcelAt(entity.location)
            if (newParcel !== parcel && !newParcel.isPresentAnd { hasBlockVisitors }) {
                remove()
                entity.remove()
            }
        }
    }

    fun swapParcels(parcel1: Parcel, parcel2: Parcel) {
        map.editLoop {
            if (value === parcel1) {
                value = parcel2
            } else if (value === parcel2) {
                value = parcel1
            }
        }
    }

}