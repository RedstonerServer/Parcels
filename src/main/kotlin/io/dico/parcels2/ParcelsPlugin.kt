package io.dico.parcels2

import io.dico.dicore.Registrator
import io.dico.dicore.command.EOverridePolicy
import io.dico.dicore.command.ICommandDispatcher
import io.dico.parcels2.command.getParcelCommands
import io.dico.parcels2.listener.ParcelEntityTracker
import io.dico.parcels2.listener.ParcelListeners
import io.dico.parcels2.storage.Storage
import io.dico.parcels2.storage.yamlObjectMapper
import io.dico.parcels2.util.tryCreate
import kotlinx.coroutines.experimental.asCoroutineDispatcher
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import org.slf4j.LoggerFactory
import java.io.File
import java.util.concurrent.Executor

val logger = LoggerFactory.getLogger("ParcelsPlugin")
private inline val plogger get() = logger

class ParcelsPlugin : JavaPlugin() {
    lateinit var optionsFile: File; private set
    lateinit var options: Options; private set
    lateinit var worlds: Worlds; private set
    lateinit var storage: Storage; private set

    val registrator = Registrator(this)
    lateinit var entityTracker: ParcelEntityTracker; private set
    private var listeners: ParcelListeners? = null
    private var cmdDispatcher: ICommandDispatcher? = null

    val mainThreadDispatcher = Executor { server.scheduler.runTask(this, it) }.asCoroutineDispatcher()

    override fun onEnable() {
        plogger.info("Debug enabled: ${plogger.isDebugEnabled}")
        if (!init()) {
            Bukkit.getPluginManager().disablePlugin(this)
        }
    }

    override fun onDisable() {
        cmdDispatcher?.unregisterFromCommandMap()
    }

    private fun init(): Boolean {
        optionsFile = File(dataFolder, "options.yml")
        options = Options()
        worlds = Worlds(this)

        try {
            if (!loadOptions()) return false

            try {
                storage = options.storage.newStorageInstance()
                storage.init()
            } catch (ex: Exception) {
                plogger.error("Failed to connect to database", ex)
                return false
            }

            worlds.loadWorlds(options)
        } catch (ex: Exception) {
            plogger.error("Error loading options", ex)
            return false
        }

        entityTracker = ParcelEntityTracker(worlds)
        registerListeners()
        registerCommands()

        return true
    }

    fun loadOptions(): Boolean {
        if (optionsFile.exists()) {
            yamlObjectMapper.readerForUpdating(options).readValue<Options>(optionsFile)
        } else if (optionsFile.tryCreate()) {
            options.addWorld("plotworld", WorldOptions())
            try {
                yamlObjectMapper.writeValue(optionsFile, options)
            } catch (ex: Throwable) {
                optionsFile.delete()
                throw ex
            }
        } else {
            plogger.error("Failed to save options file ${optionsFile.canonicalPath}")
            return false
        }
        return true
    }

    private fun registerCommands() {
        cmdDispatcher = getParcelCommands(this).apply {
            registerToCommandMap("parcels:", EOverridePolicy.FALLBACK_ONLY)
        }
    }

    private fun registerListeners() {
        if (listeners != null) {
            listeners = ParcelListeners(worlds, entityTracker)
            registrator.registerListeners(listeners!!)
        }
    }

}