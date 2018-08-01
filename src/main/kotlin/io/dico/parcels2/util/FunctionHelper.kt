package io.dico.parcels2.util

import io.dico.parcels2.ParcelsPlugin
import kotlinx.coroutines.experimental.*
import org.bukkit.scheduler.BukkitTask
import kotlin.coroutines.experimental.CoroutineContext

@Suppress("NOTHING_TO_INLINE")
class FunctionHelper(val plugin: ParcelsPlugin) {
    val mainThreadDispatcher: MainThreadDispatcher = MainThreadDispatcherImpl()

    fun <T> deferLazilyOnMainThread(block: suspend CoroutineScope.() -> T): Deferred<T> {
        return async(context = mainThreadDispatcher, start = CoroutineStart.LAZY, block = block)
    }

    fun <T> deferUndispatchedOnMainThread(block: suspend CoroutineScope.() -> T): Deferred<T> {
        return async(context = mainThreadDispatcher, start = CoroutineStart.UNDISPATCHED, block = block)
    }

    fun launchLazilyOnMainThread(block: suspend CoroutineScope.() -> Unit): Job {
        return launch(context = mainThreadDispatcher, start = CoroutineStart.LAZY, block = block)
    }

    inline fun schedule(noinline task: () -> Unit) = schedule(0, task)

    fun schedule(delay: Int, task: () -> Unit): BukkitTask {
        return plugin.server.scheduler.runTaskLater(plugin, task, delay.toLong())
    }

    fun scheduleRepeating(delay: Int, interval: Int, task: () -> Unit): BukkitTask {
        return plugin.server.scheduler.runTaskTimer(plugin, task, delay.toLong(), interval.toLong())
    }

    abstract class MainThreadDispatcher : CoroutineDispatcher() {
        abstract val mainThread: Thread
        abstract fun runOnMainThread(task: Runnable)
    }

    private inner class MainThreadDispatcherImpl : MainThreadDispatcher() {
        override val mainThread: Thread = Thread.currentThread()

        override fun dispatch(context: CoroutineContext, block: Runnable) {
            runOnMainThread(block)
        }

        @Suppress("OVERRIDE_BY_INLINE")
        override inline fun runOnMainThread(task: Runnable) {
            if (Thread.currentThread() === mainThread) task.run()
            else plugin.server.scheduler.runTaskLater(plugin, task, 0)
        }
    }

}
