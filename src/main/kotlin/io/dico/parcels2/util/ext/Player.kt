package io.dico.parcels2.util.ext

import io.dico.dicore.Formatting
import io.dico.parcels2.ParcelsPlugin
import io.dico.parcels2.logger
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.bukkit.permissions.Permissible
import org.bukkit.plugin.java.JavaPlugin

inline val OfflinePlayer.uuid get() = uniqueId

@Suppress("UsePropertyAccessSyntax")
inline val OfflinePlayer.isValid
    get() = isOnline() || hasPlayedBefore()

const val PERM_BAN_BYPASS = "parcels.admin.bypass.ban"
const val PERM_BUILD_ANYWHERE = "parcels.admin.bypass.build"
const val PERM_ADMIN_MANAGE = "parcels.admin.manage"

inline val Permissible.hasPermBanBypass get() = hasPermission(PERM_BAN_BYPASS)
inline val Permissible.hasPermGamemodeBypass get() = hasPermission("parcels.admin.bypass.gamemode")
inline val Permissible.hasPermBuildAnywhere get() = hasPermission(PERM_BUILD_ANYWHERE)
inline val Permissible.hasPermAdminManage get() = hasPermission(PERM_ADMIN_MANAGE)
inline val Permissible.hasParcelHomeOthers get() = hasPermission("parcels.command.home.others")
inline val Permissible.hasPermRandomSpecific get() = hasPermission("parcels.command.random.specific")
val Player.parcelLimit: Int
    get() {
        for (info in effectivePermissions) {
            val perm = info.permission
            if (perm.startsWith("parcels.limit.")) {
                val limitString = perm.substring("parcels.limit.".length)
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

fun Player.sendParcelMessage(except: Boolean = false, nopermit: Boolean = false, message: String) {
    if (except) {
        sendMessage(prefix + Formatting.YELLOW + Formatting.translateChars('&', message))
    } else if (nopermit) {
        sendMessage(prefix + Formatting.RED + Formatting.translateChars('&', message))
    } else {
        sendMessage(prefix + Formatting.translateChars('&', message))
    }
}

const val PLAYER_NAME_PLACEHOLDER = ":unknown_name:"
