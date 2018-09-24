package io.dico.parcels2.util.ext

import io.dico.parcels2.logger
import java.io.File

fun File.tryCreate(): Boolean {
    if (exists()) {
        return !isDirectory
    }
    val parent = parentFile
    if (parent == null || !(parent.exists() || parent.mkdirs()) || !createNewFile()) {
        logger.warn("Failed to create file $canonicalPath")
        return false
    }
    return true
}

inline fun Boolean.alsoIfTrue(block: () -> Unit): Boolean = also { if (it) block() }
inline fun Boolean.alsoIfFalse(block: () -> Unit): Boolean = also { if (!it) block() }

inline fun <R> Any.synchronized(block: () -> R): R = synchronized(this, block)

//inline fun <T> T?.isNullOr(condition: T.() -> Boolean): Boolean = this == null || condition()
//inline fun <T> T?.isPresentAnd(condition: T.() -> Boolean): Boolean = this != null && condition()
inline fun <T> T?.ifNullRun(block: () -> Unit): T? {
    if (this == null) block()
    return this
}

inline fun <T, U> MutableMap<T, U>.editLoop(block: EditLoopScope<T, U>.(T, U) -> Unit) {
    return EditLoopScope(this).doEditLoop(block)
}

inline fun <T, U> MutableMap<T, U>.editLoop(block: EditLoopScope<T, U>.() -> Unit) {
    return EditLoopScope(this).doEditLoop(block)
}

class EditLoopScope<T, U>(val _map: MutableMap<T, U>) {
    private var iterator: MutableIterator<MutableMap.MutableEntry<T, U>>? = null
    lateinit var _entry: MutableMap.MutableEntry<T, U>

    inline val key get() = _entry.key
    inline var value
        get() = _entry.value
        set(target) = run { _entry.setValue(target) }

    inline fun doEditLoop(block: EditLoopScope<T, U>.() -> Unit) {
        val it = _initIterator()
        while (it.hasNext()) {
            _entry = it.next()
            block()
        }
    }

    inline fun doEditLoop(block: EditLoopScope<T, U>.(T, U) -> Unit) {
        val it = _initIterator()
        while (it.hasNext()) {
            val entry = it.next().also { _entry = it }
            block(entry.key, entry.value)
        }
    }

    fun remove() {
        iterator!!.remove()
    }

    fun _initIterator(): MutableIterator<MutableMap.MutableEntry<T, U>> {
        iterator?.let { throw IllegalStateException() }
        return _map.entries.iterator().also { iterator = it }
    }

}
