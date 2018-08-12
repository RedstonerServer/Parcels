@file:Suppress("RedundantSuspendModifier", "DEPRECATION")

package io.dico.parcels2.storage.migration.plotme

import com.zaxxer.hikari.HikariDataSource
import io.dico.parcels2.*
import io.dico.parcels2.options.PlotmeMigrationOptions
import io.dico.parcels2.storage.Storage
import io.dico.parcels2.storage.exposed.abs
import io.dico.parcels2.storage.exposed.greater
import io.dico.parcels2.storage.migration.Migration
import io.dico.parcels2.util.toUUID
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.newFixedThreadPoolContext
import org.jetbrains.exposed.sql.*
import org.slf4j.LoggerFactory
import java.sql.Blob
import java.util.UUID
import javax.sql.DataSource

class PlotmeMigration(val options: PlotmeMigrationOptions) : Migration {
    private var dataSource: DataSource? = null
    private var database: Database? = null
    private var isShutdown: Boolean = false
    private val mlogger = LoggerFactory.getLogger("PlotMe Migrator")
    val dispatcher = newFixedThreadPoolContext(1, "PlotMe Migration Thread")

    private fun <T> transaction(statement: Transaction.() -> T) = org.jetbrains.exposed.sql.transactions.transaction(database!!, statement)

    override fun migrateTo(storage: Storage): Job {
        return launch(dispatcher) {
            init()
            doWork(storage)
            shutdown()
        }
    }

    fun init() {
        if (isShutdown || database != null) throw IllegalStateException()
        dataSource = options.storage.getDataSourceFactory()!!()
        database = Database.connect(dataSource!!)
    }

    fun shutdown() {
        if (isShutdown) throw IllegalStateException()
        dataSource?.let {
            (it as? HikariDataSource)?.close()
        }
        database = null
        isShutdown = true
    }

    suspend fun doWork(target: Storage) {
        val exit = transaction {
            (!PlotmePlotsT.exists()).also {
                if (it) mlogger.warn("Plotme tables don't appear to exist. Exiting.")
            }
        }
        if (exit) return

        val worldCache = options.worldsFromTo.mapValues { ParcelWorldId(it.value) }

        fun getParcelId(table: PlotmeTable, row: ResultRow): ParcelId? {
            val world = worldCache[row[table.world_name]] ?: return null
            return ParcelId(world, row[table.px], row[table.pz])
        }

        fun PlotmePlotPlayerMap.transmitPlotmeAddedTable(kind: AddedStatus) {
            selectAll().forEach { row ->
                val parcel = getParcelId(this, row) ?: return@forEach
                val profile = StatusKey.safe(row[player_uuid]?.toUUID(), row[player_name]) ?: return@forEach
                target.setParcelPlayerStatus(parcel, profile, kind)
            }
        }

        mlogger.info("Transmitting data from plotmeplots table")
        transaction {
            PlotmePlotsT.selectAll()
                .orderBy(PlotmePlotsT.world_name)
                .orderBy(with(SqlExpressionBuilder) { greater(PlotmePlotsT.px.abs(), PlotmePlotsT.pz.abs()) })
                .forEach { row ->
                    val parcel = getParcelId(PlotmePlotsT, row) ?: return@forEach
                    val owner = PlayerProfile.safe(row[PlotmePlotsT.owner_uuid]?.toUUID(), row[PlotmePlotsT.owner_name])
                    target.setParcelOwner(parcel, owner)
                    target.setParcelOwnerSignOutdated(parcel, true)
                }
        }

        mlogger.info("Transmitting data from plotmeallowed table")
        transaction {
            PlotmeAllowedT.transmitPlotmeAddedTable(AddedStatus.ALLOWED)
        }

        mlogger.info("Transmitting data from plotmedenied table")
        transaction {
            PlotmeDeniedT.transmitPlotmeAddedTable(AddedStatus.BANNED)
        }

        mlogger.warn("Data has been **transmitted**.")
        mlogger.warn("Loading parcel data might take a while as enqueued transactions from this migration are completed.")
    }

    private fun Blob.toUUID(): UUID? {
        val ba = ByteArray(16)
        val count = binaryStream.read(ba, 0, 16)
        if (count < 16) return null
        return ba.toUUID()
    }


}