package io.dico.parcels2.command

import io.dico.dicore.command.*
import io.dico.parcels2.Parcel
import io.dico.parcels2.ParcelWorld
import io.dico.parcels2.Worlds
import io.dico.parcels2.logger
import io.dico.parcels2.util.hasAdminManage
import io.dico.parcels2.util.uuid
import org.bukkit.entity.Player
import java.lang.reflect.Method
import kotlin.reflect.full.extensionReceiverParameter
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.jvm.javaType
import kotlin.reflect.jvm.jvmErasure
import kotlin.reflect.jvm.kotlinFunction

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class ParcelRequire(val admin: Boolean = false, val owner: Boolean = false)

sealed class BaseScope(private var _timeout: Int = 0) : ICommandSuspendReceiver {
    override fun getTimeout() = _timeout
    fun setTimeout(timeout: Int) {
        _timeout = timeout
    }
}

class SuspendOnlyScope : BaseScope()
class ParcelScope(val world: ParcelWorld, val parcel: Parcel) : BaseScope()
class WorldOnlyScope(val world: ParcelWorld) : BaseScope()

fun getParcelCommandReceiver(worlds: Worlds, context: ExecutionContext, method: Method, cmdName: String): ICommandReceiver {
    val function = method.kotlinFunction!!
    val receiverType = function.extensionReceiverParameter!!.type
    logger.info("Receiver type: ${receiverType.javaType.typeName}")

    val require = function.findAnnotation<ParcelRequire>()
    val admin = require?.admin == true
    val owner = require?.owner == true

    val player = context.sender as Player

    return when (receiverType.jvmErasure) {
        ParcelScope::class -> worlds.getParcelRequired(player, admin = admin, own = owner).let {
            ParcelScope(it.world, it)
        }
        WorldOnlyScope::class -> worlds.getWorldRequired(player, admin = admin).let {
            WorldOnlyScope(it)
        }
        SuspendOnlyScope::class -> SuspendOnlyScope()
        else -> throw InternalError("Invalid command receiver type")

    }
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

