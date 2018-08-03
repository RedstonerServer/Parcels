package io.dico.parcels2.options

import io.dico.parcels2.storage.migration.Migration
import kotlin.reflect.KClass

object MigrationOptionsFactories : PolymorphicOptionsFactories<Migration>("kind", MigrationOptions::class, PlotmeMigrationFactory())

class MigrationOptions(kind: String, options: Any) : SimplePolymorphicOptions<Migration>(kind, options, MigrationOptionsFactories)

private class PlotmeMigrationFactory : PolymorphicOptionsFactory<Migration> {
    override val supportedKeys = listOf("plotme-0.17")
    override val optionsClass: KClass<out Any> get() = TODO()

    override fun newInstance(key: String, options: Any, vararg extra: Any?): Migration {
        TODO()
    }
}
