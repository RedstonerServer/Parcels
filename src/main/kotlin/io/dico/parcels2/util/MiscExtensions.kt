package io.dico.parcels2.util

import io.dico.parcels2.logger
import org.slf4j.Logger
import java.io.File
import java.io.PrintWriter

fun File.tryCreate(): Boolean {
    val parent = parentFile
    if (parent == null || !(parent.exists() || parent.mkdirs()) || !createNewFile()) {
        logger.warn("Failed to create file ${canonicalPath}")
        return false
    }
    return true
}
