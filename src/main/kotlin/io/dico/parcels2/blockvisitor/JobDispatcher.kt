package io.dico.parcels2.blockvisitor

import io.dico.parcels2.ParcelsPlugin
import io.dico.parcels2.logger
import io.dico.parcels2.util.math.ext.clampMin
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart.LAZY
import kotlinx.coroutines.Job as CoroutineJob
import kotlinx.coroutines.launch
import org.bukkit.scheduler.BukkitTask
import java.lang.System.currentTimeMillis
import java.util.LinkedList
import kotlin.coroutines.Continuation
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn
import kotlin.coroutines.resume

typealias JobFunction = suspend JobScope.() -> Unit
typealias JobUpdateLister = Job.(Double, Long) -> Unit

data class TickJobtimeOptions(var jobTime: Int, var tickInterval: Int)

interface JobDispatcher {
    /**
     * Submit a [task] that should be run synchronously, but limited such that it does not stall the server
     */
    fun dispatch(task: JobFunction): Job

    /**
     * Get a list of all jobs
     */
    val jobs: List<Job>

    /**
     * Attempts to complete any remaining tasks immediately, without suspension.
     */
    fun completeAllTasks()
}

interface JobAndScopeMembersUnion {
    /**
     * The time that elapsed since this job was dispatched, in milliseconds
     */
    val elapsedTime: Long

    /**
     * A value indicating the progress of this job, in the range 0.0 <= progress <= 1.0
     * with no guarantees to its accuracy.
     */
    val progress: Double
}

interface Job : JobAndScopeMembersUnion {
    /**
     * The coroutine associated with this job
     */
    val job: CoroutineJob

    /**
     * true if this job has completed
     */
    val isComplete: Boolean

    /**
     * If an exception was thrown during the execution of this task,
     * returns that exception. Returns null otherwise.
     */
    val completionException: Throwable?

    /**
     * Calls the given [block] whenever the progress of this job is updated,
     * if [minInterval] milliseconds expired since the last call.
     * The first call occurs after at least [minDelay] milliseconds in a likewise manner.
     * Repeated invocations of this method result in an [IllegalStateException]
     *
     * if [asCompletionListener] is true, [onCompleted] is called with the same [block]
     */
    fun onProgressUpdate(minDelay: Int, minInterval: Int, asCompletionListener: Boolean = true, block: JobUpdateLister): Job

    /**
     * Calls the given [block] when this job completes, with the progress value 1.0.
     * Multiple listeners may be registered to this function.
     */
    fun onCompleted(block: JobUpdateLister): Job

    /**
     * Await completion of this job
     */
    suspend fun awaitCompletion()
}

interface JobScope : JobAndScopeMembersUnion {
    /**
     * A task should call this frequently during its execution, such that the timer can suspend it when necessary.
     */
    suspend fun markSuspensionPoint()

    /**
     * A task should call this method to indicate its progress
     */
    fun setProgress(progress: Double)

    /**
     * Indicate that this job is complete
     */
    fun markComplete() = setProgress(1.0)

    /**
     * Get a [JobScope] that is responsible for [portion] part of the progress
     * If [portion] is negative, the remaining progress is used
     */
    fun delegateProgress(portion: Double = -1.0): JobScope
}

inline fun <T> JobScope.delegateWork(portion: Double = -1.0, block: JobScope.() -> T): T {
    delegateProgress(portion).apply {
        val result = block()
        markComplete()
        return result
    }
}

interface JobInternal : Job, JobScope {
    /**
     * Start or resumes the execution of this job
     * and returns true if the job completed
     *
     * [worktime] is the maximum amount of time, in milliseconds,
     * that this job may run for until suspension.
     *
     * If [worktime] is not positive, the job will complete
     * without suspension and this method will always return true.
     */
    fun resume(worktime: Long): Boolean
}

/**
 * An object that controls one or more jobs, ensuring that they don't stall the server too much.
 * There is a configurable maxiumum amount of milliseconds that can be allocated to all jobs together in each server tick
 * This object attempts to split that maximum amount of milliseconds equally between all jobs
 */
class BukkitJobDispatcher(private val plugin: ParcelsPlugin, var options: TickJobtimeOptions) : JobDispatcher {
    // The currently registered bukkit scheduler task
    private var bukkitTask: BukkitTask? = null
    // The jobs.
    private val _jobs = LinkedList<JobInternal>()
    override val jobs: List<Job> = _jobs

    override fun dispatch(task: JobFunction): Job {
        val job: JobInternal = JobImpl(plugin, task)

        if (bukkitTask == null) {
            val completed = job.resume(options.jobTime.toLong())
            if (completed) return job
            bukkitTask = plugin.scheduleRepeating(0, options.tickInterval) { tickCoroutineJobs() }
        }
        _jobs.addFirst(job)
        return job
    }

