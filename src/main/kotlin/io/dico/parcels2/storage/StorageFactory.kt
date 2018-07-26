package io.dico.parcels2.storage

import io.dico.parcels2.DataConnectionOptions
import kotlin.reflect.KClass

interface StorageFactory {
    companion object StorageFactories {
        private val map: MutableMap<String, StorageFactory> = HashMap()

        fun registerFactory(dialect: String, generator: StorageFactory): Boolean = map.putIfAbsent(dialect.toLowerCase(), generator) == null

        fun getFactory(dialect: String): StorageFactory? = map[dialect.toLowerCase()]

        init {
            // have to write the code like this in kotlin.
            // This code is absolutely disgusting
            ConnectionStorageFactory().register(this)
        }
    }

    val optionsClass: KClass<out Any>

    fun newStorageInstance(dialect: String, options: Any): Storage

}

class ConnectionStorageFactory : StorageFactory {
    override val optionsClass = DataConnectionOptions::class

    private val types: Map<String, String> = mutableMapOf(
        "mysql" to "com.mysql.jdbc.jdbc2.optional.MysqlDataSource",
        "h2" to "org.h2.jdbcx.JdbcDataSource"
    )

    fun register(companion: StorageFactory.StorageFactories) {
        types.keys.forEach {
            companion.registerFactory(it, this)
        }
    }

    override fun newStorageInstance(dialect: String, options: Any): Storage {
        val driverClass = types[dialect.toLowerCase()] ?: throw IllegalArgumentException("Storage dialect $dialect is not supported")
        return StorageWithCoroutineBacking(ExposedBacking(getHikariDataSource(dialect, driverClass, options as DataConnectionOptions)))
    }

}