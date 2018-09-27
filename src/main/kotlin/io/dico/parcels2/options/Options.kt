package io.dico.parcels2.options

import io.dico.parcels2.blockvisitor.TickJobtimeOptions
import org.bukkit.GameMode
import org.bukkit.Material
import java.io.Reader
import java.io.Writer
import java.util.EnumSet

class Options {
    var worlds: Map<String, WorldOptions> = hashMapOf()
        private set
    var storage: StorageOptions = StorageOptions()
    var tickJobtime: TickJobtimeOptions = TickJobtimeOptions(20, 1)
    var migration = MigrationOptionsHolder()

    fun addWorld(name: String,
                 generatorOptions: GeneratorOptions? = null,
                 worldOptions: RuntimeWorldOptions? = null) {
        val optionsHolder = WorldOptions(
            generatorOptions ?: GeneratorOptions(),
            worldOptions ?: RuntimeWorldOptions()
        )

        (worlds as MutableMap).put(name, optionsHolder)
    }

    fun writeTo(writer: Writer) = optionsMapper.writeValue(writer, this)

    fun mergeFrom(reader: Reader) = optionsMapper.readerForUpdating(this).readValue<Options>(reader)

    override fun toString(): String = optionsMapper.writeValueAsString(this)

}

class WorldOptions(val generator: GeneratorOptions,
                   var runtime: RuntimeWorldOptions = RuntimeWorldOptions())

class RuntimeWorldOptions(var gameMode: GameMode? = GameMode.CREATIVE,
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
                          var axisLimit: Int = 10)

class DataFileOptions(val location: String = "/flatfile-storage/")

class MigrationOptionsHolder {
    var enabled = false
    var disableWhenComplete = true
    var instance: MigrationOptions? = MigrationOptions()
}