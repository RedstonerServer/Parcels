package io.dico.parcels2.command

import io.dico.dicore.command.ExecutionContext
import io.dico.dicore.command.annotation.Cmd
import io.dico.dicore.command.annotation.Flag
import io.dico.parcels2.ParcelsPlugin
import io.dico.parcels2.PlayerProfile

class CommandsAdmin(plugin: ParcelsPlugin) : AbstractParcelCommands(plugin) {

    @Cmd("setowner")
    @ParcelRequire(admin = true)
    fun ParcelScope.cmdSetowner(target: PlayerProfile): Any? {
        parcel.owner = target

        val fakeString = if (target.isFake) " (fake)" else ""
        return "${target.notNullName}$fakeString is the new owner of (${parcel.id.idString})"
    }

    @Cmd("dispose")
    @ParcelRequire(admin = true)
    fun ParcelScope.cmdDispose(): Any? {
        parcel.dispose()
        return "Data of (${parcel.id.idString}) has been disposed"
    }

    @Cmd("reset")
    @ParcelRequire(admin = true)
    fun ParcelScope.cmdReset(context: ExecutionContext, @Flag sure: Boolean): Any? {
        if (!sure) return areYouSureMessage(context)
        parcel.dispose()
        clearWithProgressUpdates(context, "Reset")
        return null
    }

    @Cmd("swap")
    fun ParcelScope.cmdSwap(context: ExecutionContext, @Flag sure: Boolean): Any? {
        if (!sure) return areYouSureMessage(context)
        TODO()
    }


}