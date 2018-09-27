package io.dico.parcels2.command

import io.dico.dicore.command.CommandException
import io.dico.dicore.command.ExecutionContext
import io.dico.dicore.command.Validate
import io.dico.dicore.command.annotation.Cmd
import io.dico.dicore.command.annotation.Flag
import io.dico.parcels2.ParcelsPlugin
import io.dico.parcels2.PlayerProfile
import io.dico.parcels2.Privilege
import io.dico.parcels2.command.ParcelTarget.TargetKind
import io.dico.parcels2.resolved

class CommandsAdmin(plugin: ParcelsPlugin) : AbstractParcelCommands(plugin) {

    @Cmd("setowner")
    @RequireParcelPrivilege(Privilege.ADMIN)
    suspend fun ParcelScope.cmdSetowner(@ProfileKind(ProfileKind.ANY) target: PlayerProfile): Any? {
        val profile = target.resolved(plugin.storage, resolveToFake = true)!!
        parcel.owner = profile

        val fakeString = if (profile.isFake) " (fake)" else ""
        return "${profile.notNullName}$fakeString is the new owner of (${parcel.id.idString})"
    }

    @Cmd("dispose")
    @RequireParcelPrivilege(Privilege.ADMIN)
    fun ParcelScope.cmdDispose(): Any? {
        parcel.dispose()
        return "Data of (${parcel.id.idString}) has been disposed"
    }

    @Cmd("reset")
    @RequireParcelPrivilege(Privilege.ADMIN)
    fun ParcelScope.cmdReset(context: ExecutionContext, @Flag sure: Boolean): Any? {
        Validate.isTrue(!parcel.hasBlockVisitors, "A process is already running in this parcel")
        if (!sure) return areYouSureMessage(context)

        parcel.dispose()
        world.blockManager.clearParcel(parcel.id).reportProgressUpdates(context, "Reset")
        return "Data of (${parcel.id.idString}) has been disposed"
    }

    @Cmd("swap")
    @RequireParcelPrivilege(Privilege.ADMIN)
    fun ParcelScope.cmdSwap(context: ExecutionContext,
                            @TargetKind(TargetKind.ID) target: ParcelTarget,
                            @Flag sure: Boolean): Any? {
        Validate.isTrue(!parcel.hasBlockVisitors, "A process is already running in this parcel")
        if (!sure) return areYouSureMessage(context)

        val parcel2 = (target as ParcelTarget.ByID).getParcel()
            ?: throw CommandException("Invalid parcel target")

        // Validate.isTrue(parcel2.world == world, "Parcel must be in the same world")
        Validate.isTrue(!parcel2.hasBlockVisitors, "A process is already running in this parcel")

        val data = parcel.data
        parcel.copyData(parcel2.data)
        parcel2.copyData(data)

        world.blockManager.swapParcels(parcel.id, parcel2.id).reportProgressUpdates(context, "Swap")
        return null
    }

}