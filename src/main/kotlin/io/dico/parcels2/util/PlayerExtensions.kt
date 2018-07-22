package io.dico.parcels2.util

import io.dico.dicore.Formatting
import io.dico.parcels2.ParcelsPlugin
import io.dico.parcels2.logger
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

inline val Player.hasBanBypass get() = hasPermission("plots.admin.bypass.ban")
inline val Player.hasBuildAnywhere get() = hasPermission("plots.admin.bypass.build")
inline val Player.hasGamemodeBypass get() = hasPermission("plots.admin.bypass.gamemode")
inline val Player.hasAdminManage get() = hasPermission("plots.admin.manage")
inline val Player.hasPlotHomeOthers get() = hasPermission("plots.command.home.others")
inline val Player.hasRandomSpecific get() = hasPermission("plots.command.random.specific")
val Player.plotLimit: Int
    get() {
        for (info in effectivePermissions) {
            val perm = info.permission
            if (perm.startsWith("plots.limit.")) {
                val limitString = perm.substring("plots.limit.".length)
                if (limitString == "*") {
                    return Int.MAX_VALUE
                }
                return limitString.toIntOrNull() ?: DEFAULT_LIMIT.also {
                    logger.warn("$name has permission '$perm'. The suffix can not be parsed to an integer (or *).")
                }
            }
        }
        return DEFAULT_LIMIT
    }

private const val DEFAULT_LIMIT = 1
private val prefix = Formatting.translateChars('&', "&4[&c${JavaPlugin.getPlugin(ParcelsPlugin::class.java).name}&4] &a")

fun Player.sendPlotMessage(except: Boolean = false, nopermit: Boolean = false, message: String) {
    if (except) {
        sendMessage(prefix + Formatting.YELLOW + Formatting.translateChars('&', message))
    } else if (nopermit) {
        sendMessage(prefix + Formatting.RED + Formatting.translateChars('&', message))
    } else {
        sendMessage(prefix + Formatting.translateChars('&', message))
    }
}