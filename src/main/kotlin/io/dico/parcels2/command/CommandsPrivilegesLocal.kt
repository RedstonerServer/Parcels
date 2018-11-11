package io.dico.parcels2.command

import io.dico.dicore.command.Validate
import io.dico.dicore.command.annotation.Cmd
import io.dico.dicore.command.annotation.Desc
import io.dico.parcels2.*
import io.dico.parcels2.PrivilegeChangeResult.*
import io.dico.parcels2.util.ext.PERM_ADMIN_MANAGE
import io.dico.parcels2.util.ext.hasPermAdminManage
import org.bukkit.entity.Player

class CommandsPrivilegesLocal(plugin: ParcelsPlugin) : AbstractParcelCommands(plugin) {

    private fun ParcelScope.checkPrivilege(sender: Player, key: PrivilegeKey) {
        val senderPrivilege = parcel.getEffectivePrivilege(sender, PERM_ADMIN_MANAGE)
        val targetPrivilege = parcel.getStoredPrivilege(key)
        Validate.isTrue(senderPrivilege > targetPrivilege, "You may not change the privilege of ${key.notNullName}")
    }

    private fun ParcelScope.checkOwned(sender: Player) {
        Validate.isTrue(parcel.owner != null || sender.hasPermAdminManage, "This parcel is unowned")
    }

    @Cmd("entrust")
    @Desc(
        "Allows a player to manage this parcel",
        shortVersion = "allows a player to manage this parcel"
    )
    @RequireParcelPrivilege(Privilege.OWNER)
    suspend fun ParcelScope.cmdEntrust(sender: Player, player: PlayerProfile): Any? {
        checkConnected("have privileges changed")
        checkOwned(sender)

        val key = toPrivilegeKey(player)
        return when (parcel.allowManage(key)) {
            FAIL_OWNER -> err("The target already owns the parcel")
            FAIL -> err("${player.name} is already allowed to manage this parcel")
            SUCCESS -> "${player.name} is now allowed to manage this parcel"
        }
    }

    @Cmd("distrust")
    @Desc(
        "Disallows a player to manage this parcel,",
        "they will still be able to build",
        shortVersion = "disallows a player to manage this parcel"
    )
    @RequireParcelPrivilege(Privilege.OWNER)
    suspend fun ParcelScope.cmdDistrust(sender: Player, player: PlayerProfile): Any? {
        checkConnected("have privileges changed")
        checkOwned(sender)

        val key = toPrivilegeKey(player)
        return when (parcel.disallowManage(key)) {
            FAIL_OWNER -> err("The target owns the parcel and can't be distrusted")
            FAIL -> err("${player.name} is not currently allowed to manage this parcel")
            SUCCESS -> "${player.name} is not allowed to manage this parcel anymore"
        }
    }

    @Cmd("allow", aliases = ["add", "permit"])
    @Desc(
        "Allows a player to build on this parcel",
        shortVersion = "allows a player to build on this parcel"
    )
    @RequireParcelPrivilege(Privilege.CAN_MANAGE)
    suspend fun ParcelScope.cmdAllow(sender: Player, player: PlayerProfile): Any? {
        checkConnected("have privileges changed")
        checkOwned(sender)

        val key = toPrivilegeKey(player)
        checkPrivilege(sender, key)

        return when (parcel.allowBuild(key)) {
            FAIL_OWNER -> err("The target already owns the parcel")
            FAIL -> err("${player.name} is already allowed to build on this parcel")
            SUCCESS -> "${player.name} is now allowed to build on this parcel"
        }
    }

    @Cmd("disallow", aliases = ["remove", "forbid"])
    @Desc(
        "Disallows a player to build on this parcel,",
        "they won't be allowed to anymore",
        shortVersion = "disallows a player to build on this parcel"
    )
    @RequireParcelPrivilege(Privilege.CAN_MANAGE)
    suspend fun ParcelScope.cmdDisallow(sender: Player, player: PlayerProfile): Any? {
        checkConnected("have privileges changed")
        checkOwned(sender)

        val key = toPrivilegeKey(player)
        checkPrivilege(sender, key)

        return when (parcel.disallowBuild(key)) {
            FAIL_OWNER -> err("The target owns the parcel")
            FAIL -> err("${player.name} is not currently allowed to build on this parcel")
            SUCCESS -> "${player.name} is not allowed to build on this parcel anymore"
        }
    }

    @Cmd("ban", aliases = ["deny"])
    @Desc(
        "Bans a player from this parcel,",
        "making them unable to enter",
        shortVersion = "bans a player from this parcel"
    )
    @RequireParcelPrivilege(Privilege.CAN_MANAGE)
    suspend fun ParcelScope.cmdBan(sender: Player, player: PlayerProfile): Any? {
        checkConnected("have privileges changed")
        checkOwned(sender)

        val key = toPrivilegeKey(player)
        checkPrivilege(sender, key)

        return when (parcel.disallowEnter(key)) {
            FAIL_OWNER -> err("The target owns the parcel")
            FAIL -> err("${player.name} is already banned from this parcel")
            SUCCESS -> "${player.name} is now banned from this parcel"
        }
    }

    @Cmd("unban", aliases = ["undeny"])
    @Desc(
        "Unbans a player from this parcel,",
        "they will be able to enter it again",
        shortVersion = "unbans a player from this parcel"
    )
    @RequireParcelPrivilege(Privilege.CAN_MANAGE)
    suspend fun ParcelScope.cmdUnban(sender: Player, player: PlayerProfile): Any? {
        checkConnected("have privileges changed")
        checkOwned(sender)

        val key = toPrivilegeKey(player)
        checkPrivilege(sender, key)

        return when (parcel.allowEnter(key)) {
            FAIL_OWNER -> err("The target owns the parcel")
            FAIL -> err("${player.name} is not currently banned from this parcel")
            SUCCESS -> "${player.name} is not banned from this parcel anymore"
        }
    }

}