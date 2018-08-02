package io.dico.parcels2.command

import io.dico.dicore.command.CommandException
import io.dico.dicore.command.EMessageType
import io.dico.dicore.command.ExecutionContext
import io.dico.dicore.command.annotation.Cmd
import io.dico.parcels2.ParcelsPlugin
import io.dico.parcels2.blockvisitor.RegionTraversal
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import java.util.Random

class CommandsDebug(plugin: ParcelsPlugin) : AbstractParcelCommands(plugin) {

    @Cmd("reloadoptions")
    fun reloadOptions() {
        plugin.loadOptions()
    }

    @Cmd("tpworld")
    fun tpWorld(sender: Player, worldName: String): String {
        if (worldName == "list") {
            return Bukkit.getWorlds().joinToString("\n- ", "- ", "")
        }
        val world = Bukkit.getWorld(worldName) ?: throw CommandException("World $worldName is not loaded")
        sender.teleport(world.spawnLocation)
        return "Teleported you to $worldName spawn"
    }

    @Cmd("make_mess")
    @ParcelRequire(owner = true)
    fun ParcelScope.cmdMakeMess(context: ExecutionContext) {
        val server = plugin.server
        val blockDatas = arrayOf(
            server.createBlockData(Material.STICKY_PISTON),
            server.createBlockData(Material.GLASS),
            server.createBlockData(Material.STONE_SLAB),
            server.createBlockData(Material.QUARTZ_BLOCK)
        )
        val random = Random()

        world.doBlockOperation(parcel.id, direction = RegionTraversal.UPWARD) { block ->
            block.blockData = blockDatas[random.nextInt(4)]
        }.onProgressUpdate(1000, 1000) { progress, elapsedTime ->
            context.sendMessage(EMessageType.INFORMATIVE, "Mess progress: %.02f%%, %.2fs elapsed"
                .format(progress * 100, elapsedTime / 1000.0))
        }
    }

}