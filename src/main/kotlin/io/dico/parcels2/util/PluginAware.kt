@file:Suppress("RedundantLambdaArrow")

package io.dico.parcels2.util

import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitTask

interface PluginAware {
    val plugin: Plugin
}

inline fun PluginAware.schedule(delay: Int = 0, crossinline task: () -> Unit): BukkitTask {
    return plugin.server.scheduler.runTaskLater(plugin, { -> task() }, delay.toLong())
}

inline fun PluginAware.scheduleRepeating(interval: Int, delay: Int = 0, crossinline task: () -> Unit): BukkitTask {
    return plugin.server.scheduler.runTaskTimer(plugin, { -> task() }, delay.toLong(), interval.toLong())
}

