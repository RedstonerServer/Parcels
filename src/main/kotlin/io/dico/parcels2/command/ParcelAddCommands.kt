package io.dico.parcels2.command

import io.dico.dicore.command.annotation.Cmd
import io.dico.dicore.command.annotation.Desc
import io.dico.parcels2.ParcelsPlugin
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player

class ParcelAddCommands(plugin: ParcelsPlugin) : AbstractParcelCommands(plugin) {

    @Cmd("allow", aliases = ["add", "permit"])
    @Desc("Allows a player to build on this parcel",
        shortVersion = "allows a player to build on this parcel")
    @ParcelRequire(owner = true)
    fun ParcelScope.cmdAllow(sender: Player, player: OfflinePlayer): Any? {
        TODO()
    }

    @Cmd("disallow", aliases = ["remove", "forbid"])
    @Desc("Disallows a player to build on this parcel,",
        "they won't be allowed to anymore",
        shortVersion = "disallows a player to build on this parcel")
    @ParcelRequire(owner = true)
    fun ParcelScope.cmdDisallow(sender: Player, player: OfflinePlayer): Any? {
        TODO()
    }

    @Cmd("ban", aliases = ["deny"])
    @Desc("Bans a player from this parcel,",
        "making them unable to enter",
        shortVersion = "bans a player from this parcel")
    @ParcelRequire(owner = true)
    fun ParcelScope.cmdBan(sender: Player, player: OfflinePlayer): Any? {
        TODO()
    }

    @Cmd("unban", aliases = ["undeny"])
    @Desc("Unbans a player from this parcel,",
        "they will be able to enter it again",
        shortVersion = "unbans a player from this parcel")
    @ParcelRequire(owner = true)
    fun ParcelScope.cmdUnban(sender: Player, player: OfflinePlayer): Any? {
        TODO()
    }

}