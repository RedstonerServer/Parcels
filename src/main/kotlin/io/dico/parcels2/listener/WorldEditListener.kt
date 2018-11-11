package io.dico.parcels2.listener

import com.sk89q.worldedit.EditSession.Stage.BEFORE_REORDER
import com.sk89q.worldedit.Vector
import com.sk89q.worldedit.Vector2D
import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.bukkit.WorldEditPlugin
import com.sk89q.worldedit.event.extent.EditSessionEvent
import com.sk89q.worldedit.extent.AbstractDelegateExtent
import com.sk89q.worldedit.extent.Extent
import com.sk89q.worldedit.util.eventbus.EventHandler.Priority.VERY_EARLY
import com.sk89q.worldedit.util.eventbus.Subscribe
import com.sk89q.worldedit.world.biome.BaseBiome
import com.sk89q.worldedit.world.block.BlockStateHolder
import io.dico.parcels2.ParcelWorld
import io.dico.parcels2.ParcelsPlugin
import io.dico.parcels2.canBuildFast
import io.dico.parcels2.util.ext.hasPermBuildAnywhere
import io.dico.parcels2.util.ext.sendParcelMessage
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin

class WorldEditListener(val parcels: ParcelsPlugin, val worldEdit: WorldEdit) {

    @Subscribe(priority = VERY_EARLY)
    fun onEditSession(event: EditSessionEvent) {
        val worldName = event.world?.name ?: return
        val world = parcels.parcelProvider.getWorld(worldName) ?: return
        if (event.stage == BEFORE_REORDER) return

        val actor = event.actor
        if (actor == null || !actor.isPlayer) return

        val player = parcels.server.getPlayer(actor.uniqueId)
        if (player.hasPermBuildAnywhere) return

        event.extent = ParcelsExtent(event.extent, world, player)
    }

    private class ParcelsExtent(extent: Extent,
                                val world: ParcelWorld,
                                val player: Player) : AbstractDelegateExtent(extent) {
        private var messageSent = false

        private fun canBuild(x: Int, z: Int): Boolean {
            world.getParcelAt(x, z)?.let { parcel ->
                if (parcel.canBuildFast(player)) {
                    return true
                }
            }

            if (!messageSent) {
                messageSent = true
                player.sendParcelMessage(except = true, message = "You can't use WorldEdit there")
            }

            return false
        }

        override fun setBlock(location: Vector, block: BlockStateHolder<*>): Boolean {
            return canBuild(location.blockX, location.blockZ) && super.setBlock(location, block)
        }

        override fun setBiome(coord: Vector2D, biome: BaseBiome): Boolean {
            return canBuild(coord.blockX, coord.blockZ) && super.setBiome(coord, biome)
        }

    }

    companion object {
        fun register(parcels: ParcelsPlugin, worldEditPlugin: Plugin) {
            if (worldEditPlugin !is WorldEditPlugin) return
            val worldEdit = worldEditPlugin.worldEdit
            val listener = WorldEditListener(parcels, worldEdit)
            worldEdit.eventBus.register(listener)
        }
    }

}