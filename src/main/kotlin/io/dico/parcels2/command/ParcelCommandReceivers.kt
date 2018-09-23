package io.dico.parcels2.command

import io.dico.dicore.command.CommandException
import io.dico.dicore.command.ExecutionContext
import io.dico.dicore.command.ICommandReceiver
import io.dico.dicore.command.Validate
import io.dico.parcels2.Parcel
import io.dico.parcels2.ParcelProvider
import io.dico.parcels2.ParcelWorld
import io.dico.parcels2.util.ext.hasAdminManage
import io.dico.parcels2.util.ext.uuid
import org.bukkit.entity.Player
import java.lang.reflect.Method
import kotlin.reflect.full.extensionReceiverParameter
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.jvm.jvmErasure
import kotlin.reflect.jvm.kotlinFunction

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class ParcelRequire(val admin: Boolean = false, val owner: Boolean = false)

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class SuspensionTimeout(val millis: Int)

open class WorldScope(val world: ParcelWorld) : ICommandReceiver
open class ParcelScope(val parcel: Parcel) : WorldScope(parcel.world) {
    fun checkCanManage(player: Player, action: String) = Validate.isTrue(player.hasAdminManage || parcel.isOwner(player.uuid),
        "You must own this parcel to $action")
}

fun getParcelCommandReceiver(parcelProvider: ParcelProvider, context: ExecutionContext, method: Method, cmdName: String): ICommandReceiver {
    val player = context.sender as Player
    val function = method.kotlinFunction!!
    val receiverType = function.extensionReceiverParameter!!.type
    val require = function.findAnnotation<ParcelRequire>()
    val admin = require?.admin == true
    val owner = require?.owner == true

    return when (receiverType.jvmErasure) {
        ParcelScope::class -> ParcelScope(parcelProvider.getParcelRequired(player, admin, owner))
        WorldScope::class -> WorldScope(parcelProvider.getWorldRequired(player, admin))
        else -> throw InternalError("Invalid command receiver type")
    }
}

fun ParcelProvider.getWorldRequired(player: Player, admin: Boolean = false): ParcelWorld {
    if (admin) Validate.isTrue(player.hasAdminManage, "You must have admin rights to use that command")
    return getWorld(player.world)
        ?: throw CommandException("You must be in a parcel world to use that command")
}

fun ParcelProvider.getParcelRequired(player: Player, admin: Boolean = false, own: Boolean = false): Parcel {
    val parcel = getWorldRequired(player, admin = admin).getParcelAt(player)
        ?: throw CommandException("You must be in a parcel to use that command")
    if (own) Validate.isTrue(parcel.isOwner(player.uuid) || player.hasAdminManage,
        "You must own this parcel to use that command")
    return parcel
}

