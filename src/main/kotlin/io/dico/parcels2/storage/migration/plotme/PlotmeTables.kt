package io.dico.parcels2.storage.migration.plotme

import org.jetbrains.exposed.sql.Table

const val uppercase: Boolean = false
@Suppress("ConstantConditionIf")
fun String.toCorrectCase() = if (uppercase) this else toLowerCase()

sealed class PlotmeTable(name: String) : Table(name) {
    val px = integer("idX").primaryKey()
    val pz = integer("idZ").primaryKey()
    val world_name = varchar("world", 32).primaryKey()
}

object PlotmePlotsT : PlotmeTable("plotmePlots".toCorrectCase()) {
    val owner_name = varchar("owner", 32)
    val owner_uuid = blob("ownerid").nullable()
}

sealed class PlotmePlotPlayerMap(name: String) : PlotmeTable(name) {
    val player_name = varchar("player", 32)
    val player_uuid = blob("playerid").nullable()
}

object PlotmeAllowedT : PlotmePlotPlayerMap("plotmeAllowed".toCorrectCase())
object PlotmeDeniedT : PlotmePlotPlayerMap("plotmeDenied".toCorrectCase())
