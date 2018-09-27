package io.dico.parcels2.command

import io.dico.dicore.command.ExecutionContext
import io.dico.dicore.command.Validate
import io.dico.dicore.command.annotation.Cmd
import io.dico.dicore.command.annotation.Desc
import io.dico.dicore.command.annotation.Flag
import io.dico.dicore.command.annotation.RequireParameters
import io.dico.parcels2.ParcelsPlugin
import io.dico.parcels2.PlayerProfile
import io.dico.parcels2.Privilege
import io.dico.parcels2.command.ParcelTarget.TargetKind
import io.dico.parcels2.util.ext.hasParcelHomeOthers
import io.dico.parcels2.util.ext.hasPermAdminManage
import io.dico.parcels2.util.ext.uuid
import org.bukkit.block.Biome
import org.bukkit.entity.Player

class CommandsGeneral(plugin: ParcelsPlugin, parent: SpecialCommandAddress) : AbstractParcelCommands(plugin) {

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
            ?: err("This world is full, please ask an admin to upsize it")
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

    init {
        parent.addSpeciallyTreatedKeys("home", "h")
    }

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
        @TargetKind(TargetKind.OWNER_REAL) target: ParcelTarget
    ): Any? {
        return cmdGoto(player, target)
    }

    @Cmd("tp", aliases = ["teleport"])
    suspend fun cmdTp(
        player: Player,
        @TargetKind(TargetKind.ID) target: ParcelTarget
    ): Any? {
        return cmdGoto(player, target)
    }

    @Cmd("goto")
    suspend fun cmdGoto(
        player: Player,
        @TargetKind(TargetKind.ANY) target: ParcelTarget
    ): Any? {
        if (target is ParcelTarget.ByOwner) {
            target.resolveOwner(plugin.storage)
            if (!target.owner.matches(player) && !player.hasParcelHomeOthers) {
                err("You do not have permission to teleport to other people's parcels")
            }
        }

        val match = target.getParcelSuspend(plugin.storage)
            ?: err("The specified parcel could not be matched")
        player.teleport(match.homeLocation)
        return null
    }

    @Cmd("goto_fake")
    suspend fun cmdGotoFake(
        player: Player,
        @TargetKind(TargetKind.OWNER_FAKE) target: ParcelTarget
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
        parcel.owner.takeIf { !player.hasPermAdminManage }?.let {
            err(if (it.matches(player)) "You already own this parcel" else "This parcel is not available")
        }

        checkParcelLimit(player, world)
        parcel.owner = PlayerProfile(player)
        return "Enjoy your new parcel!"
    }

    @Cmd("unclaim")
    @Desc("Unclaims this parcel")
    @RequireParcelPrivilege(Privilege.OWNER)
    fun ParcelScope.cmdUnclaim(player: Player): Any? {
        checkConnected("be unclaimed")
        parcel.dispose()
        return "Your parcel has been disposed"
    }

    @Cmd("clear")
    @RequireParcelPrivilege(Privilege.OWNER)
    fun ParcelScope.cmdClear(context: ExecutionContext, @Flag sure: Boolean): Any? {
        Validate.isTrue(!parcel.hasBlockVisitors, "A process is already running in this parcel")
        if (!sure) return areYouSureMessage(context)
        world.blockManager.clearParcel(parcel.id).reportProgressUpdates(context, "Clear")
        return null
    }

    @Cmd("setbiome")
    @RequireParcelPrivilege(Privilege.OWNER)
    fun ParcelScope.cmdSetbiome(context: ExecutionContext, biome: Biome): Any? {
        Validate.isTrue(!parcel.hasBlockVisitors, "A process is already running in this parcel")
        world.blockManager.setBiome(parcel.id, biome).reportProgressUpdates(context, "Biome change")
        return null
    }

}