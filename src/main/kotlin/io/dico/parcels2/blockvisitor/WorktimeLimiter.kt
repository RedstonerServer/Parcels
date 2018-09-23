package io.dico.parcels2.blockvisitor

import io.dico.parcels2.ParcelsPlugin
import io.dico.parcels2.logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart.LAZY
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.bukkit.scheduler.BukkitTask
import java.lang.System.currentTimeMillis
import java.util.LinkedList
import kotlin.coroutines.Continuation
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

typealias TimeLimitedTask = suspend WorkerScope.() -> Unit
typealias WorkerUpdateLister = Worker.(Double, Long) -> Unit

data class TickWorktimeOptions(var workTime: Int, var tickInterval: Int)

interface WorktimeLimiter {
    /**
     * Submit a [task] that should be run synchronously, but limited such that it does not stall the server
     * a bunch
     */
    fun submit(task: TimeLimitedTask): Worker

    /**
     * Get a list of all workers
     */
    val workers: List<Worker>

    /**
     * Attempts to complete any remaining tasks immediately, without suspension.
     */
    fun completeAllTasks()
}

interface Timed {
    /**
     * The time that elapsed since this worker was dispatched, in milliseconds
     */
    val elapsedTime: Long
}

interface Worker : Timed {
    /**
     * The coroutine associated with this worker, if any
     */
    val job: Job?

    /**
     * true if this worker has completed
     */
    val isComplete: Boolean

    /**
     * If an exception was thrown during the execution of this task,
     * returns that exception. Returns null otherwise.
     */
    val completionException: Throwable?

    /**
     * A value indicating the progress of this worker, in the range 0.0 <= progress <= 1.0
     * with no guarantees to its accuracy. May be null.
     */
    val progress: Double?

    /**
     * Calls the given [block] whenever the progress of this worker is updated,
     * if [minInterval] milliseconds expired since the last call.
     * The first call occurs after at least [minDelay] milliseconds in a likewise manner.
     * Repeated invocations of this method result in an [IllegalStateException]
     *
     * if [asCompletionListener] is true, [onCompleted] is called with the same [block]
     */
    fun onProgressUpdate(minDelay: Int, minInterval: Int, asCompletionListener: Boolean = true, block: WorkerUpdateLister): Worker

    /**
     * Calls the given [block] when this worker completes, with the progress value 1.0.
     * Multiple listeners may be registered to this function.
     */
    fun onCompleted(block: WorkerUpdateLister): Worker

    /**
     * Await completion of this worker
     */
    suspend fun awaitCompletion()
}

interface WorkerScope : Timed {
    /**
     * A task should call this frequently during its execution, such that the timer can suspend it when necessary.
     */
    suspend fun markSuspensionPoint()

    /**
     * A task should call this method to indicate its progress
     */
    fun setProgress(progress: Double)
}

private interface WorkerContinuation : Worker, WorkerScope {
    /**
     * Start or resumes the execution of this worker
     * and returns true if the worker completed
     *
     * [worktime] is the maximum amount of time, in milliseconds,
     * that this job may run for until suspension.
     *
     * If [worktime] is not positive, the worker will complete
     * without suspension and this method will always return true.
     */
    fun resume(worktime: Long): Boolean
}

/**
 * An object that controls one or more jobs, ensuring that they don't stall the server too much.
 * There is a configurable maxiumum amount of milliseconds that can be allocated to all workers together in each server tick
 * This object attempts to split that maximum amount of milliseconds equally between all jobs
 */
class TickWorktimeLimiter(private val plugin: ParcelsPlugin, var options: TickWorktimeOptions) : WorktimeLimiter {
    // The currently registered bukkit scheduler task
    private var bukkitTask: BukkitTask? = null
    // The workers.
    private val _workers = LinkedList<WorkerContinuation>()
    override val workers: List<Worker> = _workers

    override fun submit(task: TimeLimitedTask): Worker {
        val worker: WorkerContinuation = WorkerImpl(plugin, task)

        if (bukkitTask == null) {
            val completed = worker.resume(options.workTime.toLong())
            if (completed) return worker
            bukkitTask = plugin.scheduleRepeating(0, options.tickInterval) { tickJobs() }
        }

        _workers.addFirst(worker)
        return worker
    }

    private fun tickJobs() {
        val workers = _workers
        if (workers.isEmpty()) return
        val tickStartTime = System.currentTimeMillis()

        val iterator = workers.listIterator(index = 0)
        while (iterator.hasNext()) {
            val time = System.currentTimeMillis()
            val timeElapsed = time - tickStartTime
            val timeLeft = options.workTime - timeElapsed
            if (timeLeft <= 0) return

            val count = workers.size - iterator.nextIndex()
            val timePerJob = (timeLeft + count - 1) / count
            val worker = iterator.next()
            val completed = worker.resume(timePerJob)
            if (completed) {
                iterator.remove()
            }
        }

        if (workers.isEmpty()) {
            bukkitTask?.cancel()
            bukkitTask = null
        }
    }

