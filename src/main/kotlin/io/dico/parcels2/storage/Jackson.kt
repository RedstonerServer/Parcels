package io.dico.parcels2.storage

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.dico.parcels2.GeneratorFactories
import io.dico.parcels2.GeneratorOptions
import io.dico.parcels2.StorageOptions
import org.bukkit.Bukkit
import org.bukkit.block.data.BlockData
import kotlin.reflect.full.isSuperclassOf

val yamlObjectMapper = ObjectMapper(YAMLFactory()).apply {
    propertyNamingStrategy = PropertyNamingStrategy.KEBAB_CASE

    val kotlinModule = KotlinModule()

    with(kotlinModule) {
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
        })

        addSerializer(BlockDataSerializer())
        addDeserializer(BlockData::class.java, BlockDataDeserializer())

        addSerializer(StorageOptionsSerializer())
        addDeserializer(StorageOptions::class.java, StorageOptionsDeserializer())

        addDeserializer(GeneratorOptions::class.java, GeneratorOptionsDeserializer())
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

class StorageOptionsDeserializer : JsonDeserializer<StorageOptions>() {

    override fun deserialize(p: JsonParser?, ctxt: DeserializationContext?): StorageOptions {
        val node = p!!.readValueAsTree<JsonNode>()
        val dialect = node.get("dialect").asText()
        val optionsNode = node.get("options")
        val factory = StorageFactory.getFactory(dialect) ?: throw IllegalStateException("Unknown storage dialect: $dialect")
        val options = p.codec.treeToValue(optionsNode, factory.optionsClass.java)
        return StorageOptions(dialect, options)
    }

}

class StorageOptionsSerializer : StdSerializer<StorageOptions>(StorageOptions::class.java) {

    override fun serialize(value: StorageOptions?, gen: JsonGenerator?, serializers: SerializerProvider?) {
        with(gen!!) {
            writeStartObject()
            writeStringField("dialect", value!!.dialect)
            writeFieldName("options")
            writeObject(value.options)
            writeEndObject()
        }
    }

}

class GeneratorOptionsDeserializer : JsonDeserializer<GeneratorOptions>() {

    override fun deserialize(parser: JsonParser?, ctx: DeserializationContext?): GeneratorOptions? {
        val node = parser!!.readValueAsTree<JsonNode>()
        val name = node.get("name").asText()
        val optionsNode = node.get("options")
        val factory = GeneratorFactories.getFactory(name) ?: throw IllegalStateException("Unknown generator: $name")

        return parser.codec.treeToValue(optionsNode, factory.optionsClass.java)
    }

}

class GeneratorOptionsSerializer(private val defaultSerializer: JsonSerializer<GeneratorOptions>) : JsonSerializer<GeneratorOptions>() {

    override fun serialize(input: GeneratorOptions?, generator: JsonGenerator?, provider: SerializerProvider?) {
        with(generator!!) {
            writeStartObject()
            writeStringField("name", input!!.generatorFactory().name)
            writeFieldName("options")
            defaultSerializer.serialize(input, generator, provider)
            writeEndObject()
        }
    }

}