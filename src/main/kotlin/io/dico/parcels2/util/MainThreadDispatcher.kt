package io.dico.parcels2.util

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Runnable
import org.bukkit.plugin.Plugin
import kotlin.coroutines.CoroutineContext

abstract class MainThreadDispatcher : CoroutineDispatcher() {
    abstract val mainThread: Thread
    abstract fun runOnMainThread(task: Runnable)
}

@Suppress("FunctionName")
fun MainThreadDispatcher(plugin: Plugin): MainThreadDispatcher {
    return object : MainThreadDispatcher() {
        override val mainThread: Thread = Thread.currentThread()

        override fun dispatch(context: CoroutineContext, block: Runnable) {
            doDispatch(block)
        }

        override  fun runOnMainThread(task: Runnable) {
            doDispatch(task)
        }

        private fun doDispatch(task: Runnable) {
            if (Thread.currentThread() === mainThread) task.run()
            else plugin.server.scheduler.runTaskLater(plugin, task, 0)
        }
    }
}