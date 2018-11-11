package io.dico.parcels2.options

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import io.dico.parcels2.logger
import kotlin.reflect.KClass

abstract class PolymorphicOptions<T : Any>(val key: String,
                                           val options: Any,
                                           factories: PolymorphicOptionsFactories<T>) {
    val factory = factories.getFactory(key)!!
}

abstract class SimplePolymorphicOptions<T : Any>(key: String, options: Any, factories: PolymorphicOptionsFactories<T>)
    : PolymorphicOptions<T>(key, options, factories) {
    fun newInstance(): T = factory.newInstance(key, options)
}

interface PolymorphicOptionsFactory<T : Any> {
    val supportedKeys: List<String>
    val optionsClass: KClass<out Any>
    fun newInstance(key: String, options: Any, vararg extra: Any?): T
}

@Suppress("UNCHECKED_CAST")
abstract class PolymorphicOptionsFactories<T : Any>(val serializeKeyAs: String,
                                                    rootClass: KClass<out PolymorphicOptions<T>>,
                                                    vararg defaultFactories: PolymorphicOptionsFactory<T>) {
    val rootClass = rootClass as KClass<PolymorphicOptions<T>>
    private val map: MutableMap<String, PolymorphicOptionsFactory<T>> = linkedMapOf()
    val availableKeys: Collection<String> get() = map.keys

    fun registerFactory(factory: PolymorphicOptionsFactory<T>) = factory.supportedKeys.forEach { map.putIfAbsent(it.toLowerCase(), factory) }

    fun getFactory(key: String): PolymorphicOptionsFactory<T>? = map[key.toLowerCase()]

    fun registerSerialization(module: SimpleModule) {
        module.addSerializer(PolymorphicOptionsSerializer(this))
        module.addDeserializer(rootClass.java, PolymorphicOptionsDeserializer(this))
    }

    init {
        defaultFactories.forEach { registerFactory(it) }
    }
}


private class PolymorphicOptionsDeserializer<T : Any>(val factories: PolymorphicOptionsFactories<T>) : JsonDeserializer<PolymorphicOptions<T>>() {

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext?): PolymorphicOptions<T> {
        val node = p.readValueAsTree<JsonNode>()
        val key = node.get(factories.serializeKeyAs).asText()
        val factory = getFactory(key)
        val optionsNode = node.get("options")
        val options = p.codec.treeToValue(optionsNode, factory.optionsClass.java)
        return factories.rootClass.constructors.first().call(key, options)
    }

    private fun getFactory(key: String): PolymorphicOptionsFactory<T> {
        factories.getFactory(key)?.let { return it }

        logger.warn("Unknown ${factories.rootClass.simpleName} ${factories.serializeKeyAs}: $key. " +
            "\nAvailable options: ${factories.availableKeys}")

        val default = factories.getFactory(factories.availableKeys.first())
            ?: throw IllegalStateException("No default ${factories.rootClass.simpleName} factory registered.")
        return default
    }

}

private class PolymorphicOptionsSerializer<T : Any>(val factories: PolymorphicOptionsFactories<T>) : StdSerializer<PolymorphicOptions<T>>(factories.rootClass.java) {

    override fun serialize(value: PolymorphicOptions<T>, gen: JsonGenerator, sp: SerializerProvider?) {
        with(gen) {
            writeStartObject()
            writeStringField(factories.serializeKeyAs, value.key)
            writeFieldName("options")
            writeObject(value.options)
            writeEndObject()
        }
    }
}