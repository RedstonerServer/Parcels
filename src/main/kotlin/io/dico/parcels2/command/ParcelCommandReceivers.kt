package io.dico.parcels2.command

import io.dico.dicore.command.CommandException
import io.dico.dicore.command.ExecutionContext
import io.dico.dicore.command.registration.reflect.ICommandReceiver
import io.dico.dicore.command.Validate
import io.dico.parcels2.*
import io.dico.parcels2.Privilege.*
import io.dico.parcels2.util.ext.hasPermAdminManage
import io.dico.parcels2.util.ext.uuid
import org.bukkit.entity.Player
import java.lang.reflect.Method
import kotlin.reflect.full.extensionReceiverParameter
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.jvm.jvmErasure
import kotlin.reflect.jvm.kotlinFunction

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class RequireParcelPrivilege(val privilege: Privilege)

/*
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class SuspensionTimeout(val millis: Int)
*/

open class WorldScope(val world: ParcelWorld) : ICommandReceiver
open class ParcelScope(val parcel: Parcel) : WorldScope(parcel.world) {
    fun checkCanManage(player: Player, action: String) = Validate.isTrue(parcel.canManage(player), "You must own this parcel to $action")
}

fun getParcelCommandReceiver(parcelProvider: ParcelProvider, context: ExecutionContext, method: Method, cmdName: String): ICommandReceiver {
    val player = context.sender as Player
    val function = method.kotlinFunction!!
    val receiverType = function.extensionReceiverParameter!!.type
    val require = function.findAnnotation<RequireParcelPrivilege>()

    return when (receiverType.jvmErasure) {
        ParcelScope::class -> ParcelScope(parcelProvider.getParcelRequired(player, require?.privilege))
        WorldScope::class -> WorldScope(parcelProvider.getWorldRequired(player, require?.privilege == ADMIN))
        else -> throw InternalError("Invalid command receiver type")
    }
}

fun ParcelProvider.getWorldRequired(player: Player, admin: Boolean = false): ParcelWorld {
    if (admin) Validate.isTrue(player.hasPermAdminManage, "You must have admin rights to use that command")
    return getWorld(player.world)
        ?: throw CommandException("You must be in a parcel world to use that command")
}

fun ParcelProvider.getParcelRequired(player: Player, privilege: Privilege? = null): Parcel {
    val parcel = getWorldRequired(player, admin = privilege == ADMIN).getParcelAt(player)
        ?: throw CommandException("You must be in a parcel to use that command")

    if (!player.hasPermAdminManage) {
        @Suppress("NON_EXHAUSTIVE_WHEN")
        when (privilege) {
            OWNER ->
                Validate.isTrue(parcel.isOwner(player.uuid), "You must own this parcel to use that command")
            CAN_MANAGE ->
                Validate.isTrue(parcel.canManage(player), "You must have management privileges on this parcel to use that command")
        }
    }

    return parcel
}

