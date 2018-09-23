@file:Suppress("CanBePrimaryConstructorProperty", "UsePropertyAccessSyntax")

package io.dico.parcels2.defaultimpl

import io.dico.parcels2.*
import io.dico.parcels2.blockvisitor.WorktimeLimiter
import io.dico.parcels2.options.RuntimeWorldOptions
import io.dico.parcels2.storage.Storage
import kotlinx.coroutines.CoroutineScope
import org.bukkit.World
import org.joda.time.DateTime
import java.util.UUID

class ParcelWorldImpl(override val world: World,
                      override val generator: ParcelGenerator,
                      override var options: RuntimeWorldOptions,
                      override val storage: Storage,
                      override val globalAddedData: GlobalAddedDataManager,
                      containerFactory: ParcelContainerFactory,
                      coroutineScope: CoroutineScope,
                      worktimeLimiter: WorktimeLimiter)
    : ParcelWorld,
      ParcelWorldId,
      ParcelContainer, /* missing delegation */
      ParcelLocator /* missing delegation */ {

    override val id: ParcelWorldId get() = this
    override val uid: UUID? get() = world.uid

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
        val pair = generator.makeParcelLocatorAndBlockManager(id, container, coroutineScope, worktimeLimiter)
        locator = pair.first
        blockManager = pair.second

        enforceOptions()
    }

    fun enforceOptions() {
        if (options.dayTime) {
            world.setGameRuleValue("doDaylightCycle", "false")
            world.setTime(6000)
        }

        if (options.noWeather) {
            world.setStorm(false)
            world.setThundering(false)
            world.weatherDuration = Integer.MAX_VALUE
        }

        world.setGameRuleValue("doTileDrops", "${options.doTileDrops}")
    }

    // Updated by ParcelProviderImpl
    override var creationTime: DateTime? = null

    /*
    Interface delegation needs to be implemented manually because JetBrains has yet to fix it.
     */

    // ParcelLocator interface
    override fun getParcelAt(x: Int, z: Int): Parcel? {
        return locator.getParcelAt(x, z)
    }

    override fun getParcelIdAt(x: Int, z: Int): ParcelId? {
        return locator.getParcelIdAt(x, z)
    }

    // ParcelContainer interface
    override fun getParcelById(x: Int, z: Int): Parcel? {
        return container.getParcelById(x, z)
    }

    override fun nextEmptyParcel(): Parcel? {
        return container.nextEmptyParcel()
    }

    override fun toString() = toStringExt()
}
