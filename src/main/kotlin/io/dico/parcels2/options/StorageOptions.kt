package io.dico.parcels2.options

import com.zaxxer.hikari.HikariDataSource
import io.dico.parcels2.logger
import io.dico.parcels2.storage.Storage
import io.dico.parcels2.storage.StorageWithCoroutineBacking
import io.dico.parcels2.storage.exposed.ExposedBacking
import io.dico.parcels2.storage.getHikariConfig

object StorageOptionsFactories : PolymorphicOptionsFactories<Storage>("dialect", StorageOptions::class, ConnectionStorageFactory())

class StorageOptions(dialect: String, options: Any) : SimplePolymorphicOptions<Storage>(dialect, options, StorageOptionsFactories)

private class ConnectionStorageFactory : PolymorphicOptionsFactory<Storage> {
    override val optionsClass = DataConnectionOptions::class
    override val supportedKeys: List<String> = listOf("postgresql", "mariadb")

    override fun newInstance(key: String, options: Any, vararg extra: Any?): Storage {
        val hikariConfig = getHikariConfig(key, options as DataConnectionOptions)
        val dataSourceFactory = suspend { HikariDataSource(hikariConfig) }
        return StorageWithCoroutineBacking(ExposedBacking(dataSourceFactory))
    }
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