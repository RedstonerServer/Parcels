package io.dico.parcels2


import com.fasterxml.jackson.annotation.JsonIgnore
import io.dico.parcels2.blockvisitor.TickWorktimeOptions
import io.dico.parcels2.storage.Storage
import io.dico.parcels2.storage.StorageFactory
import io.dico.parcels2.storage.yamlObjectMapper
import org.bukkit.Bukkit.createBlockData
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.block.Biome
import org.bukkit.block.data.BlockData
import java.io.Reader
import java.io.Writer
import java.util.*

class Options {
    var worlds: Map<String, WorldOptions> = HashMap()
        private set
    var storage: StorageOptions = StorageOptions("postgresql", DataConnectionOptions())
    var tickWorktime: TickWorktimeOptions = TickWorktimeOptions(30, 1)

    fun addWorld(name: String, options: WorldOptions) = (worlds as MutableMap).put(name, options)

    fun writeTo(writer: Writer) = yamlObjectMapper.writeValue(writer, this)

    fun mergeFrom(reader: Reader) = yamlObjectMapper.readerForUpdating(this).readValue<Options>(reader)

    override fun toString(): String = yamlObjectMapper.writeValueAsString(this)

}

data class WorldOptions(var gameMode: GameMode? = GameMode.CREATIVE,
                        var dayTime: Boolean = true,
                        var noWeather: Boolean = true,
                        var dropEntityItems: Boolean = true,
                        var doTileDrops: Boolean = false,
                        var disableExplosions: Boolean = true,
                        var blockPortalCreation: Boolean = true,
                        var blockMobSpawning: Boolean = true,
                        var blockedItems: Set<Material> = EnumSet.of(Material.FLINT_AND_STEEL, Material.SNOWBALL),
                        var axisLimit: Int = 10,
                        var generator: GeneratorOptions = DefaultGeneratorOptions()) {

}

abstract class GeneratorOptions {

    abstract fun generatorFactory(): GeneratorFactory

    fun getGenerator(worlds: Worlds, worldName: String) = generatorFactory().newParcelGenerator(worlds, worldName, this)

}

data class DefaultGeneratorOptions(var defaultBiome: Biome = Biome.JUNGLE,
                                   var wallType: BlockData = createBlockData(Material.STONE_SLAB),
                                   var floorType: BlockData = createBlockData(Material.QUARTZ_BLOCK),
                                   var fillType: BlockData = createBlockData(Material.QUARTZ_BLOCK),
                                   var pathMainType: BlockData = createBlockData(Material.SANDSTONE),
                                   var pathAltType: BlockData = createBlockData(Material.REDSTONE_BLOCK),
                                   var parcelSize: Int = 101,
                                   var pathSize: Int = 9,
                                   var floorHeight: Int = 64,
                                   var offsetX: Int = 0,
                                   var offsetZ: Int = 0) : GeneratorOptions() {

    override fun generatorFactory(): GeneratorFactory = DefaultParcelGenerator.Factory

}

class StorageOptions(val dialect: String,
                     val options: Any) {

    @get:JsonIgnore
    val factory = StorageFactory.getFactory(dialect)
        ?: throw IllegalArgumentException("Invalid storage dialect: $dialect")

    fun newStorageInstance(): Storage = factory.newStorageInstance(dialect, options)

}

data class DataConnectionOptions(val address: String = "localhost",
                                 val database: String = "parcels",
                                 val username: String = "root",
                                 val password: String = "",
                                 val poolSize: Int = 4) {

    fun splitAddressAndPort(defaultPort: Int = 3306): Pair<String, Int>? {
        val idx = address.indexOf(":").takeUnless { it == -1 } ?: return Pair(address, defaultPort)

        val addressName = address.substring(0, idx).takeUnless { it.isBlank() } ?: return null.also {
            logger.error("(Invalidly) blank address in data storage options")
        }

        val port = address.substring(idx + 1).toIntOrNull() ?: return null.also {
            logger.error("Invalid port number in data storage options: $it, using $defaultPort as default")
        }

        return Pair(addressName, port)
    }

}

data class DataFileOptions(val location: String = "/flatfile-storage/")