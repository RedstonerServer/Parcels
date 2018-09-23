package io.dico.parcels2.util

import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitTask

interface PluginScheduler {
    val plugin: Plugin

    fun schedule(delay: Int, task: () -> Unit): BukkitTask {
        return plugin.server.scheduler.runTaskLater(plugin, task, delay.toLong())
    }

    fun scheduleRepeating(delay: Int, interval: Int, task: () -> Unit): BukkitTask {
        return plugin.server.scheduler.runTaskTimer(plugin, task, delay.toLong(), interval.toLong())
    }
}

@Suppress("NOTHING_TO_INLINE")
inline fun PluginScheduler.schedule(noinline task: () -> Unit) = schedule(0, task)

