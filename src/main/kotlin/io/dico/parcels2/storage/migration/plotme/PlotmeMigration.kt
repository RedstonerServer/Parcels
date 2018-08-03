@file:Suppress("RedundantSuspendModifier", "DEPRECATION")

package io.dico.parcels2.storage.migration.plotme

import com.zaxxer.hikari.HikariDataSource
import io.dico.parcels2.*
import io.dico.parcels2.options.PlotmeMigrationOptions
import io.dico.parcels2.storage.Storage
import io.dico.parcels2.storage.migration.Migration
import io.dico.parcels2.util.Vec2i
import io.dico.parcels2.util.isValid
import io.dico.parcels2.util.toUUID
import io.dico.parcels2.util.uuid
import kotlinx.coroutines.experimental.*
import org.bukkit.Bukkit
import org.jetbrains.exposed.sql.*
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream
import java.sql.Blob
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.sql.DataSource
import kotlin.coroutines.experimental.coroutineContext

class PlotmeMigration(val options: PlotmeMigrationOptions) : Migration {
    private var dataSource: DataSource? = null
    private var database: Database? = null
    private var isShutdown: Boolean = false
    private val mlogger = LoggerFactory.getLogger("PlotMe Migrator")

    private fun <T> transaction(statement: Transaction.() -> T) = org.jetbrains.exposed.sql.transactions.transaction(database!!, statement)

    override fun migrateTo(storage: Storage): Job {
        return launch(context = storage.asyncDispatcher) {
            init()
            transaction { launch(context = Unconfined, start = CoroutineStart.UNDISPATCHED) { doWork(storage) } }
            shutdown()
        }
    }

    suspend fun init() {
        if (isShutdown) throw IllegalStateException()
        dataSource = options.storage.getDataSourceFactory()!!()
        database = Database.connect(dataSource!!)
    }

    suspend fun shutdown() {
        if (isShutdown) throw IllegalStateException()
        dataSource?.let {
            (it as? HikariDataSource)?.close()
        }
        database = null
        isShutdown = true
    }

    private val parcelsCache = hashMapOf<String, MutableMap<Vec2i, ParcelData>>()

    private fun getMap(worldName: String): MutableMap<Vec2i, ParcelData>? {
        val mapped = options.worldsFromTo[worldName] ?: return null
        return parcelsCache.computeIfAbsent(mapped) { mutableMapOf() }
    }

    private fun getData(worldName: String, position: Vec2i): ParcelData? {
        return getMap(worldName)?.computeIfAbsent(position) { ParcelDataHolder(addedMap = ConcurrentHashMap()) }
    }

    suspend fun doWork(target: Storage): Unit {
        if (!PlotmePlotsT.exists()) {
            mlogger.warn("Plotme tables don't appear to exist. Exiting.")
            return
        }

        parcelsCache.clear()

        iterPlotmeTable(PlotmePlotsT) { data, row ->
            // in practice, owner_uuid is not null for any plot currently. It will convert well.
            data.owner = ParcelOwner(row[owner_uuid]?.toUUID(), row[owner_name])
        }

        launch(context = target.asyncDispatcher) {
            iterPlotmeTable(PlotmeAllowedT) { data, row ->
                val uuid = row[player_uuid]?.toUUID()
                    ?: Bukkit.getOfflinePlayer(row[player_name]).takeIf { it.isValid }?.uuid
                    ?: return@iterPlotmeTable

                data.setAddedStatus(uuid, AddedStatus.ALLOWED)
            }
        }

        launch(context = target.asyncDispatcher) {
            iterPlotmeTable(PlotmeDeniedT) { data, row ->
                val uuid = row[player_uuid]?.toUUID()
                    ?: Bukkit.getOfflinePlayer(row[player_name]).takeIf { it.isValid }?.uuid
                    ?: return@iterPlotmeTable

                data.setAddedStatus(uuid, AddedStatus.BANNED)
            }
        }

        println(coroutineContext[Job]!!.children)
        coroutineContext[Job]!!.joinChildren()

        for ((worldName, map) in parcelsCache) {
            val world = ParcelWorldId(worldName)
            for ((pos, data) in map) {
                val parcel = ParcelId(world, pos)
                target.setParcelData(parcel, data)
            }
        }

    }

    private fun Blob.toUUID(): UUID {
        val out = ByteArrayOutputStream(16)
        binaryStream.copyTo(out, bufferSize = 16)
        return out.toByteArray().toUUID()
    }

    private inline fun <T : PlotmeTable> iterPlotmeTable(table: T, block: T.(ParcelData, ResultRow) -> Unit) {
        table.selectAll().forEach { row ->
            val data = getData(row[table.world_name], Vec2i(row[table.px], row[table.pz])) ?: return@forEach
            table.block(data, row)
        }
    }

}