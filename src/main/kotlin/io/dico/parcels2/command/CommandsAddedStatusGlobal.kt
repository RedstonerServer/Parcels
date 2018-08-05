package io.dico.parcels2.command

import io.dico.dicore.command.Validate
import io.dico.dicore.command.annotation.Cmd
import io.dico.dicore.command.annotation.Desc
import io.dico.parcels2.GlobalAddedData
import io.dico.parcels2.GlobalAddedDataManager
import io.dico.parcels2.PlayerProfile
import io.dico.parcels2.ParcelsPlugin
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player

class CommandsAddedStatusGlobal(plugin: ParcelsPlugin) : AbstractParcelCommands(plugin) {
    private inline val data get() = plugin.globalAddedData
    @Suppress("NOTHING_TO_INLINE")
    private inline operator fun GlobalAddedDataManager.get(player: OfflinePlayer): GlobalAddedData = data[PlayerProfile(player)]

    @Cmd("allow", aliases = ["add", "permit"])
    @Desc("Globally allows a player to build on all",
        "the parcels that you own.",
        shortVersion = "globally allows a player to build on your parcels")
    @ParcelRequire(owner = true)
    fun cmdAllow(sender: Player, player: OfflinePlayer): Any? {
        Validate.isTrue(player != sender, "The target cannot be yourself")
        Validate.isTrue(data[sender].allow(player), "${player.name} is already allowed globally")
        return "${player.name} is now allowed to build on all your parcels"
    }

    @Cmd("disallow", aliases = ["remove", "forbid"])
    @Desc("Globally disallows a player to build on",
        "the parcels that you own.",
        "If the player is allowed to build on specific",
        "parcels, they can still build there.",
        shortVersion = "globally disallows a player to build on your parcels")
    @ParcelRequire(owner = true)
    fun cmdDisallow(sender: Player, player: OfflinePlayer): Any? {
        Validate.isTrue(player != sender, "The target cannot be yourself")
        Validate.isTrue(data[sender].disallow(player), "${player.name} is not currently allowed globally")
        return "${player.name} is not allowed to build on all your parcels anymore"
    }

    @Cmd("ban", aliases = ["deny"])
    @Desc("Globally bans a player from all the parcels",
        "that you own, making them unable to enter.",
        shortVersion = "globally bans a player from your parcels")
    @ParcelRequire(owner = true)
    fun cmdBan(sender: Player, player: OfflinePlayer): Any? {
        Validate.isTrue(player != sender, "The target cannot be yourself")
        Validate.isTrue(data[sender].ban(player), "${player.name} is already banned from all your parcels")
        return "${player.name} is now banned from all your parcels"
    }

    @Cmd("unban", aliases = ["undeny"])
    @Desc("Globally unbans a player from all the parcels",
        "that you own, they can enter again.",
        "If the player is banned from specific parcels,",
        "they will still be banned there.",
        shortVersion = "globally unbans a player from your parcels")
    @ParcelRequire(owner = true)
    fun cmdUnban(sender: Player, player: OfflinePlayer): Any? {
        Validate.isTrue(data[sender].unban(player), "${player.name} is not currently banned from all your parcels")
        return "${player.name} is not banned from all your parcels anymore"
    }

}