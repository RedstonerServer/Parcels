package io.dico.parcels2.options

import io.dico.parcels2.storage.migration.Migration
import io.dico.parcels2.storage.migration.plotme.PlotmeMigration
import kotlin.reflect.KClass

object MigrationOptionsFactories : PolymorphicOptionsFactories<Migration>("kind", MigrationOptions::class, PlotmeMigrationFactory())

class MigrationOptions(kind: String = "plotme-0.17", options: Any = PlotmeMigrationOptions()) : SimplePolymorphicOptions<Migration>(kind, options, MigrationOptionsFactories)

private class PlotmeMigrationFactory : PolymorphicOptionsFactory<Migration> {
    override val supportedKeys = listOf("plotme-0.17")
    override val optionsClass: KClass<out Any> get() = PlotmeMigrationOptions::class

    override fun newInstance(key: String, options: Any, vararg extra: Any?): Migration {
        return PlotmeMigration(options as PlotmeMigrationOptions)
    }
}

class PlotmeMigrationOptions(val worldsFromTo: Map<String, String> = mapOf("plotworld" to "parcels"),
                             val storage: StorageOptions = StorageOptions(options = DataConnectionOptions(database = "plotme")),
                             val tableNamesUppercase: Boolean = false)