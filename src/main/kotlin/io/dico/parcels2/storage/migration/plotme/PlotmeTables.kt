package io.dico.parcels2.storage.migration.plotme

import org.jetbrains.exposed.sql.Table

class PlotmeTables(val uppercase: Boolean) {
    fun String.toCorrectCase() = if (uppercase) this else toLowerCase()

    val PlotmePlots = PlotmePlotsT()
    val PlotmeAllowed = PlotmeAllowedT()
    val PlotmeDenied = PlotmeDeniedT()

    inner abstract class PlotmeTable(name: String) : Table(name) {
        val px = integer("idX").primaryKey()
        val pz = integer("idZ").primaryKey()
        val world_name = varchar("world", 32).primaryKey()
    }

    inner abstract class PlotmePlotPlayerMap(name: String) : PlotmeTable(name) {
        val player_name = varchar("player", 32)
        val player_uuid = blob("playerid").nullable()
    }

    inner class PlotmePlotsT : PlotmeTable("plotmePlots".toCorrectCase()) {
        val owner_name = varchar("owner", 32)
        val owner_uuid = blob("ownerid").nullable()
    }

    inner class PlotmeAllowedT : PlotmePlotPlayerMap("plotmeAllowed".toCorrectCase())
    inner class PlotmeDeniedT : PlotmePlotPlayerMap("plotmeDenied".toCorrectCase())
}

