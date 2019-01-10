package io.dico.parcels2

import io.dico.dicore.Registrator
import io.dico.dicore.command.EOverridePolicy
import io.dico.dicore.command.ICommandDispatcher
import io.dico.parcels2.command.getParcelCommands
import io.dico.parcels2.defaultimpl.GlobalPrivilegesManagerImpl
import io.dico.parcels2.defaultimpl.ParcelProviderImpl
import io.dico.parcels2.listener.ParcelEntityTracker
import io.dico.parcels2.listener.ParcelListeners
import io.dico.parcels2.listener.WorldEditListener
import io.dico.parcels2.options.Options
import io.dico.parcels2.options.optionsMapper
import io.dico.parcels2.storage.Storage
import io.dico.parcels2.util.MainThreadDispatcher
import io.dico.parcels2.util.PluginAware
import io.dico.parcels2.util.ext.tryCreate
import io.dico.parcels2.util.isServerThread
import io.dico.parcels2.util.scheduleRepeating
import kotlinx.coroutines.CoroutineScope
import org.bukkit.Bukkit
import org.bukkit.generator.ChunkGenerator
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.java.JavaPlugin
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import kotlin.coroutines.CoroutineContext

val logger: Logger = LoggerFactory.getLogger("ParcelsPlugin")
private inline val plogger get() = logger

class ParcelsPlugin : JavaPlugin(), CoroutineScope, PluginAware {
    lateinit var optionsFile: File; private set
    lateinit var options: Options; private set
    lateinit var parcelProvider: ParcelProvider; private set
    lateinit var storage: Storage; private set
    lateinit var globalPrivileges: GlobalPrivilegesManager; private set

    val registrator = Registrator(this)
    lateinit var entityTracker: ParcelEntityTracker; private set
    private var listeners: ParcelListeners? = null
    private var cmdDispatcher: ICommandDispatcher? = null

    override val coroutineContext: CoroutineContext = MainThreadDispatcher(this)
    override val plugin: Plugin get() = this
    val jobDispatcher: JobDispatcher by lazy { BukkitJobDispatcher(this, this, options.tickJobtime) }

    override fun onEnable() {
        plogger.info("Is server thread: ${isServerThread()}")
        plogger.info("Debug enabled: ${plogger.isDebugEnabled}")
        plogger.debug(System.getProperty("user.dir"))
        if (!init()) {
            Bukkit.getPluginManager().disablePlugin(this)
        }
    }

    override fun onDisable() {
        val hasWorkers = jobDispatcher.jobs.isNotEmpty()
        if (hasWorkers) {
            plogger.warn("Parcels is attempting to complete all ${jobDispatcher.jobs.size} remaining jobs before shutdown...")
        }
        jobDispatcher.completeAllTasks()
        if (hasWorkers) {
            plogger.info("Parcels has completed the remaining jobs.")
        }

        cmdDispatcher?.unregisterFromCommandMap()
    }

    private fun init(): Boolean {
        optionsFile = File(dataFolder, "options.yml")
        options = Options()
        parcelProvider = ParcelProviderImpl(this)

        try {
            if (!loadOptions()) return false

            try {
                storage = options.storage.newInstance()
                storage.init()
            } catch (ex: Exception) {
                plogger.error("Failed to connect to database", ex)
                return false
            }

            globalPrivileges = GlobalPrivilegesManagerImpl(this)
            entityTracker = ParcelEntityTracker(parcelProvider)
        } catch (ex: Exception) {
            plogger.error("Error loading options", ex)
            return false
        }

        registerListeners()
        registerCommands()

        parcelProvider.loadWorlds()
        return true
    }

    fun loadOptions(): Boolean {
        when {
            optionsFile.exists() -> optionsMapper.readerForUpdating(options).readValue<Options>(optionsFile)
            else -> run {
                options.addWorld("parcels")
                if (saveOptions()) {
                    plogger.warn("Created options file with a world template. Please review it before next start.")
                } else {
                    plogger.error("Failed to save options file ${optionsFile.canonicalPath}")
                }
                return false
            }
        }
        return true
    }

    fun saveOptions(): Boolean {
        if (optionsFile.tryCreate()) {
            try {
                optionsMapper.writeValue(optionsFile, options)
            } catch (ex: Throwable) {
                optionsFile.delete()
                throw ex
            }
            return true
        }
        return false
    }

    override fun getDefaultWorldGenerator(worldName: String, generatorId: String?): ChunkGenerator? {
        return parcelProvider.getWorldGenerator(worldName)
    }

    private fun registerCommands() {
        cmdDispatcher = getParcelCommands(this).apply {
            registerToCommandMap("parcels:", EOverridePolicy.FALLBACK_ONLY)
        }
    }

    private fun registerListeners() {
        if (listeners == null) {
            listeners = ParcelListeners(parcelProvider, entityTracker, storage)
            registrator.registerListeners(listeners!!)

            val worldEditPlugin = server.pluginManager.getPlugin("WorldEdit")
            if (worldEditPlugin != null) {
                WorldEditListener.register(this, worldEditPlugin)
            }
        }

        scheduleRepeating(5, delay = 100, task = entityTracker::tick)
    }

}