package io.dico.parcels2.storage

import com.zaxxer.hikari.HikariConfig
import io.dico.parcels2.options.DataConnectionOptions

fun getHikariConfig(dialectName: String,
                    dco: DataConnectionOptions): HikariConfig = HikariConfig().apply {

    val (address, port) = dco.splitAddressAndPort() ?: throw IllegalArgumentException("Invalid address: ${dco.address}")

    when (dialectName) {
        "postgresql" -> run {
            dataSourceClassName = "org.postgresql.ds.PGSimpleDataSource"
            dataSourceProperties["serverName"] = address
            dataSourceProperties["portNumber"] = port.toString()
            dataSourceProperties["databaseName"] = dco.database
        }

        "mariadb" -> run {
            dataSourceClassName = "org.mariadb.jdbc.MariaDbDataSource"
            dataSourceProperties["serverName"] = address
            dataSourceProperties["port"] = port.toString()
            dataSourceProperties["databaseName"] = dco.database
            dataSourceProperties["properties"] = "useUnicode=true;characterEncoding=utf8"
        }

        else -> throw IllegalArgumentException("Unsupported dialect: $dialectName")
    }

    poolName = "parcels"
    maximumPoolSize = dco.poolSize
    username = dco.username
    password = dco.password
    connectionTimeout = 15000
    leakDetectionThreshold = 30000
    connectionTestQuery = "SELECT 1"


    /*

    addDataSourceProperty("serverName", address)
    addDataSourceProperty("port", port.toString())
    addDataSourceProperty("databaseName", dco.database)

    // copied from github.com/lucko/LuckPerms
    if (dialectName.toLowerCase() == "mariadb") {
        addDataSourceProperty("properties", "useUnicode=true;characterEncoding=utf8")
    } else if (dialectName.toLowerCase() == "h2") {
        dataSourceProperties.remove("serverName")
        dataSourceProperties.remove("port")
        dataSourceProperties.remove("databaseName")
        addDataSourceProperty("url", "jdbc:h2:${if (address.isBlank()) "" else "tcp://$address/"}~/${dco.database}")
    } else if (dialectName.toLowerCase() == "mysql") {
        // doesn't exist on the MariaDB driver
        addDataSourceProperty("cachePrepStmts", "true")
        addDataSourceProperty("alwaysSendSetIsolation", "false")
        addDataSourceProperty("cacheServerConfiguration", "true")
        addDataSourceProperty("elideSetAutoCommits", "true")
        addDataSourceProperty("useLocalSessionState", "true")

        // already set as default on mariadb
        addDataSourceProperty("useServerPrepStmts", "true")
        addDataSourceProperty("prepStmtCacheSize", "250")
        addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
        addDataSourceProperty("cacheCallableStmts", "true")

        // make sure unicode characters can be used.
        addDataSourceProperty("characterEncoding", "utf8")
        addDataSourceProperty("useUnicode", "true")
    } else {

    }*/
}
