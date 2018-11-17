package io.dico.parcels2.defaultimpl

import io.dico.parcels2.*
import io.dico.parcels2.blockvisitor.Schematic
import io.dico.parcels2.util.math.Region
import io.dico.parcels2.util.math.Vec3d
import io.dico.parcels2.util.math.Vec3i
import io.dico.parcels2.util.schedule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.WorldCreator
import org.bukkit.entity.Entity
import org.bukkit.util.Vector
import org.joda.time.DateTime

class ParcelProviderImpl(val plugin: ParcelsPlugin) : ParcelProvider {
    inline val options get() = plugin.options
    override val worlds: Map<String, ParcelWorld> get() = _worlds
    private val _worlds: MutableMap<String, ParcelWorld> = hashMapOf()
    private val _generators: MutableMap<String, ParcelGenerator> = hashMapOf()
    private var _worldsLoaded = false
    private var _dataIsLoaded = false

    // disabled while !_dataIsLoaded. getParcelById() will work though for data loading.
    override fun getWorld(name: String): ParcelWorld? = _worlds[name]?.takeIf { _dataIsLoaded }

    override fun getWorldById(id: ParcelWorldId): ParcelWorld? {
        if (id is ParcelWorld) return id
        return _worlds[id.name] ?: id.bukkitWorld?.let { getWorld(it) }
    }

    override fun getParcelById(id: ParcelId): Parcel? {
        if (id is Parcel) return id
        return getWorldById(id.worldId)?.container?.getParcelById(id.x, id.z)
    }

    override fun getWorldGenerator(worldName: String): ParcelGenerator? {
        return _worlds[worldName]?.generator
            ?: _generators[worldName]
            ?: options.worlds[worldName]?.generator?.newInstance(worldName)?.also { _generators[worldName] = it }
    }

    override fun loadWorlds() {
        if (_worldsLoaded) throw IllegalStateException()
        _worldsLoaded = true
        loadWorlds0()
    }

    private fun loadWorlds0() {
        if (Bukkit.getWorlds().isEmpty()) {
            plugin.schedule { loadWorlds0() }
            plugin.logger.warning("Scheduling to load worlds in the next tick because no bukkit worlds are loaded yet")
            return
        }

        val newlyCreatedWorlds = mutableListOf<ParcelWorld>()
        for ((worldName, worldOptions) in options.worlds.entries) {
            var parcelWorld = _worlds[worldName]
            if (parcelWorld != null) continue

            val generator: ParcelGenerator = getWorldGenerator(worldName)!!
            val worldExists = Bukkit.getWorld(worldName) != null
            val bukkitWorld =
                if (worldExists) Bukkit.getWorld(worldName)!!
                else {
                    logger.info("Creating world $worldName")
                    WorldCreator(worldName).generator(generator).createWorld()
                }

            parcelWorld =
                ParcelWorldImpl(plugin, bukkitWorld, generator, worldOptions.runtime, ::DefaultParcelContainer)

            if (!worldExists) {
                val time = DateTime.now()
                plugin.storage.setWorldCreationTime(parcelWorld.id, time)
                parcelWorld.creationTime = time
                newlyCreatedWorlds.add(parcelWorld)
            } else {
                GlobalScope.launch(context = Dispatchers.Unconfined) {
                    parcelWorld.creationTime = plugin.storage.getWorldCreationTime(parcelWorld.id).await() ?:
                        DateTime.now()
                }
            }

            _worlds[worldName] = parcelWorld
        }

        loadStoredData(newlyCreatedWorlds.toSet())
    }

    private fun loadStoredData(newlyCreatedWorlds: Collection<ParcelWorld> = emptyList()) {
        plugin.launch {
            val migration = plugin.options.migration
            if (migration.enabled) {
                migration.instance?.newInstance()?.apply {
                    logger.warn("Migrating database now...")
                    migrateTo(plugin.storage).join()
                    logger.warn("Migration completed")

                    if (migration.disableWhenComplete) {
                        migration.enabled = false
                        plugin.saveOptions()
                    }
                }
            }

            logger.info("Loading all parcel data...")

            val job1 = launch {
                val channel = plugin.storage.transmitAllParcelData()
                while (true) {
                    val (id, data) = channel.receiveOrNull() ?: break
                    val parcel = getParcelById(id) ?: continue
                    data?.let { parcel.copyData(it, callerIsDatabase = true) }
                }
            }

            val channel2 = plugin.storage.transmitAllGlobalPrivileges()
            while (true) {
                val (profile, data) = channel2.receiveOrNull() ?: break
                if (profile !is PrivilegeKey) {
                    logger.error("Received profile that is not a privilege key: ${profile.javaClass}, $profile")
                    continue
                }
                (plugin.globalPrivileges[profile] as PrivilegesHolder).copyPrivilegesFrom(data)
            }

            job1.join()

            logger.info("Loading data completed")
            _dataIsLoaded = true
        }
    }

    override fun acquireBlockVisitorPermit(parcelId: ParcelId, with: Permit): Boolean {
        val parcel = getParcelById(parcelId) as? ParcelImpl ?: return true
        return parcel.acquireBlockVisitorPermit(with)
    }

