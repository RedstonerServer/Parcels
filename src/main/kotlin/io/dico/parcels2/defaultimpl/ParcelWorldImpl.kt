@file:Suppress("CanBePrimaryConstructorProperty", "UsePropertyAccessSyntax")

package io.dico.parcels2.defaultimpl

import io.dico.parcels2.*
import io.dico.parcels2.options.RuntimeWorldOptions
import io.dico.parcels2.storage.Storage
import kotlinx.coroutines.CoroutineScope
import org.bukkit.GameRule
import org.bukkit.World
import org.joda.time.DateTime
import java.util.UUID

class ParcelWorldImpl(
    val plugin: ParcelsPlugin,
    override val world: World,
    override val generator: ParcelGenerator,
    override var options: RuntimeWorldOptions,
    containerFactory: ParcelContainerFactory
) : ParcelWorld, ParcelWorldId, ParcelContainer, ParcelLocator {
    override val id: ParcelWorldId get() = this
    override val uid: UUID? get() = world.uid

    override val storage get() = plugin.storage
    override val globalPrivileges get() = plugin.globalPrivileges

    init {
        if (generator.world != world) {
            throw IllegalArgumentException()
        }
    }

    override val name: String = world.name!!
    override val container: ParcelContainer = containerFactory(this)
    override val locator: ParcelLocator
    override val blockManager: ParcelBlockManager

    init {
        val (locator, blockManager) = generator.makeParcelLocatorAndBlockManager(plugin.parcelProvider, container, plugin, plugin.jobDispatcher)
        this.locator = locator
        this.blockManager = blockManager
        enforceOptions()
    }

    fun enforceOptions() {
        if (options.dayTime) {
            world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false)
            world.setTime(6000)
        }

        if (options.noWeather) {
            world.setStorm(false)
            world.setThundering(false)
            world.weatherDuration = Int.MAX_VALUE
        }

        world.setGameRule(GameRule.DO_TILE_DROPS, options.doTileDrops)
    }

    // Accessed by ParcelProviderImpl
    override var creationTime: DateTime? = null


    override fun getParcelAt(x: Int, z: Int): Parcel? = locator.getParcelAt(x, z)

    override fun getParcelIdAt(x: Int, z: Int): ParcelId? = locator.getParcelIdAt(x, z)

    override fun getParcelById(x: Int, z: Int): Parcel? = container.getParcelById(x, z)

    override fun getParcelById(id: ParcelId): Parcel? = container.getParcelById(id)

    override fun nextEmptyParcel(): Parcel? = container.nextEmptyParcel()

    override fun toString() = parcelWorldIdToString()
}
