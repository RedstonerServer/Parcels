package io.dico.parcels2.storage

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.statements.UpdateStatement
import org.jetbrains.exposed.sql.transactions.TransactionManager

/*
 * insertOrUpdate from https://github.com/JetBrains/Exposed/issues/167#issuecomment-403837917
 */
inline fun <T : Table> T.insertOrUpdate(vararg onDuplicateUpdateKeys: Column<*>, body: T.(InsertStatement<Number>) -> Unit) =
    InsertOrUpdate<Number>(onDuplicateUpdateKeys, this).apply {
        body(this)
        execute(TransactionManager.current())
    }

class InsertOrUpdate<Key : Any>(
    private val onDuplicateUpdateKeys: Array<out Column<*>>,
    table: Table,
    isIgnore: Boolean = false
) : InsertStatement<Key>(table, isIgnore) {
    override fun prepareSQL(transaction: Transaction): String {
        val onUpdateSQL = if (onDuplicateUpdateKeys.isNotEmpty()) {
            " ON DUPLICATE KEY UPDATE " + onDuplicateUpdateKeys.joinToString { "${transaction.identity(it)}=VALUES(${transaction.identity(it)})" }
        } else ""
        return super.prepareSQL(transaction) + onUpdateSQL
    }
}


class UpsertStatement<Key : Any>(table: Table, conflictColumn: Column<*>? = null, conflictIndex: Index? = null)
    : InsertStatement<Key>(table, false) {
    val indexName: String
    val indexColumns: List<Column<*>>

    private fun getUpdateStatement(): UpdateStatement {
        val map: Map<Column<Any?>, Any?> = values.castUnchecked()
        val statement = updateBody(table, UpdateStatement(table, null, combineAsConjunctions(indexColumns.castUnchecked(), map))) {
            map.forEach { col, value -> if (col !in indexColumns)
                it[col] = value
            }
        }
        return statement
    }

    init {
        when {
            conflictIndex != null -> {
                indexName = conflictIndex.indexName
                indexColumns = conflictIndex.columns
            }
            conflictColumn != null -> {
                indexName = conflictColumn.name
                indexColumns = listOf(conflictColumn)
            }
            else -> throw IllegalArgumentException()
        }
    }

    override fun prepareSQL(transaction: Transaction): String {
        val insertSQL = super.prepareSQL(transaction)
        val updateStatement = getUpdateStatement()
        val updateSQL = updateStatement.prepareSQL(transaction)
        super.arguments = listOf(super.arguments!!.first(), updateStatement.firstDataSet)

        return buildString {
            append(insertSQL)
            append(" ON CONFLICT(")
            append(indexName)
            append(") DO UPDATE ")
            append(updateSQL)
        }.also { println(it) }
    }

    private companion object {

        inline fun <T : Table> updateBody(table: T, updateStatement: UpdateStatement,
                                          body: T.(UpdateStatement) -> Unit): UpdateStatement {
            table.body(updateStatement)
            return updateStatement
        }

        @Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
        inline fun <T> Any.castUnchecked() = this as T

        private val absent = Any() // marker object
        fun combineAsConjunctions(columns: Iterable<Column<Any?>>, map: Map<Column<Any?>, Any?>): Op<Boolean>? {
            return with(SqlExpressionBuilder) {
                columns.fold<Column<Any?>, Op<Boolean>?>(null) { op, col ->
                    val arg = map.getOrDefault(col, absent)
                    if (arg === absent) return@fold op
                    op?.let { it and (col eq arg) } ?: col eq arg
                }
            }
        }

    }
}

inline fun <T : Table> T.upsert(conflictColumn: Column<*>? = null, conflictIndex: Index? = null, body: T.(UpsertStatement<Number>) -> Unit) =
    UpsertStatement<Number>(this, conflictColumn, conflictIndex).apply {
        body(this)
        execute(TransactionManager.current())
    }

fun Table.indexR(customIndexName: String? = null, isUnique: Boolean = false, vararg columns: Column<*>): Index {
    val index = Index(columns.toList(), isUnique, customIndexName)
    indices.add(index)
    return index
}

fun Table.uniqueIndexR(customIndexName: String? = null, vararg columns: Column<*>): Index = indexR(customIndexName, true, *columns)
