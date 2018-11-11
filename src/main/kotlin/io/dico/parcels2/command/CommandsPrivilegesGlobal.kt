package io.dico.parcels2.command

import io.dico.dicore.command.ExecutionContext
import io.dico.dicore.command.annotation.Cmd
import io.dico.dicore.command.annotation.Desc
import io.dico.parcels2.ParcelsPlugin
import io.dico.parcels2.PlayerProfile
import org.bukkit.entity.Player

class CommandsPrivilegesGlobal(plugin: ParcelsPlugin,
                               val adminVersion: CommandsAdminPrivilegesGlobal) : AbstractParcelCommands(plugin) {
    @Cmd("list", aliases = ["l"])
    @Desc(
        "List globally declared privileges, players you",
        "allowed to build on or banned from all your parcels",
        shortVersion = "lists globally declared privileges"
    )
    fun cmdList(sender: Player, context: ExecutionContext) =
        adminVersion.cmdList(context, sender)

    @Cmd("entrust")
    @Desc(
        "Allows a player to manage globally",
        shortVersion = "allows a player to manage globally"
    )
    suspend fun cmdEntrust(sender: Player, context: ExecutionContext, player: PlayerProfile) =
        adminVersion.cmdEntrust(context, sender, player)

    @Cmd("distrust")
    @Desc(
        "Disallows a player to manage globally,",
        "they will still be able to build",
        shortVersion = "disallows a player to manage globally"
    )
    suspend fun cmdDistrust(sender: Player, context: ExecutionContext, player: PlayerProfile) =
        adminVersion.cmdDistrust(context, sender, player)

    @Cmd("allow", aliases = ["add", "permit"])
    @Desc(
        "Globally allows a player to build on all",
        "the parcels that you own.",
        shortVersion = "globally allows a player to build on your parcels"
    )
    suspend fun cmdAllow(sender: Player, context: ExecutionContext, player: PlayerProfile) =
        adminVersion.cmdAllow(context, sender, player)

    @Cmd("disallow", aliases = ["remove", "forbid"])
    @Desc(
        "Globally disallows a player to build on",
        "the parcels that you own.",
        "If the player is allowed to build on specific",
        "parcels, they can still build there.",
        shortVersion = "globally disallows a player to build on your parcels"
    )
    suspend fun cmdDisallow(sender: Player, context: ExecutionContext, player: PlayerProfile) =
        adminVersion.cmdDisallow(context, sender, player)

    @Cmd("ban", aliases = ["deny"])
    @Desc(
        "Globally bans a player from all the parcels",
        "that you own, making them unable to enter.",
        shortVersion = "globally bans a player from your parcels"
    )
    suspend fun cmdBan(sender: Player, context: ExecutionContext, player: PlayerProfile) =
        adminVersion.cmdBan(context, sender, player)

    @Cmd("unban", aliases = ["undeny"])
    @Desc(
        "Globally unbans a player from all the parcels",
        "that you own, they can enter again.",
        "If the player is banned from specific parcels,",
        "they will still be banned there.",
        shortVersion = "globally unbans a player from your parcels"
    )
    suspend fun cmdUnban(sender: Player, context: ExecutionContext, player: PlayerProfile) =
        adminVersion.cmdUnban(context, sender, player)
}