package io.dico.parcels2.options

import io.dico.parcels2.ParcelGenerator
import io.dico.parcels2.defaultimpl.DefaultParcelGenerator
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.block.Biome
import org.bukkit.block.data.BlockData
import kotlin.reflect.KClass

object GeneratorOptionsFactories : PolymorphicOptionsFactories<ParcelGenerator>("name", GeneratorOptions::class, DefaultGeneratorOptionsFactory())

class GeneratorOptions (name: String = "default", options: Any = DefaultGeneratorOptions()) : PolymorphicOptions<ParcelGenerator>(name, options, GeneratorOptionsFactories) {
    fun newInstance(worldName: String) = factory.newInstance(key, options, worldName)
}

private class DefaultGeneratorOptionsFactory : PolymorphicOptionsFactory<ParcelGenerator> {
    override val supportedKeys: List<String> = listOf("default")
    override val optionsClass: KClass<out Any> get() = DefaultGeneratorOptions::class

    override fun newInstance(key: String, options: Any, vararg extra: Any?): ParcelGenerator {
        return DefaultParcelGenerator(extra.first() as String, options as DefaultGeneratorOptions)
    }
}

class DefaultGeneratorOptions(val defaultBiome: Biome = Biome.JUNGLE,
                              val wallType: BlockData = Bukkit.createBlockData(Material.STONE_SLAB),
                              val floorType: BlockData = Bukkit.createBlockData(Material.QUARTZ_BLOCK),
                              val fillType: BlockData = Bukkit.createBlockData(Material.QUARTZ_BLOCK),
                              val pathMainType: BlockData = Bukkit.createBlockData(Material.SANDSTONE),
                              val pathAltType: BlockData = Bukkit.createBlockData(Material.REDSTONE_BLOCK),
                              val parcelSize: Int = 101,
                              val pathSize: Int = 9,
                              val floorHeight: Int = 64,
                              val offsetX: Int = 0,
                              val offsetZ: Int = 0)