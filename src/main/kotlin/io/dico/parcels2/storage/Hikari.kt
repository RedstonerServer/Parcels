package io.dico.parcels2.storage

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.dico.parcels2.DataConnectionOptions
import javax.sql.DataSource

fun getHikariDataSource(dialectName: String,
                        driver: String,
                        dco: DataConnectionOptions): DataSource = with(HikariConfig()) {

    val (address, port) = dco.splitAddressAndPort() ?: throw IllegalArgumentException("Invalid address: ${dco.address}")

    poolName = "redstonerplots"
    maximumPoolSize = dco.poolSize
    dataSourceClassName = driver
    username = dco.username
    password = dco.password
    connectionTimeout = 15000
    leakDetectionThreshold = 10000
    connectionTestQuery = "SELECT 1"

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
        addDataSourceProperty("url", "jdbc:h2:tcp://$address/~/${dco.database}")
    } else {
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
    }

    HikariDataSource(this)

}
