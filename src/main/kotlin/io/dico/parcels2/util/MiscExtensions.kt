package io.dico.parcels2.util

import io.dico.parcels2.logger
import java.io.File

fun File.tryCreate(): Boolean {
    val parent = parentFile
    if (parent == null || !(parent.exists() || parent.mkdirs()) || !createNewFile()) {
        logger.warn("Failed to create file ${canonicalPath}")
        return false
    }
    return true
}

inline fun <R> Any.synchronized(block: () -> R): R {
    return synchronized(this, block)
}
