package io.dico.parcels2.command

import io.dico.dicore.command.*
import io.dico.dicore.command.IContextFilter.Priority.PERMISSION
import io.dico.dicore.command.annotation.Cmd
import io.dico.dicore.command.annotation.PreprocessArgs
import io.dico.dicore.command.annotation.RequireParameters
import io.dico.dicore.command.parameter.ArgumentBuffer
import io.dico.parcels2.*
import io.dico.parcels2.blockvisitor.RegionTraverser
import io.dico.parcels2.util.ext.PERM_ADMIN_MANAGE
import io.dico.parcels2.util.ext.PERM_BAN_BYPASS
import io.dico.parcels2.util.ext.PERM_BUILD_ANYWHERE
import kotlinx.coroutines.launch
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.block.data.Directional
import org.bukkit.command.CommandSender
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
        val world = Bukkit.getWorld(worldName) ?: err("World $worldName is not loaded")
        sender.teleport(world.spawnLocation)
        return "Teleported you to $worldName spawn"
    }

    @Cmd("make_mess")
    @RequireParcelPrivilege(Privilege.OWNER)
    fun ParcelScope.cmdMakeMess(context: ExecutionContext) {
        Validate.isTrue(!parcel.hasBlockVisitors, "A process is already running in this parcel")

        val server = plugin.server
        val blockDatas = arrayOf(
            server.createBlockData(Material.BLUE_WOOL),
            server.createBlockData(Material.LIME_WOOL),
            server.createBlockData(Material.GLASS),
            server.createBlockData(Material.STONE_SLAB),
            server.createBlockData(Material.STONE),
            server.createBlockData(Material.QUARTZ_BLOCK),
            server.createBlockData(Material.BROWN_CONCRETE)
        )
        val random = Random()

        world.blockManager.doBlockOperation(parcel.id, traverser = RegionTraverser.upward) { block ->
            block.blockData = blockDatas[random.nextInt(7)]
        }.onProgressUpdate(1000, 1000) { progress, elapsedTime ->
            context.sendMessage(
                EMessageType.INFORMATIVE, "Mess progress: %.02f%%, %.2fs elapsed"
                    .format(progress * 100, elapsedTime / 1000.0)
            )
        }
    }

    @Cmd("directionality", aliases = ["dir"])
    fun cmdDirectionality(sender: Player, context: ExecutionContext, material: Material): Any? {
        val senderLoc = sender.location
        val block = senderLoc.add(senderLoc.direction.setY(0).normalize().multiply(2).toLocation(sender.world)).block

        val blockData = Bukkit.createBlockData(material)
        if (blockData is Directional) {
            blockData.facing = BlockFace.SOUTH
        }

        block.blockData = blockData
        return if (blockData is Directional) "The block is facing south" else "The block is not directional, however it implements " +
            blockData.javaClass.interfaces!!.contentToString()
    }

    @Cmd("jobs")
    fun cmdJobs(): Any? {
        val workers = plugin.jobDispatcher.jobs
        println(workers.map { it.job }.joinToString(separator = "\n"))
        return "Task count: ${workers.size}"
    }

    @Cmd("complete_jobs")
    fun cmdCompleteJobs(): Any? = cmdJobs().also {
        plugin.launch { plugin.jobDispatcher.completeAllTasks() }
    }

    @Cmd("message")
    @PreprocessArgs
    fun cmdMessage(sender: CommandSender, message: String): Any? {
        // testing @PreprocessArgs which merges "hello there" into a single argument
        sender.sendMessage(message)
        return null
    }

    @Cmd("hasperm")
    fun cmdHasperm(target: Player, permission: String): Any? {
        return target.hasPermission(permission).toString()
    }

    @Cmd("permissions")
    fun cmdPermissions(context: ExecutionContext, of: Player, vararg address: String): Any? {
        val target = context.address.dispatcherForTree.getDeepChild(ArgumentBuffer(address))
        Validate.isTrue(target.depth == address.size && target.hasCommand(), "Not found: /${address.joinToString(separator = " ")}")
        return getPermissionsOf(target).joinToString(separator = "\n") { "$it: ${of.hasPermission(it)}" }
    }

    @Cmd("privilege")
    @RequireParameters(1)
    suspend fun ParcelScope.cmdPrivilege(target: PlayerProfile, adminPerm: String?): Any? {
        val key = toPrivilegeKey(target)

        val perm = when (adminPerm) {
            "none" -> null
            "build" -> PERM_BUILD_ANYWHERE
            "manage", null -> PERM_ADMIN_MANAGE
            "enter" -> PERM_BAN_BYPASS
            else -> err("adminPerm should be build, manager or enter")
        }

        val privilege = if (perm == null) {
            parcel.getStoredPrivilege(key)
        } else {
            if (key is PlayerProfile.Star) err("* can't have permissions")
            parcel.getEffectivePrivilege(key.player!!, perm)
        }

        return privilege.toString()
    }

    private fun getPermissionsOf(address: ICommandAddress) = getPermissionsOf(address, emptyArray(), mutableListOf())

    private fun getPermissionsOf(address: ICommandAddress, path: Array<String>, result: MutableList<String>): List<String> {
        val command = address.command ?: return result

        var inherited = false
        for (filter in command.contextFilters) {
            when (filter) {
                is PermissionContextFilter -> {
                    if (path.isEmpty()) result.add(filter.permission)
                    else if (filter.isInheritable) result.add(filter.getInheritedPermission(path))
                }
                is InheritingContextFilter -> {
                    if (filter.priority == PERMISSION && address.hasParent() && !inherited) {
                        inherited = true
                        getPermissionsOf(address.parent, arrayOf(address.mainKey, *path), result)
                    }
                }
            }
        }

        return result
    }

}