package io.dico.parcels2.util

import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitTask

inline fun Plugin.doAwait(checkNow: Boolean = true, configure: AwaitTask.() -> Unit) {
    with(AwaitTask()) {
        configure()
        start(checkNow = checkNow)
    }
}

private typealias Action<T> = () -> T

class AwaitTask : Runnable {
    //@formatter:off
    var cond: Action<Boolean>? = null               ; set(value) { checkNotRunning(); field = value }
    var onSuccess: Action<Unit>? = null             ; set(value) { checkNotRunning(); field = value }
    var onFailure: Action<Unit>? = null             ; set(value) { checkNotRunning(); field = value }
    var delay: Int = -1                             ; set(value) { checkNotRunning(); field = value }
    var interval: Int = 20                          ; set(value) { checkNotRunning(); field = value }
    var maxChecks: Int = 0                          ; set(value) { checkNotRunning(); field = value }

    var task: BukkitTask? = null                    ; private set
    var elapsedChecks = 0                           ; private set
    var cancelled = false                           ; private set
    //@formatter:on

    fun Plugin.start(checkNow: Boolean = true) {
        if (cancelled) throw IllegalStateException()

        requireNotNull(cond)
        requireNotNull(onSuccess)

        if (checkNow && check()) {
            cancel()
            onSuccess!!.invoke()
            return
        }

        task = server.scheduler.runTaskTimer(this, this@AwaitTask, delay.toLong(), interval.toLong())
    }

    override fun run() {
        if (task?.isCancelled != false) return

        if (check()) {
            cancel()
            onSuccess!!.invoke()
        }

        elapsedChecks++

        if (maxChecks in 1 until elapsedChecks) {
            cancel()
            onFailure?.invoke()
        }
    }

    private fun check(): Boolean {
        elapsedChecks++
        return cond!!.invoke()
    }

    fun cancel() {
        task?.cancel()
        cancelled = true
    }

    private fun checkNotRunning() {
        if (cancelled || task != null) throw IllegalStateException()
    }

}