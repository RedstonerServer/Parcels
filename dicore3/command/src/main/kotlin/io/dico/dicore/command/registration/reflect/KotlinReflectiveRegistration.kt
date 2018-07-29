package io.dico.dicore.command.registration.reflect

import io.dico.dicore.command.CommandException
import io.dico.dicore.command.EMessageType
import io.dico.dicore.command.ExecutionContext
import io.dico.dicore.command.ICommandReceiver
import kotlinx.coroutines.experimental.CoroutineStart.UNDISPATCHED
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.asCoroutineDispatcher
import kotlinx.coroutines.experimental.async
import java.lang.reflect.Method
import java.util.*
import java.util.concurrent.CancellationException
import java.util.concurrent.Executor
import kotlin.coroutines.experimental.intrinsics.suspendCoroutineOrReturn
import kotlin.reflect.jvm.kotlinFunction

fun isSuspendFunction(method: Method): Boolean {
    val func = method.kotlinFunction ?: return false
    return func.isSuspend
}

fun callAsCoroutine(command: ReflectiveCommand,
                    factory: ICommandReceiver.Factory,
                    context: ExecutionContext,
                    args: Array<Any?>): String? {
    val dispatcher = Executor { task -> factory.plugin.server.scheduler.runTask(factory.plugin, task) }.asCoroutineDispatcher()

    // UNDISPATCHED causes the handler to run until the first suspension point on the current thread,
    // meaning command handlers that don't have suspension points will run completely synchronously.
    // Tasks that take time to compute should suspend the coroutine and resume on another thread.
    val job = async(context = dispatcher, start = UNDISPATCHED) { command.method.invokeSuspend(command.instance, args) }

    if (job.isCompleted) {
        return job.getResult()
    }

    job.invokeOnCompletion {
        val cc = context.address.chatController
        try {
            val result = job.getResult()
            cc.sendMessage(context.sender, EMessageType.RESULT, result)
        } catch (ex: Throwable) {
            cc.handleException(context.sender, context, ex)
        }
    }

    return null
}

private suspend fun Method.invokeSuspend(instance: Any?, args: Array<Any?>): Any? {
    return suspendCoroutineOrReturn { cont ->
        invoke(instance, *args, cont)
    }
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

fun getNonPrimitiveClass(clazz: Class<*>): Class<*>? {
    return if (clazz.isPrimitive)
        clazz.kotlin.javaObjectType
    else
        null
}