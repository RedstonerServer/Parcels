package io.dico.parcels2.command

import io.dico.dicore.command.CommandException
import io.dico.dicore.command.CommandResult
import io.dico.dicore.command.EMessageType
import io.dico.dicore.command.ExecutionContext
import io.dico.dicore.command.chat.IChatController
import io.dico.parcels2.ParcelsPlugin
import io.dico.parcels2.logger
import kotlinx.coroutines.experimental.*
import org.bukkit.command.CommandSender
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.suspendCoroutine

/*
 * Interface to implicitly access plugin by creating extension functions for it
 */
interface HasPlugin {
    val plugin: ParcelsPlugin
}

class CommandAsyncScope {

    suspend fun <T> HasPlugin.awaitSynchronousTask(delay: Int = 0, task: () -> T): T {
        return suspendCoroutine { cont: Continuation<T> ->
            plugin.server.scheduler.runTaskLater(plugin, l@{
                val result = try {
                    task()
                } catch (ex: CommandException) {
                    cont.resumeWithException(ex)
                    return@l
                } catch (ex: Throwable) {
                    cont.context.cancel(ex)
                    return@l
                }
                cont.resume(result)
            }, delay.toLong())
        }
    }

    fun <T> HasPlugin.synchronousTask(delay: Int = 0, task: () -> T): Deferred<T> {
        return async { awaitSynchronousTask(delay, task) }
    }

}

fun <T : Any?> HasPlugin.delegateCommandAsync(context: ExecutionContext,
                                              block: suspend CommandAsyncScope.() -> T) {

    val job: Deferred<Any?> = async(/*context = plugin.storage.asyncDispatcher, */start = CoroutineStart.ATOMIC) {
        CommandAsyncScope().block()
    }

    fun Job.invokeOnCompletionSynchronously(block: (Throwable?) -> Unit) = invokeOnCompletion {
        plugin.server.scheduler.runTask(plugin) { block(it) }
    }

    job.invokeOnCompletionSynchronously l@{ exception: Throwable? ->
        exception?.let {
            context.address.chatController.handleCoroutineException(context.sender, context, it)
            return@l
        }

        val result = job.getCompleted()
        val message = when (result) {
            is String -> result
            is CommandResult -> result.message
            else -> null
        }

        context.address.chatController.sendMessage(context.sender, EMessageType.RESULT, message)
    }

}

fun IChatController.handleCoroutineException(sender: CommandSender, context: ExecutionContext, exception: Throwable) {
    if (exception is CancellationException) {
        sendMessage(sender, EMessageType.EXCEPTION, "The command was cancelled unexpectedly (see console)")
        logger.warn("An asynchronously dispatched command was cancelled unexpectedly", exception)
    } else {
        handleException(sender, context, exception)
    }
}
