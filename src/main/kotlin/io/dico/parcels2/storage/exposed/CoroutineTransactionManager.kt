package io.dico.parcels2.storage.exposed

import kotlinx.coroutines.experimental.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.StatementContext
import org.jetbrains.exposed.sql.statements.StatementInterceptor
import org.jetbrains.exposed.sql.statements.expandArgs
import org.jetbrains.exposed.sql.transactions.*
import org.slf4j.LoggerFactory
import java.sql.Connection
import kotlin.coroutines.experimental.CoroutineContext

fun <T> ctransaction(db: Database? = null, statement: suspend Transaction.() -> T): T {
    return ctransaction(TransactionManager.manager.defaultIsolationLevel, 3, db, statement)
}

fun <T> ctransaction(transactionIsolation: Int, repetitionAttempts: Int, db: Database? = null, statement: suspend Transaction.() -> T): T {
    return transaction(transactionIsolation, repetitionAttempts, db) {
        if (this !is CoroutineTransaction) throw IllegalStateException("ctransaction requires CoroutineTransactionManager.")

        val job = async(context = manager.context, start = CoroutineStart.UNDISPATCHED) {
            this@transaction.statement()
        }

        if (job.isActive) {
            runBlocking(context = Unconfined) {
                job.join()
            }
        }

        job.getCompleted()
    }
}

class CoroutineTransactionManager(private val db: Database,
                                  dispatcher: CoroutineDispatcher,
                                  override var defaultIsolationLevel: Int = DEFAULT_ISOLATION_LEVEL) : TransactionManager {
    val context: CoroutineDispatcher = TransactionCoroutineDispatcher(dispatcher)
    private val transaction = ThreadLocal<CoroutineTransaction?>()

    override fun currentOrNull(): Transaction? {


        return transaction.get()
        ?: null
    }

    override fun newTransaction(isolation: Int): Transaction {
        return CoroutineTransaction(this, CoroutineTransactionInterface(db, isolation, transaction)).also { transaction.set(it) }
    }

    private inner class TransactionCoroutineDispatcher(val delegate: CoroutineDispatcher) : CoroutineDispatcher() {

        // When the thread changes, move the transaction to the new thread
        override fun dispatch(context: CoroutineContext, block: Runnable) {
            val existing = transaction.get()

            val newContext: CoroutineContext
            if (existing != null) {
                transaction.set(null)
                newContext = context // + existing
            } else {
                newContext = context
            }

            delegate.dispatch(newContext, Runnable {
                if (existing != null) {
                    transaction.set(existing)
                }

                block.run()
            })
        }

    }

}

private class CoroutineTransaction(val manager: CoroutineTransactionManager,
                                   itf: CoroutineTransactionInterface) : Transaction(itf), CoroutineContext.Element {
    companion object Key : CoroutineContext.Key<CoroutineTransaction>

    override val key: CoroutineContext.Key<CoroutineTransaction> = Key
}

private class CoroutineTransactionInterface(override val db: Database, isolation: Int, val threadLocal: ThreadLocal<CoroutineTransaction?>) : TransactionInterface {
    private val connectionLazy = lazy(LazyThreadSafetyMode.NONE) {
        db.connector().apply {
            autoCommit = false
            transactionIsolation = isolation
        }
    }
    override val connection: Connection
        get() = connectionLazy.value

    override val outerTransaction: CoroutineTransaction? = threadLocal.get()

    override fun commit() {
        if (connectionLazy.isInitialized())
            connection.commit()
    }

    override fun rollback() {
        if (connectionLazy.isInitialized() && !connection.isClosed) {
            connection.rollback()
        }
    }

    override fun close() {
        try {
            if (connectionLazy.isInitialized()) connection.close()
        } finally {
            threadLocal.set(outerTransaction)
        }
    }

}