    override fun releaseBlockVisitorPermit(parcelId: ParcelId, with: Permit) {
        val parcel = getParcelById(parcelId) as? ParcelImpl ?: return
        parcel.releaseBlockVisitorPermit(with)
    }

    override fun trySubmitBlockVisitor(permit: Permit, vararg parcelIds: ParcelId, function: JobFunction): Job? {
        val withPermit = parcelIds.filter { acquireBlockVisitorPermit(it, permit) }
        if (withPermit.size != parcelIds.size) {
            withPermit.forEach { releaseBlockVisitorPermit(it, permit) }
            return null
        }

        val job = plugin.jobDispatcher.dispatch(function)

        plugin.launch {
            job.awaitCompletion()
            withPermit.forEach { releaseBlockVisitorPermit(it, permit) }
        }

        return job
    }

    override fun swapParcels(parcelId1: ParcelId, parcelId2: ParcelId): Job? {
        val world1 = getWorldById(parcelId1.worldId) ?: return null
        val world2 = getWorldById(parcelId2.worldId) ?: return null
        val blockManager1 = world1.blockManager
        val blockManager2 = world2.blockManager

        class CopyTarget(val world: World, val region: Region)
        class CopySource(val origin: Vec3i, val schematic: Schematic, val entities: Collection<Entity>)

        suspend fun JobScope.copy(source: CopySource, target: CopyTarget) {
            with(source.schematic) { paste(target.world, target.region.origin) }

            for (entity in source.entities) {
                entity.velocity = Vector(0, 0, 0)
                val location = entity.location
                location.world = target.world
                val coords = target.region.origin + (Vec3d(entity.location) - source.origin)
                coords.copyInto(location)
                entity.teleport(location)
            }
        }

        return trySubmitBlockVisitor(Permit(), parcelId1, parcelId2) {
            val temporaryParcel = world1.nextEmptyParcel()
                ?: world2.nextEmptyParcel()
                ?: return@trySubmitBlockVisitor

            var region1 = blockManager1.getRegion(parcelId1)
            var region2 = blockManager2.getRegion(parcelId2)

            val size = region1.size.clampMax(region2.size)
            if (size != region1.size) {
                region1 = region1.withSize(size)
                region2 = region2.withSize(size)
            }

            // Teleporting entities safely requires a different approach:
            // * Copy schematic1 into temporary location
            // * Teleport entities1 into temporary location
            // * Copy schematic2 into parcel1
            // * Teleport entities2 into parcel1
            // * Copy schematic1 into parcel2
            // * Teleport entities1 into parcel2
            // * Clear temporary location

            lateinit var source1: CopySource
            lateinit var source2: CopySource

            delegateWork(0.30) {
                val schematicOf1 = delegateWork(0.50) { Schematic().apply { load(blockManager1.world, region1) } }
                val schematicOf2 = delegateWork(0.50) { Schematic().apply { load(blockManager2.world, region2) } }

                source1 = CopySource(region1.origin, schematicOf1, blockManager1.getEntities(region1))
                source2 = CopySource(region2.origin, schematicOf2, blockManager2.getEntities(region2))
            }

            val target1 = CopyTarget(blockManager1.world, region1)
            val target2 = CopyTarget(blockManager2.world, region2)
            val targetTemp = CopyTarget(
                temporaryParcel.world.world,
                temporaryParcel.world.blockManager.getRegion(temporaryParcel.id)
            )

            delegateWork {
                delegateWork(1.0 / 3.0) { copy(source1, targetTemp) }
                delegateWork(1.0 / 3.0) { copy(source2, target1) }
                delegateWork(1.0 / 3.0) { copy(source1, target2) }
            }

            // Separate job. Whatever
            temporaryParcel.world.blockManager.clearParcel(temporaryParcel.id)
        }
    }

    /*
    fun loadWorlds(options: Options) {
        for ((worldName, worldOptions) in options.worlds.entries) {
            val world: ParcelWorld
            try {

                world = ParcelWorldImpl(
                    worldName,
                    worldOptions,
                    worldOptions.generator.newGenerator(this, worldName),
                    plugin.storage,
                    plugin.globalPrivileges,
                    ::DefaultParcelContainer)

            } catch (ex: Exception) {
                ex.printStackTrace()
                continue
            }

            _worlds[worldName] = world
        }

        plugin.functionHelper.schedule(10) {
            println("Parcels generating parcelProvider now")
            for ((name, world) in _worlds) {
                if (Bukkit.getWorld(name) == null) {
                    val bworld = WorldCreator(name).generator(world.generator).createWorld()
                    val spawn = world.generator.getFixedSpawnLocation(bworld, null)
                    bworld.setSpawnLocation(spawn.x.floor(), spawn.y.floor(), spawn.z.floor())
                }
            }

            val channel = plugin.storage.transmitAllParcelData()
            val job = plugin.functionHelper.launchLazilyOnMainThread {
                do {
                    val pair = channel.receiveOrNull() ?: break
                    val parcel = getParcelById(pair.first) ?: continue
                    pair.second?.let { parcel.copyDataIgnoringDatabase(it) }
                } while (true)
            }
            job.start()
        }

    }
    */
}