    override fun completeAllTasks() {
        _workers.forEach {
            it.resume(-1)
        }
        _workers.clear()
        bukkitTask?.cancel()
        bukkitTask = null
    }

}

private class WorkerImpl(
    val scope: CoroutineScope,
    val task: TimeLimitedTask
) : WorkerContinuation, CoroutineScope by scope {
    override var job: Job? = null; private set

    override val elapsedTime
        get() = job?.let {
            if (it.isCompleted) startTimeOrElapsedTime
            else currentTimeMillis() - startTimeOrElapsedTime
        } ?: 0L

    override val isComplete get() = job?.isCompleted == true

    override var completionException: Throwable? = null; private set

    override var progress: Double? = null; private set

    private var startTimeOrElapsedTime: Long = 0L // startTime before completed, elapsed time otherwise
    private var onProgressUpdate: WorkerUpdateLister? = null
    private var progressUpdateInterval: Int = 0
    private var lastUpdateTime: Long = 0L
    private var onCompleted: WorkerUpdateLister? = null
    private var continuation: Continuation<Unit>? = null
    private var nextSuspensionTime: Long = 0L
    private var completeForcefully = false

    private fun initJob(job: Job) {
        this.job?.let { throw IllegalStateException() }
        this.job = job
        startTimeOrElapsedTime = System.currentTimeMillis()
        job.invokeOnCompletion { exception ->
            // report any error that occurred
            completionException = exception?.also {
                if (it !is CancellationException)
                    logger.error("TimeLimitedTask generated an exception", it)
            }

            // convert to elapsed time here
            startTimeOrElapsedTime = System.currentTimeMillis() - startTimeOrElapsedTime
            onCompleted?.let { it(1.0, elapsedTime) }

            onCompleted = null
            onProgressUpdate = { prog, el -> }
        }
    }

    override fun onProgressUpdate(minDelay: Int, minInterval: Int, asCompletionListener: Boolean, block: WorkerUpdateLister): Worker {
        onProgressUpdate?.let { throw IllegalStateException() }
        if (asCompletionListener) onCompleted(block)
        if (isComplete) return this
        onProgressUpdate = block
        progressUpdateInterval = minInterval
        lastUpdateTime = System.currentTimeMillis() + minDelay - minInterval

        return this
    }

    override fun onCompleted(block: WorkerUpdateLister): Worker {
        if (isComplete) {
            block(1.0, startTimeOrElapsedTime)
            return this
        }

        val cur = onCompleted
        onCompleted = if (cur == null) {
            block
        } else {
            fun Worker.(prog: Double, el: Long) {
                cur(prog, el)
                block(prog, el)
            }
        }
        return this
    }

    override suspend fun markSuspensionPoint() {
        if (System.currentTimeMillis() >= nextSuspensionTime && !completeForcefully)
            suspendCoroutineUninterceptedOrReturn { cont: Continuation<Unit> ->
                continuation = cont
                COROUTINE_SUSPENDED
            }
    }

    override fun setProgress(progress: Double) {
        this.progress = progress
        val onProgressUpdate = onProgressUpdate ?: return
        val time = System.currentTimeMillis()
        if (time > lastUpdateTime + progressUpdateInterval) {
            onProgressUpdate(progress, elapsedTime)
            lastUpdateTime = time
        }
    }

    override fun resume(worktime: Long): Boolean {
        if (worktime > 0) {
            nextSuspensionTime = currentTimeMillis() + worktime
        } else {
            completeForcefully = true
        }

        continuation?.let {
            continuation = null
            it.resume(Unit)
            return continuation == null
        }

        job?.let {
            nextSuspensionTime = 0L
            throw IllegalStateException()
        }

        try {
            val job = launch(start = LAZY) { task() }
            initJob(job = job)
            job.start()
        } catch (t: Throwable) {
            // do nothing: handled by job.invokeOnCompletion()
        }

        return continuation == null
    }

    override suspend fun awaitCompletion() {
        if (isComplete) return

        // easy path - if the job was initialized already
        job?.apply { join(); return }

        // other way.
        return suspendCoroutine { cont ->
            onCompleted { prog, el -> cont.resume(Unit) }
        }
    }

}

/*
/**
 * While the implementation of [kotlin.coroutines.experimental.intrinsics.intercepted] is intrinsic, it should look something like this
 * We don't care for intercepting the coroutine as we want it to resume immediately when we call resume().
 * Thus, above, we use an unintercepted suspension. It's not necessary as the dispatcher (or interceptor) also calls it synchronously, but whatever.
 */
private fun <T> Continuation<T>.interceptedImpl(): Continuation<T> {
    return context[ContinuationInterceptor]?.interceptContinuation(this) ?: this
}
 */
