@file:Suppress("CanBePrimaryConstructorProperty", "UsePropertyAccessSyntax")

package io.dico.parcels2.defaultimpl

import io.dico.parcels2.*
import io.dico.parcels2.blockvisitor.WorktimeLimiter
import io.dico.parcels2.options.RuntimeWorldOptions
import io.dico.parcels2.storage.Storage
import org.bukkit.World
import java.util.UUID

class ParcelWorldImpl private
constructor(override val world: World,
            override val generator: ParcelGenerator,
            override var options: RuntimeWorldOptions,
            override val storage: Storage,
            override val globalAddedData: GlobalAddedDataManager,
            containerFactory: ParcelContainerFactory,
            blockManager: ParcelBlockManager)
    : ParcelWorld,
      ParcelWorldId,
      ParcelContainer, // missing delegation
      ParcelLocator, // missing delegation
      ParcelBlockManager by blockManager {
    override val id: ParcelWorldId get() = this
    override val uid: UUID? get() = world.uid

    init {
        if (generator.world != world) {
            throw IllegalArgumentException()
        }
    }

    override val name: String = world.name!!
    override val container: ParcelContainer = containerFactory(this)
    override val locator: ParcelLocator = generator.makeParcelLocator(container)
    override val blockManager: ParcelBlockManager = blockManager

    init {
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

    /*
    Interface delegation needs to be implemented manually because JetBrains has yet to fix it.
     */

    companion object {
        // Use this to be able to delegate blockManager and assign it to a property too, at least.
        operator fun invoke(world: World,
                            generator: ParcelGenerator,
                            options: RuntimeWorldOptions,
                            storage: Storage,
                            globalAddedData: GlobalAddedDataManager,
                            containerFactory: ParcelContainerFactory,
                            worktimeLimiter: WorktimeLimiter): ParcelWorldImpl {
            val blockManager = generator.makeParcelBlockManager(worktimeLimiter)
            return ParcelWorldImpl(world, generator, options, storage, globalAddedData, containerFactory, blockManager)
        }
    }

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


}
