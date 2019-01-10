package io.dico.parcels2.options

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.bukkit.Bukkit
import org.bukkit.block.data.BlockData

val optionsMapper = ObjectMapper(YAMLFactory()).apply {
    propertyNamingStrategy = PropertyNamingStrategy.KEBAB_CASE

    val kotlinModule = KotlinModule()

    with(kotlinModule) {
        /*
        setSerializerModifier(object : BeanSerializerModifier() {
            @Suppress("UNCHECKED_CAST")
            override fun modifySerializer(config: SerializationConfig?, beanDesc: BeanDescription, serializer: JsonSerializer<*>): JsonSerializer<*> {

                val newSerializer = if (GeneratorOptions::class.isSuperclassOf(beanDesc.beanClass.kotlin)) {
                    GeneratorOptionsSerializer(serializer as JsonSerializer<GeneratorOptions>)
                } else {
                    serializer
                }

                return super.modifySerializer(config, beanDesc, newSerializer)
            }
        })*/

        addSerializer(BlockDataSerializer())
        addDeserializer(BlockData::class.java, BlockDataDeserializer())

        GeneratorOptionsFactories.registerSerialization(this)
        StorageOptionsFactories.registerSerialization(this)
        MigrationOptionsFactories.registerSerialization(this)
    }

    registerModule(kotlinModule)
}

private class BlockDataSerializer : StdSerializer<BlockData>(BlockData::class.java) {

    override fun serialize(value: BlockData, gen: JsonGenerator, provider: SerializerProvider) {
        gen.writeString(value.asString)
    }

}

private class BlockDataDeserializer : StdDeserializer<BlockData>(BlockData::class.java) {

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): BlockData? {
        try {
            return Bukkit.createBlockData(p.valueAsString)
        } catch (ex: Exception) {
            throw RuntimeException("Exception occurred at ${p.currentLocation}", ex)
        }
    }

}
