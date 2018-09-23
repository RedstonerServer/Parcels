package io.dico.parcels2.command

import io.dico.dicore.command.ExecutionContext
import io.dico.dicore.command.Validate
import io.dico.dicore.command.annotation.Cmd
import io.dico.dicore.command.annotation.Desc
import io.dico.dicore.command.annotation.Flag
import io.dico.dicore.command.annotation.RequireParameters
import io.dico.parcels2.ParcelsPlugin
import io.dico.parcels2.PlayerProfile
import io.dico.parcels2.command.ParcelTarget.Kind
import io.dico.parcels2.util.ext.hasAdminManage
import io.dico.parcels2.util.ext.hasParcelHomeOthers
import io.dico.parcels2.util.ext.uuid
import org.bukkit.block.Biome
import org.bukkit.entity.Player

class CommandsGeneral(plugin: ParcelsPlugin) : AbstractParcelCommands(plugin) {

    @Cmd("auto")
    @Desc(
        "Finds the unclaimed parcel nearest to origin,",
        "and gives it to you",
        shortVersion = "sets you up with a fresh, unclaimed parcel"
    )
    suspend fun WorldScope.cmdAuto(player: Player): Any? {
        checkConnected("be claimed")
        checkParcelLimit(player, world)

        val parcel = world.nextEmptyParcel()
            ?: error("This world is full, please ask an admin to upsize it")
        parcel.owner = PlayerProfile(uuid = player.uuid)
        player.teleport(parcel.homeLocation)
        return "Enjoy your new parcel!"
    }

    @Cmd("info", aliases = ["i"])
    @Desc(
        "Displays general information",
        "about the parcel you're on",
        shortVersion = "displays information about this parcel"
    )
    fun ParcelScope.cmdInfo(player: Player) = parcel.infoString

    @Cmd("home", aliases = ["h"])
    @Desc(
        "Teleports you to your parcels,",
        "unless another player was specified.",
        "You can specify an index number if you have",
        "more than one parcel",
        shortVersion = "teleports you to parcels"
    )
    @RequireParameters(0)
    suspend fun cmdHome(
        player: Player,
        @Kind(ParcelTarget.OWNER_REAL) target: ParcelTarget
    ): Any? {
        return cmdGoto(player, target)
    }

    @Cmd("tp", aliases = ["teleport"])
    suspend fun cmdTp(
        player: Player,
        @Kind(ParcelTarget.ID) target: ParcelTarget
    ): Any? {
        return cmdGoto(player, target)
    }

    @Cmd("goto")
    suspend fun cmdGoto(
        player: Player,
        @Kind(ParcelTarget.ANY) target: ParcelTarget
    ): Any? {
        if (target is ParcelTarget.ByOwner) {
            target.resolveOwner(plugin.storage)
            if (!target.owner.matches(player) && !player.hasParcelHomeOthers) {
                error("You do not have permission to teleport to other people's parcels")
            }
        }

        val match = target.getParcelSuspend(plugin.storage)
            ?: error("The specified parcel could not be matched")
        player.teleport(match.homeLocation)
        return null
    }

    @Cmd("goto_fake")
    suspend fun cmdGotoFake(
        player: Player,
        @Kind(ParcelTarget.OWNER_FAKE) target: ParcelTarget
    ): Any? {
        return cmdGoto(player, target)
    }

    @Cmd("claim")
    @Desc(
        "If this parcel is unowned, makes you the owner",
        shortVersion = "claims this parcel"
    )
    suspend fun ParcelScope.cmdClaim(player: Player): Any? {
        checkConnected("be claimed")
        parcel.owner.takeIf { !player.hasAdminManage }?.let {
            error(if (it.matches(player)) "You already own this parcel" else "This parcel is not available")
        }

        checkParcelLimit(player, world)
        parcel.owner = PlayerProfile(player)
        return "Enjoy your new parcel!"
    }

    @Cmd("unclaim")
    @Desc("Unclaims this parcel")
    @ParcelRequire(owner = true)
    fun ParcelScope.cmdUnclaim(player: Player): Any? {
        parcel.dispose()
        return "Your parcel has been disposed"
    }

    @Cmd("clear")
    @ParcelRequire(owner = true)
    fun ParcelScope.cmdClear(context: ExecutionContext, @Flag sure: Boolean): Any? {
        if (!sure) return areYouSureMessage(context)

        clearWithProgressUpdates(context, "Clear")
        return null
    }

    @Cmd("setbiome")
    @ParcelRequire(owner = true)
    fun ParcelScope.cmdSetbiome(context: ExecutionContext, biome: Biome): Any? {
        Validate.isTrue(!parcel.hasBlockVisitors, "A process is already running in this parcel")
        world.blockManager.setBiome(parcel.id, biome)
        return "Biome has been set to $biome"
    }

}