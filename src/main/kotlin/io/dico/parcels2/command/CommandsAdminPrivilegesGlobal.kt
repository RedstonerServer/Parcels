@file:Suppress("NON_EXHAUSTIVE_WHEN")

package io.dico.parcels2.command

import io.dico.dicore.command.ExecutionContext
import io.dico.dicore.command.Validate
import io.dico.dicore.command.annotation.Cmd
import io.dico.dicore.command.annotation.Desc
import io.dico.parcels2.*
import io.dico.parcels2.Privilege.BANNED
import io.dico.parcels2.Privilege.CAN_BUILD
import io.dico.parcels2.PrivilegeChangeResult.*
import io.dico.parcels2.defaultimpl.InfoBuilder
import io.dico.parcels2.util.ext.PERM_ADMIN_MANAGE
import org.bukkit.OfflinePlayer

class CommandsAdminPrivilegesGlobal(plugin: ParcelsPlugin) : AbstractParcelCommands(plugin) {
    private val data
        inline get() = plugin.globalPrivileges

    private fun checkContext(context: ExecutionContext, owner: OfflinePlayer, changing: Boolean = true): OfflinePlayer {
        if (changing) {
            checkConnected("have privileges changed")
        }
        val sender = context.sender
        if (sender !== owner) {
            Validate.isAuthorized(sender, PERM_ADMIN_MANAGE)
        }
        return owner
    }

    @Cmd("list", aliases = ["l"])
    @Desc(
        "List globally declared privileges, players you",
        "allowed to build on or banned from all your parcels",
        shortVersion = "lists globally declared privileges"
    )
    fun cmdList(context: ExecutionContext, owner: OfflinePlayer): Any? {
        checkContext(context, owner, changing = false)
        val map = plugin.globalPrivileges[owner]
        Validate.isTrue(map.hasAnyDeclaredPrivileges(), "This user has not declared any global privileges")

        return StringBuilder().apply {
            with(InfoBuilder) {
                appendProfilesWithPrivilege("Globally Allowed", map, null, CAN_BUILD)
                appendProfilesWithPrivilege("Globally Banned", map, null, BANNED)
            }
        }.toString()
    }

    @Cmd("entrust")
    @Desc(
        "Allows a player to manage globally",
        shortVersion = "allows a player to manage globally"
    )
    suspend fun cmdEntrust(context: ExecutionContext, owner: OfflinePlayer, player: PlayerProfile): Any? =
        when (data[checkContext(context, owner)].allowManage(toPrivilegeKey(player))) {
            FAIL_OWNER -> err("The target cannot be the owner themselves")
            FAIL -> err("${player.name} is already allowed to manage globally")
            SUCCESS -> "${player.name} is now allowed to manage globally"
        }

    @Cmd("distrust")
    @Desc(
        "Disallows a player to manage globally,",
        "they will still be able to build",
        shortVersion = "disallows a player to manage globally"
    )
    suspend fun cmdDistrust(context: ExecutionContext, owner: OfflinePlayer, player: PlayerProfile): Any? =
        when (data[checkContext(context, owner)].disallowManage(toPrivilegeKey(player))) {
            FAIL_OWNER -> err("The target cannot be the owner themselves")
            FAIL -> err("${player.name} is not currently allowed to manage globally")
            SUCCESS -> "${player.name} is not allowed to manage globally anymore"
        }

    @Cmd("allow", aliases = ["add", "permit"])
    @Desc(
        "Globally allows a player to build on all",
        "the parcels that you own.",
        shortVersion = "globally allows a player to build on your parcels"
    )
    suspend fun cmdAllow(context: ExecutionContext, owner: OfflinePlayer, player: PlayerProfile): Any? =
        when (data[checkContext(context, owner)].allowBuild(toPrivilegeKey(player))) {
            FAIL_OWNER -> err("The target cannot be the owner themselves")
            FAIL -> err("${player.name} is already allowed globally")
            SUCCESS -> "${player.name} is now allowed to build globally"
        }

    @Cmd("disallow", aliases = ["remove", "forbid"])
    @Desc(
        "Globally disallows a player to build on",
        "the parcels that you own.",
        "If the player is allowed to build on specific",
        "parcels, they can still build there.",
        shortVersion = "globally disallows a player to build on your parcels"
    )
    suspend fun cmdDisallow(context: ExecutionContext, owner: OfflinePlayer, player: PlayerProfile): Any? =
        when (data[checkContext(context, owner)].disallowBuild(toPrivilegeKey(player))) {
            FAIL_OWNER -> err("The target cannot be the owner themselves")
            FAIL -> err("${player.name} is not currently allowed globally")
            SUCCESS -> "${player.name} is not allowed to build globally anymore"
        }

    @Cmd("ban", aliases = ["deny"])
    @Desc(
        "Globally bans a player from all the parcels",
        "that you own, making them unable to enter.",
        shortVersion = "globally bans a player from your parcels"
    )
    suspend fun cmdBan(context: ExecutionContext, owner: OfflinePlayer, player: PlayerProfile): Any? =
        when (data[checkContext(context, owner)].disallowEnter(toPrivilegeKey(player))) {
            FAIL_OWNER -> err("The target cannot be the owner themselves")
            FAIL -> err("${player.name} is already banned globally")
            SUCCESS -> "${player.name} is now banned globally"
        }

    @Cmd("unban", aliases = ["undeny"])
    @Desc(
        "Globally unbans a player from all the parcels",
        "that you own, they can enter again.",
        "If the player is banned from specific parcels,",
        "they will still be banned there.",
        shortVersion = "globally unbans a player from your parcels"
    )
    suspend fun cmdUnban(context: ExecutionContext, owner: OfflinePlayer, player: PlayerProfile): Any? =
        when (data[checkContext(context, owner)].allowEnter(toPrivilegeKey(player))) {
            FAIL_OWNER -> err("The target cannot be the owner themselves")
            FAIL -> err("${player.name} is not currently banned globally")
            SUCCESS -> "${player.name} is not banned globally anymore"
        }

}