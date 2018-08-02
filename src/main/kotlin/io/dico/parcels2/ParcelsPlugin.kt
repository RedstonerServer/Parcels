package io.dico.parcels2

import io.dico.dicore.Registrator
import io.dico.dicore.command.EOverridePolicy
import io.dico.dicore.command.ICommandDispatcher
import io.dico.parcels2.blockvisitor.TickWorktimeLimiter
import io.dico.parcels2.blockvisitor.WorktimeLimiter
import io.dico.parcels2.command.getParcelCommands
import io.dico.parcels2.defaultimpl.GlobalAddedDataManagerImpl
import io.dico.parcels2.defaultimpl.ParcelProviderImpl
import io.dico.parcels2.listener.ParcelEntityTracker
import io.dico.parcels2.listener.ParcelListeners
import io.dico.parcels2.storage.Storage
import io.dico.parcels2.storage.yamlObjectMapper
import io.dico.parcels2.util.FunctionHelper
import io.dico.parcels2.util.tryCreate
import org.bukkit.Bukkit
import org.bukkit.generator.ChunkGenerator
import org.bukkit.plugin.java.JavaPlugin
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File

val logger: Logger = LoggerFactory.getLogger("ParcelsPlugin")
private inline val plogger get() = logger

class ParcelsPlugin : JavaPlugin() {
    lateinit var optionsFile: File; private set
    lateinit var options: Options; private set
    lateinit var parcelProvider: ParcelProvider; private set
    lateinit var storage: Storage; private set
    lateinit var globalAddedData: GlobalAddedDataManager; private set

    val registrator = Registrator(this)
    lateinit var entityTracker: ParcelEntityTracker; private set
    private var listeners: ParcelListeners? = null
    private var cmdDispatcher: ICommandDispatcher? = null

    val functionHelper: FunctionHelper = FunctionHelper(this)
    val worktimeLimiter: WorktimeLimiter by lazy { TickWorktimeLimiter(this, options.tickWorktime) }

    override fun onEnable() {
        plogger.info("Debug enabled: ${plogger.isDebugEnabled}")
        if (!init()) {
            Bukkit.getPluginManager().disablePlugin(this)
        }
    }

    override fun onDisable() {
        worktimeLimiter.completeAllTasks()
        cmdDispatcher?.unregisterFromCommandMap()
    }

    private fun init(): Boolean {
        optionsFile = File(dataFolder, "options.yml")
        options = Options()
        parcelProvider = ParcelProviderImpl(this)

        try {
            if (!loadOptions()) return false

            try {
                storage = options.storage.newStorageInstance()
                storage.init()
            } catch (ex: Exception) {
                plogger.error("Failed to connect to database", ex)
                return false
            }

            globalAddedData = GlobalAddedDataManagerImpl(this)
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
            optionsFile.exists() -> yamlObjectMapper.readerForUpdating(options).readValue<Options>(optionsFile)
            optionsFile.tryCreate() -> {
                options.addWorld("parcels")
                try {
                    yamlObjectMapper.writeValue(optionsFile, options)
                } catch (ex: Throwable) {
                    optionsFile.delete()
                    throw ex
                }
                plogger.warn("Created options file with a world template. Please review it before next start.")
                return false
            }
            else -> {
                plogger.error("Failed to save options file ${optionsFile.canonicalPath}")
                return false
            }
        }
        return true
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
            listeners = ParcelListeners(parcelProvider, entityTracker)
            registrator.registerListeners(listeners!!)
        }
    }

}