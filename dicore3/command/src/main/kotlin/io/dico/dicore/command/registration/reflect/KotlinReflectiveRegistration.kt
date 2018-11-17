package io.dico.dicore.command.registration.reflect

import io.dico.dicore.command.*
import kotlinx.coroutines.CoroutineStart.UNDISPATCHED
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import java.lang.reflect.Method
import java.util.concurrent.CancellationException
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.intrinsics.intercepted
import kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn
import kotlin.reflect.jvm.kotlinFunction

fun isSuspendFunction(method: Method): Boolean {
    val func = method.kotlinFunction ?: return false
    return func.isSuspend
}

@Throws(CommandException::class)
fun callCommandAsCoroutine(
    executionContext: ExecutionContext,
    coroutineContext: CoroutineContext,
    continuationIndex: Int,
    method: Method,
    instance: Any?,
    args: Array<Any?>
): String? {

    // UNDISPATCHED causes the handler to run until the first suspension point on the current thread,
    // meaning command handlers that don't have suspension points will run completely synchronously.
    // Tasks that take time to compute should suspend the coroutine and resume on another thread.
    val job = GlobalScope.async(context = coroutineContext, start = UNDISPATCHED) {
        suspendCoroutineUninterceptedOrReturn<Any?> { cont ->
            args[continuationIndex] = cont.intercepted()
            method.invoke(instance, *args)
        }
    }

    if (job.isCompleted) {
        return job.getResult()
    }

    job.invokeOnCompletion {
        val chatHandler = executionContext.address.chatHandler
        try {
            val result = job.getResult()
            chatHandler.sendMessage(executionContext.sender, EMessageType.RESULT, result)
        } catch (ex: Throwable) {
            chatHandler.handleException(executionContext.sender, executionContext, ex)
        }
    }

    return null
}

@Throws(CommandException::class)
private fun Deferred<Any?>.getResult(): String? {
    getCompletionExceptionOrNull()?.let { ex ->
        if (ex is CancellationException) {
            System.err.println("An asynchronously dispatched command was cancelled unexpectedly")
            ex.printStackTrace()
            throw CommandException("The command was cancelled unexpectedly (see console)")
        }
        if (ex is Exception) return ReflectiveCommand.getResult(null, ex)
        throw ex
    }
    return ReflectiveCommand.getResult(getCompleted(), null)
}
