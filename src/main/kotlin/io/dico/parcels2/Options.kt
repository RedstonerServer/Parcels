package io.dico.parcels2

import com.fasterxml.jackson.annotation.JsonIgnore
import io.dico.parcels2.blockvisitor.TickWorktimeOptions
import io.dico.parcels2.defaultimpl.DefaultGeneratorOptions
import io.dico.parcels2.storage.Storage
import io.dico.parcels2.storage.StorageFactory
import io.dico.parcels2.storage.yamlObjectMapper
import org.bukkit.GameMode
import org.bukkit.Material
import java.io.Reader
import java.io.Writer
import java.util.EnumSet

class Options {
    var worlds: Map<String, WorldOptionsHolder> = hashMapOf()
        private set
    var storage: StorageOptions = StorageOptions("postgresql", DataConnectionOptions())
    var tickWorktime: TickWorktimeOptions = TickWorktimeOptions(20, 1)

    fun addWorld(name: String,
                 generatorOptions: GeneratorOptions? = null,
                 worldOptions: WorldOptions? = null) {
        val optionsHolder = WorldOptionsHolder(
            generatorOptions ?: DefaultGeneratorOptions(),
            worldOptions ?: WorldOptions()
        )

        (worlds as MutableMap).put(name, optionsHolder)
    }

    fun writeTo(writer: Writer) = yamlObjectMapper.writeValue(writer, this)

    fun mergeFrom(reader: Reader) = yamlObjectMapper.readerForUpdating(this).readValue<Options>(reader)

    override fun toString(): String = yamlObjectMapper.writeValueAsString(this)

}

class WorldOptionsHolder(var generator: GeneratorOptions = DefaultGeneratorOptions(),
                         var runtime: WorldOptions = WorldOptions())

data class WorldOptions(var gameMode: GameMode? = GameMode.CREATIVE,
                        var dayTime: Boolean = true,
                        var noWeather: Boolean = true,
                        var preventWeatherBlockChanges: Boolean = true,
                        var preventBlockSpread: Boolean = true, // TODO
                        var dropEntityItems: Boolean = true,
                        var doTileDrops: Boolean = false,
                        var disableExplosions: Boolean = true,
                        var blockPortalCreation: Boolean = true,
                        var blockMobSpawning: Boolean = true,
                        var blockedItems: Set<Material> = EnumSet.of(Material.FLINT_AND_STEEL, Material.SNOWBALL),
                        var axisLimit: Int = 10) {

}

abstract class GeneratorOptions {

    abstract fun generatorFactory(): GeneratorFactory

    fun newGenerator(worldName: String) = generatorFactory().newParcelGenerator(worldName, this)

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

class MigrationOptions() {


}