    private fun tickCoroutineJobs() {
        val jobs = _jobs
        if (jobs.isEmpty()) return
        val tickStartTime = System.currentTimeMillis()

        val iterator = jobs.listIterator(index = 0)
        while (iterator.hasNext()) {
            val time = System.currentTimeMillis()
            val timeElapsed = time - tickStartTime
            val timeLeft = options.jobTime - timeElapsed
            if (timeLeft <= 0) return

            val count = jobs.size - iterator.nextIndex()
            val timePerJob = (timeLeft + count - 1) / count
            val job = iterator.next()
            val completed = job.resume(timePerJob)
            if (completed) {
                iterator.remove()
            }
        }

        if (jobs.isEmpty()) {
            bukkitTask?.cancel()
            bukkitTask = null
        }
    }

    override fun completeAllTasks() {
        _jobs.forEach {
            it.resume(-1)
        }
        _jobs.clear()
        bukkitTask?.cancel()
        bukkitTask = null
    }

}

private class JobImpl(scope: CoroutineScope, task: JobFunction) : JobInternal {
    override val job: CoroutineJob = scope.launch(start = LAZY) { task() }

    private var continuation: Continuation<Unit>? = null
    private var nextSuspensionTime: Long = 0L
    private var completeForcefully = false
    private var isStarted = false

    override val elapsedTime
        get() =
            if (job.isCompleted) startTimeOrElapsedTime
            else currentTimeMillis() - startTimeOrElapsedTime

    override val isComplete get() = job.isCompleted

    private var _progress = 0.0
    override val progress get() = _progress
    override var completionException: Throwable? = null; private set

    private var startTimeOrElapsedTime: Long = 0L // startTime before completed, elapsed time otherwise
    private var onProgressUpdate: JobUpdateLister? = null
    private var progressUpdateInterval: Int = 0
    private var lastUpdateTime: Long = 0L
    private var onCompleted: JobUpdateLister? = null

    init {
        job.invokeOnCompletion { exception ->
            // report any error that occurred
            completionException = exception?.also {
                if (it !is CancellationException)
                    logger.error("JobFunction generated an exception", it)
            }

            // convert to elapsed time here
            startTimeOrElapsedTime = System.currentTimeMillis() - startTimeOrElapsedTime
            onCompleted?.let { it(1.0, elapsedTime) }

            onCompleted = null
            onProgressUpdate = { prog, el -> }
        }
    }

    override fun onProgressUpdate(minDelay: Int, minInterval: Int, asCompletionListener: Boolean, block: JobUpdateLister): Job {
        onProgressUpdate?.let { throw IllegalStateException() }
        if (asCompletionListener) onCompleted(block)
        if (isComplete) return this
        onProgressUpdate = block
        progressUpdateInterval = minInterval
        lastUpdateTime = System.currentTimeMillis() + minDelay - minInterval

        return this
    }

    override fun onCompleted(block: JobUpdateLister): Job {
        if (isComplete) {
            block(1.0, startTimeOrElapsedTime)
            return this
        }

        val cur = onCompleted
        onCompleted = if (cur == null) {
            block
        } else {
            fun Job.(prog: Double, el: Long) {
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
        this._progress = progress
        val onProgressUpdate = onProgressUpdate ?: return
        val time = System.currentTimeMillis()
        if (time > lastUpdateTime + progressUpdateInterval) {
            onProgressUpdate(progress, elapsedTime)
            lastUpdateTime = time
        }
    }

    override fun resume(worktime: Long): Boolean {
        if (isComplete) return true

        if (worktime > 0) {
            nextSuspensionTime = currentTimeMillis() + worktime
        } else {
            completeForcefully = true
        }

        if (isStarted) {
            continuation?.let {
                continuation = null
                it.resume(Unit)
                return continuation == null
            }
            return true
        }

        isStarted = true
        startTimeOrElapsedTime = System.currentTimeMillis()
        job.start()

        return continuation == null
    }

    override suspend fun awaitCompletion() {
        job.join()
    }

    private fun delegateProgress(curPortion: Double, portion: Double): JobScope =
        DelegateScope(progress, curPortion * (if (portion < 0) 1.0 - progress else portion).clampMin(0.0))

    override fun delegateProgress(portion: Double): JobScope = delegateProgress(1.0, portion)

    private inner class DelegateScope(val progressStart: Double, val portion: Double) : JobScope {
        override val elapsedTime: Long
            get() = this@JobImpl.elapsedTime

        override suspend fun markSuspensionPoint() =
            this@JobImpl.markSuspensionPoint()

        override val progress: Double
            get() = (this@JobImpl.progress - progressStart) / portion

        override fun setProgress(progress: Double) =
            this@JobImpl.setProgress(progressStart + progress * portion)

        override fun delegateProgress(portion: Double): JobScope =
            this@JobImpl.delegateProgress(this.portion, portion)
    }
}
