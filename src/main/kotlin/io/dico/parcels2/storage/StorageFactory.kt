package io.dico.parcels2.storage

import com.zaxxer.hikari.HikariDataSource
import io.dico.parcels2.DataConnectionOptions
import io.dico.parcels2.storage.exposed.ExposedBacking
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
    private val types: List<String> = listOf("postgresql", "mariadb")

    fun register(companion: StorageFactory.StorageFactories) {
        types.forEach { companion.registerFactory(it, this) }
    }

    override fun newStorageInstance(dialect: String, options: Any): Storage {
        val hikariConfig = getHikariConfig(dialect, options as DataConnectionOptions)
        val dataSourceFactory = suspend { HikariDataSource(hikariConfig) }
        return StorageWithCoroutineBacking(ExposedBacking(dataSourceFactory))
    }

}