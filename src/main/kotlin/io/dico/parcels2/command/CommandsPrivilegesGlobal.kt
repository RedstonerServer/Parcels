@file:Suppress("NON_EXHAUSTIVE_WHEN")

package io.dico.parcels2.command

import io.dico.dicore.command.annotation.Cmd
import io.dico.dicore.command.annotation.Desc
import io.dico.parcels2.ParcelsPlugin
import io.dico.parcels2.PrivilegeChangeResult.*
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player

class CommandsPrivilegesGlobal(plugin: ParcelsPlugin) : AbstractParcelCommands(plugin) {
    private val data
        inline get() = plugin.globalPrivileges

    @Cmd("entrust")
    @Desc(
        "Allows a player to manage this parcel",
        shortVersion = "allows a player to manage this parcel"
    )
    fun cmdEntrust(sender: Player, player: OfflinePlayer): Any? =
        when (data[sender].allowManage(player)) {
            FAIL_OWNER -> err("The target cannot be yourself")
            FAIL -> err("${player.name} is already allowed to manage globally")
            SUCCESS -> "${player.name} is now allowed to manage globally"
        }

    @Cmd("distrust")
    @Desc(
        "Disallows a player to manage globally,",
        "they will still be able to build",
        shortVersion = "disallows a player to manage globally"
    )
    fun cmdDistrust(sender: Player, player: OfflinePlayer): Any? =
        when (data[sender].disallowManage(player)) {
            FAIL_OWNER -> err("The target cannot be yourself")
            FAIL -> err("${player.name} is not currently allowed to manage globally")
            SUCCESS -> "${player.name} is not allowed to manage globally anymore"
        }

    @Cmd("allow", aliases = ["add", "permit"])
    @Desc(
        "Globally allows a player to build on all",
        "the parcels that you own.",
        shortVersion = "globally allows a player to build on your parcels"
    )
    fun cmdAllow(sender: Player, player: OfflinePlayer): Any? =
        when (data[sender].allowBuild(player)) {
            FAIL_OWNER -> err("The target cannot be yourself")
            FAIL -> err("${player.name} is already allowed globally")
            SUCCESS -> "${player.name} is now allowed to build on all your parcels"
        }

    @Cmd("disallow", aliases = ["remove", "forbid"])
    @Desc(
        "Globally disallows a player to build on",
        "the parcels that you own.",
        "If the player is allowed to build on specific",
        "parcels, they can still build there.",
        shortVersion = "globally disallows a player to build on your parcels"
    )
    fun cmdDisallow(sender: Player, player: OfflinePlayer): Any? =
        when (data[sender].disallowBuild(player)) {
            FAIL_OWNER -> err("The target cannot be yourself")
            FAIL -> err("${player.name} is not currently allowed globally")
            SUCCESS -> "${player.name} is not allowed to build on all your parcels anymore"
        }

    @Cmd("ban", aliases = ["deny"])
    @Desc(
        "Globally bans a player from all the parcels",
        "that you own, making them unable to enter.",
        shortVersion = "globally bans a player from your parcels"
    )
    fun cmdBan(sender: Player, player: OfflinePlayer): Any? =
        when (data[sender].ban(player)) {
            FAIL_OWNER -> err("The target cannot be yourself")
            FAIL -> err("${player.name} is already banned from all your parcels")
            SUCCESS -> "${player.name} is now banned from all your parcels"
        }

    @Cmd("unban", aliases = ["undeny"])
    @Desc(
        "Globally unbans a player from all the parcels",
        "that you own, they can enter again.",
        "If the player is banned from specific parcels,",
        "they will still be banned there.",
        shortVersion = "globally unbans a player from your parcels"
    )
    fun cmdUnban(sender: Player, player: OfflinePlayer): Any? =
        when (data[sender].unban(player)) {
            FAIL_OWNER -> err("The target cannot be yourself")
            FAIL -> err("${player.name} is not currently banned from all your parcels")
            SUCCESS -> "${player.name} is not banned from all your parcels anymore"
        }

}