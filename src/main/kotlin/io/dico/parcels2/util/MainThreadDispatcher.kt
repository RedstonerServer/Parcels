package io.dico.parcels2.util

import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Delay
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.timeunit.TimeUnit
import org.bukkit.plugin.Plugin
import kotlin.coroutines.CoroutineContext

abstract class MainThreadDispatcher : CoroutineDispatcher(), Delay {
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

        override fun scheduleResumeAfterDelay(time: Long, unit: TimeUnit, continuation: CancellableContinuation<Unit>) {
            val task = Runnable {
                with (continuation) { resumeUndispatched(Unit) }
            }

            val millis = unit.toMillis(time)
            plugin.server.scheduler.runTaskLater(plugin, task, (millis + 25) / 50 - 1)
        }
    }
}