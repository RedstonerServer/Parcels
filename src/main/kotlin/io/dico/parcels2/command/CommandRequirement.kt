@file:Suppress("NOTHING_TO_INLINE")

package io.dico.parcels2.command

import io.dico.dicore.command.CommandException
import io.dico.dicore.command.Validate
import io.dico.parcels2.Parcel
import io.dico.parcels2.ParcelWorld
import io.dico.parcels2.Worlds
import io.dico.parcels2.util.hasAdminManage
import io.dico.parcels2.util.uuid
import org.bukkit.entity.Player

/*
 * Scope types for extension lambdas
 */
sealed class BaseScope

class WorldOnlyScope(val world: ParcelWorld) : BaseScope()
class ParcelScope(val world: ParcelWorld, val parcel: Parcel) : BaseScope()

/*
 * Interface to implicitly access worlds object by creating extension functions for it
 */
interface HasWorlds {
    val worlds: Worlds
}

/*
 * Functions to be used by command implementations
 */
inline fun <T> HasWorlds.requireInWorld(player: Player,
                                        admin: Boolean = false,
                                        block: WorldOnlyScope.() -> T): T {
    return WorldOnlyScope(worlds.getWorldRequired(player, admin = admin)).block()
}

inline fun <T> HasWorlds.requireInParcel(player: Player,
                                         admin: Boolean = false,
                                         own: Boolean = false,
                                         block: ParcelScope.() -> T): T {
    val parcel = worlds.getParcelRequired(player, admin = admin, own = own)
    return ParcelScope(parcel.world, parcel).block()
}

/*
 * Functions for checking
 */
fun Worlds.getWorldRequired(player: Player, admin: Boolean = false): ParcelWorld {
    if (admin) Validate.isTrue(player.hasAdminManage, "You must have admin rights to use that command")
    return getWorld(player.world)
        ?: throw CommandException("You must be in a parcel world to use that command")
}

fun Worlds.getParcelRequired(player: Player, admin: Boolean = false, own: Boolean = false): Parcel {
    val parcel = getWorldRequired(player, admin = admin).parcelAt(player)
        ?: throw CommandException("You must be in a parcel to use that command")
    if (own) Validate.isTrue(parcel.isOwner(player.uuid) || player.hasAdminManage,
        "You must own this parcel to use that command")
    return parcel
}


