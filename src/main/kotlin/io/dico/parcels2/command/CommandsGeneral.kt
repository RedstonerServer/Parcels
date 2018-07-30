package io.dico.parcels2.command

import io.dico.dicore.command.EMessageType
import io.dico.dicore.command.ExecutionContext
import io.dico.dicore.command.annotation.Cmd
import io.dico.dicore.command.annotation.Desc
import io.dico.dicore.command.annotation.RequireParameters
import io.dico.parcels2.ParcelOwner
import io.dico.parcels2.ParcelsPlugin
import io.dico.parcels2.blockvisitor.JobUpdateListener
import io.dico.parcels2.command.NamedParcelDefaultValue.FIRST_OWNED
import io.dico.parcels2.storage.getParcelBySerializedValue
import io.dico.parcels2.util.hasAdminManage
import io.dico.parcels2.util.hasParcelHomeOthers
import io.dico.parcels2.util.uuid
import kotlinx.coroutines.experimental.Job
import org.bukkit.entity.Player

//@Suppress("unused")
class CommandsGeneral(plugin: ParcelsPlugin) : AbstractParcelCommands(plugin) {

    @Cmd("auto")
    @Desc("Finds the unclaimed parcel nearest to origin,",
        "and gives it to you",
        shortVersion = "sets you up with a fresh, unclaimed parcel")
    suspend fun WorldScope.cmdAuto(player: Player): Any? {
        checkConnected("be claimed")
        checkParcelLimit(player)

        val parcel = world.nextEmptyParcel()
            ?: error("This world is full, please ask an admin to upsize it")
        parcel.owner = ParcelOwner(uuid = player.uuid)
        player.teleport(parcel.homeLocation)
        return "Enjoy your new parcel!"
    }

    @Cmd("info", aliases = ["i"])
    @Desc("Displays general information",
        "about the parcel you're on",
        shortVersion = "displays information about this parcel")
    fun ParcelScope.cmdInfo(player: Player) = parcel.infoString

    @Cmd("home", aliases = ["h"])
    @Desc("Teleports you to your parcels,",
        "unless another player was specified.",
        "You can specify an index number if you have",
        "more than one parcel",
        shortVersion = "teleports you to parcels")
    @RequireParameters(0)
    suspend fun cmdHome(player: Player,
                        @NamedParcelDefault(FIRST_OWNED) target: NamedParcelTarget): Any? {
        if (player !== target.player && !player.hasParcelHomeOthers) {
            error("You do not have permission to teleport to other people's parcels")
        }

        val ownedParcelsResult = plugin.storage.getOwnedParcels(ParcelOwner(uuid = target.player.uuid)).await()

        val uuid = target.player.uuid
        val ownedParcels = ownedParcelsResult
            .map { worlds.getParcelBySerializedValue(it) }
            .filter { it != null && it.world == target.world && it.owner?.uuid == uuid }

        val targetMatch = ownedParcels.getOrNull(target.index)
            ?: error("The specified parcel could not be matched")

        player.teleport(targetMatch.homeLocation)
        return ""
    }

    @Cmd("claim")
    @Desc("If this parcel is unowned, makes you the owner",
        shortVersion = "claims this parcel")
    suspend fun ParcelScope.cmdClaim(player: Player): Any? {
        checkConnected("be claimed")
        parcel.owner.takeIf { !player.hasAdminManage }?.let {
            error(if (it.matches(player)) "You already own this parcel" else "This parcel is not available")
        }

        checkParcelLimit(player)
        parcel.owner = ParcelOwner(uuid = player.uuid, name = player.name)
        return "Enjoy your new parcel!"
    }

    @Cmd("clear")
    @ParcelRequire(owner = true)
    fun ParcelScope.cmdClear(player: Player, context: ExecutionContext) {
        val onProgressUpdate: JobUpdateListener = { progress, elapsedTime ->
            context.sendMessage("[Clearing] Progress: %.06f%%".format(progress * 100))
        }
        world.generator.clearParcel(parcel)
            .onProgressUpdate(5, 5) { progress, elapsedTime ->
                context.sendMessage(EMessageType.INFORMATIVE, "Clear progress: %.06f%%, %.2fs elapsed"
                    .format(progress * 100, elapsedTime / 1000.0))
            }
    }

}