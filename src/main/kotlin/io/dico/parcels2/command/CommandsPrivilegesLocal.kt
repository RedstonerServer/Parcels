package io.dico.parcels2.command

import io.dico.dicore.command.Validate
import io.dico.dicore.command.annotation.Cmd
import io.dico.dicore.command.annotation.Desc
import io.dico.parcels2.ParcelsPlugin
import io.dico.parcels2.Privilege
import io.dico.parcels2.util.ext.hasPermAdminManage
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player

class CommandsPrivilegesLocal(plugin: ParcelsPlugin) : AbstractParcelCommands(plugin) {

    @Cmd("entrust")
    @Desc(
        "Allows a player to manage this parcel",
        shortVersion = "allows a player to manage this parcel"
    )
    @RequireParcelPrivilege(Privilege.OWNER)
    fun ParcelScope.cmdEntrust(sender: Player, player: OfflinePlayer): Any? {
        Validate.isTrue(parcel.owner != null || sender.hasPermAdminManage, "This parcel is unowned")
        Validate.isTrue(!parcel.owner!!.matches(player), "The target already owns the parcel")
        Validate.isTrue(parcel.allowManage(player), "${player.name} is already allowed to manage this parcel")
        return "${player.name} is now allowed to manage this parcel"
    }

    @Cmd("distrust")
    @Desc(
        "Disallows a player to manage this parcel,",
        "they will still be able to build",
        shortVersion = "disallows a player to manage this parcel"
    )
    @RequireParcelPrivilege(Privilege.OWNER)
    fun ParcelScope.cmdDistrust(sender: Player, player: OfflinePlayer): Any? {
        Validate.isTrue(parcel.disallowManage(player), "${player.name} is not currently allowed to manage this parcel")
        return "${player.name} is not allowed to manage this parcel anymore"
    }

    @Cmd("allow", aliases = ["add", "permit"])
    @Desc(
        "Allows a player to build on this parcel",
        shortVersion = "allows a player to build on this parcel"
    )
    @RequireParcelPrivilege(Privilege.CAN_MANAGE)
    fun ParcelScope.cmdAllow(sender: Player, player: OfflinePlayer): Any? {
        Validate.isTrue(parcel.owner != null || sender.hasPermAdminManage, "This parcel is unowned")
        Validate.isTrue(!parcel.owner!!.matches(player), "The target already owns the parcel")
        Validate.isTrue(parcel.allowBuild(player), "${player.name} is already allowed to build on this parcel")
        return "${player.name} is now allowed to build on this parcel"
    }

    @Cmd("disallow", aliases = ["remove", "forbid"])
    @Desc(
        "Disallows a player to build on this parcel,",
        "they won't be allowed to anymore",
        shortVersion = "disallows a player to build on this parcel"
    )
    @RequireParcelPrivilege(Privilege.CAN_MANAGE)
    fun ParcelScope.cmdDisallow(sender: Player, player: OfflinePlayer): Any? {
        Validate.isTrue(parcel.disallowBuild(player), "${player.name} is not currently allowed to build on this parcel")
        return "${player.name} is not allowed to build on this parcel anymore"
    }

    @Cmd("ban", aliases = ["deny"])
    @Desc(
        "Bans a player from this parcel,",
        "making them unable to enter",
        shortVersion = "bans a player from this parcel"
    )
    @RequireParcelPrivilege(Privilege.CAN_MANAGE)
    fun ParcelScope.cmdBan(sender: Player, player: OfflinePlayer): Any? {
        Validate.isTrue(parcel.owner != null || sender.hasPermAdminManage, "This parcel is unowned")
        Validate.isTrue(!parcel.owner!!.matches(player), "The owner cannot be banned from the parcel")
        Validate.isTrue(parcel.ban(player), "${player.name} is already banned from this parcel")
        return "${player.name} is now banned from this parcel"
    }

    @Cmd("unban", aliases = ["undeny"])
    @Desc(
        "Unbans a player from this parcel,",
        "they will be able to enter it again",
        shortVersion = "unbans a player from this parcel"
    )
    @RequireParcelPrivilege(Privilege.CAN_MANAGE)
    fun ParcelScope.cmdUnban(sender: Player, player: OfflinePlayer): Any? {
        Validate.isTrue(parcel.unban(player), "${player.name} is not currently banned from this parcel")
        return "${player.name} is not banned from this parcel anymore"
    }

}