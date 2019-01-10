@file:Suppress("RedundantSuspendModifier", "DEPRECATION")

package io.dico.parcels2.storage.migration.plotme

import com.zaxxer.hikari.HikariDataSource
import io.dico.parcels2.*
import io.dico.parcels2.options.PlotmeMigrationOptions
import io.dico.parcels2.storage.Storage
import io.dico.parcels2.storage.exposed.abs
import io.dico.parcels2.storage.exposed.greaterOf
import io.dico.parcels2.storage.migration.Migration
import io.dico.parcels2.storage.migration.plotme.PlotmeTables.PlotmePlotPlayerMap
import io.dico.parcels2.storage.migration.plotme.PlotmeTables.PlotmeTable
import io.dico.parcels2.storage.toUUID
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.newFixedThreadPoolContext
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
    private val tables = PlotmeTables(options.tableNamesUppercase)
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

    suspend fun doWork(target: Storage) = with (tables) {
        val exit = transaction {
            (!PlotmePlots.exists()).also {
                if (it) mlogger.warn("Plotme tables don't appear to exist. Exiting.")
            }
        }
        if (exit) return

        val worldCache = options.worldsFromTo.mapValues { ParcelWorldId(it.value) }

        fun getParcelId(table: PlotmeTable, row: ResultRow): ParcelId? {
            val world = worldCache[row[table.world_name]] ?: return null
            return ParcelId(world, row[table.px], row[table.pz])
        }

        fun PlotmePlotPlayerMap.transmitPlotmeAddedTable(kind: Privilege) {
            selectAll().forEach { row ->
                val parcel = getParcelId(this, row) ?: return@forEach
                val profile = PrivilegeKey.safe(row[player_uuid]?.toUUID(), row[player_name]) ?: return@forEach
                target.setLocalPrivilege(parcel, profile, kind)
            }
        }

        mlogger.info("Transmitting data from plotmeplots table")
        var count = 0
        transaction {

            PlotmePlots.selectAll()
                .orderBy(PlotmePlots.world_name)
                .orderBy(greaterOf(PlotmePlots.px.abs(), PlotmePlots.pz.abs()))
                .forEach { row ->
                    val parcel = getParcelId(PlotmePlots, row) ?: return@forEach
                    val owner = PlayerProfile.safe(row[PlotmePlots.owner_uuid]?.toUUID(), row[PlotmePlots.owner_name])
                    target.setParcelOwner(parcel, owner)
                    target.setParcelOwnerSignOutdated(parcel, true)
                    ++count
                }
        }

        mlogger.info("Transmitting data from plotmeallowed table")
        transaction {
            PlotmeAllowed.transmitPlotmeAddedTable(Privilege.CAN_BUILD)
        }

        mlogger.info("Transmitting data from plotmedenied table")
        transaction {
            PlotmeDenied.transmitPlotmeAddedTable(Privilege.BANNED)
        }

        mlogger.warn("Data has been **transmitted**. $count plots were migrated to the parcels database.")
        mlogger.warn("Loading parcel data might take a while as enqueued transactions from this migration are completed.")
    }

    private fun Blob.toUUID(): UUID? {
        val ba = ByteArray(16)
        val count = binaryStream.read(ba, 0, 16)
        if (count < 16) return null
        return ba.toUUID()
    }


}