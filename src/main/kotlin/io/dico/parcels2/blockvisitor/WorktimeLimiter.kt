package io.dico.parcels2.blockvisitor

import io.dico.parcels2.Options
import kotlinx.coroutines.experimental.CoroutineStart
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.asCoroutineDispatcher
import kotlinx.coroutines.experimental.launch
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitTask
import java.util.*
import java.util.concurrent.Executor
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.ContinuationInterceptor
import kotlin.coroutines.experimental.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.experimental.intrinsics.suspendCoroutineUninterceptedOrReturn

interface WorktimeLimiter {
    /**
     * Submit a task that should be run synchronously, but limited such that it does not stall the server
     * a bunch
     */
    fun submit(job: TimeLimitedTask): JobData

    /**
     * A task should call this frequently during its execution, such that the timer can suspend it when necessary.
     */
    suspend fun markSuspensionPoint()

    /**
     * A task should call this method to indicate its progress
     */
    fun setProgress(progress: Double)
}

typealias TimeLimitedTask = suspend WorktimeLimiter.() -> Unit

interface JobData {
    val job: Job?
    val isComplete: Boolean
    val progress: Double?

    /**
     * Calls the given [block] whenever the progress is updated,
     * if [minInterval] milliseconds expired since the last call.
     *
     * The first call occurs after at least [minDelay] milliseconds in a likewise manner.
     * Repeated invocations of this method result in an [IllegalStateException]
     */
    fun onProgressUpdate(minDelay: Int, minInterval: Int, block: JobUpdateListener): JobData
    val isUpdateBlockPresent: Boolean

    /**
     * Calls the given [block] when this job completes.
     */
    fun onCompleted(block: JobUpdateListener): JobData
}

typealias JobUpdateListener = JobData.(Double) -> Unit

class JobDataImpl(val task: TimeLimitedTask) : JobData {
    override var job: Job? = null
        set(value) {
            field?.let { throw IllegalStateException() }
            field = value!!
            value.invokeOnCompletion { onCompletedBlock?.invoke(this, 1.0) }
        }

    var next: Continuation<Unit>? = null

    override var progress: Double? = null
        set(value) {
            field = value
            doProgressUpdate()
        }

    private fun doProgressUpdate() {
        val progressUpdate = progressUpdateBlock ?: return
        val time = System.currentTimeMillis()
        if (time > lastUpdateTime + progressUpdateInterval) {
            progressUpdate(progress!!)
            lastUpdateTime = time
        }
    }

    override val isUpdateBlockPresent get() = progressUpdateBlock != null
    private var progressUpdateBlock: JobUpdateListener? = null
    private var progressUpdateInterval: Int = 0
    private var lastUpdateTime: Long = 0L
    override fun onProgressUpdate(minDelay: Int, minInterval: Int, block: JobUpdateListener): JobDataImpl {
        progressUpdateBlock?.let { throw IllegalStateException() }
        progressUpdateBlock = block
        progressUpdateInterval = minInterval
        lastUpdateTime = System.currentTimeMillis() + minDelay - minInterval
        return this
    }

    override val isComplete get() = job?.isCompleted == true
    private var onCompletedBlock: JobUpdateListener? = null
    override fun onCompleted(block: JobUpdateListener): JobDataImpl {
        onCompletedBlock?.let { throw IllegalStateException() }
        onCompletedBlock = block
        return this
    }

}

/**
 * An object that controls one or more jobs, ensuring that they don't stall the server too much.
 * The amount of milliseconds that can accumulate each server tick is configurable
 */
class TickWorktimeLimiter(private val plugin: Plugin, private val optionsRoot: Options) : WorktimeLimiter {
    // Coroutine dispatcher for jobs
    private val dispatcher = Executor(Runnable::run).asCoroutineDispatcher()
    // union of Continuation<Unit> and suspend WorktimeLimited.() -> Unit
    private var jobs = LinkedList<JobDataImpl>()
    // The currently registered bukkit scheduler task
    private var task: BukkitTask? = null
    // The data associated with the task that is currently being executed
    private var curJobData: JobDataImpl? = null
    // Used to keep track of when the current task should end
    private var curJobEndTime = 0L
    // Tick work time options
    private inline val options get() = optionsRoot.tickWorktime

    override fun submit(job: TimeLimitedTask): JobData {
        val jobData = JobDataImpl(job)
        jobs.addFirst(jobData)
        if (task == null) task = plugin.server.scheduler.runTaskTimer(plugin, ::tickJobs, 0, options.tickInterval.toLong())
        return jobData
    }

    override suspend fun markSuspensionPoint() {
        if (System.currentTimeMillis() >= curJobEndTime)
            suspendCoroutineUninterceptedOrReturn(::scheduleContinuation)
    }

    override fun setProgress(progress: Double) {
        curJobData!!.progress = progress
    }

    private fun tickJobs() {
        if (jobs.isEmpty()) return
        val tickStartTime = System.currentTimeMillis()
        val jobs = this.jobs; this.jobs = LinkedList()

        var count = jobs.size

        while (!jobs.isEmpty()) {
            val job = jobs.poll()
            val time = System.currentTimeMillis()
            val timeElapsed = time - tickStartTime
            val timeLeft = options.workTime - timeElapsed

            if (timeLeft <= 0) {
                this.jobs.addAll(0, jobs)
                return
            }

            val timePerJob = (timeLeft + count - 1) / count
            tickJob(job, time + timePerJob)
            count--
        }

        if (jobs.isEmpty() && this.jobs.isEmpty()) {
            task?.cancel()
            task = null
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun tickJob(job: JobDataImpl, endTime: Long) {
        curJobData = job
        curJobEndTime = endTime
        try {
            val next = job.next
            if (next == null) startJob(job)
            else next.resume(Unit)
        }
        finally {
            curJobData = null
            curJobEndTime = 0L
        }
    }

    private fun startJob(job: JobDataImpl) {
        job.job = launch(context = dispatcher, start = CoroutineStart.UNDISPATCHED) { job.task(this@TickWorktimeLimiter) }
    }

    private fun scheduleContinuation(continuation: Continuation<Unit>): Any? {
        curJobData!!.next = continuation
        jobs.addLast(curJobData)
        return COROUTINE_SUSPENDED
    }

}

data class TickWorktimeOptions(var workTime: Int, var tickInterval: Int)


/**
 * While the implementation of [kotlin.coroutines.experimental.intrinsics.intercepted] is intrinsic, it should look something like this
 * We don't care for intercepting the coroutine as we want it to resume immediately when we call resume().
 * Thus, above, we use an unintercepted suspension. It's not necessary as the dispatcher (or interceptor) also calls it synchronously, but whatever.
 */
private fun <T> Continuation<T>.interceptedImpl(): Continuation<T> {
    return context[ContinuationInterceptor]?.interceptContinuation(this) ?: this
}