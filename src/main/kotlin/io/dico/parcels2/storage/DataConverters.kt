package io.dico.parcels2.storage

import java.lang.IllegalArgumentException
import java.nio.ByteBuffer
import java.util.UUID

/* For putting it into the database */
fun UUID.toByteArray(): ByteArray =
    ByteBuffer.allocate(16).apply {
        putLong(mostSignificantBits)
        putLong(leastSignificantBits)
    }.array()

/* For getting it out of the database */
fun ByteArray.toUUID(): UUID =
    ByteBuffer.wrap(this).run {
        val mostSignificantBits = getLong()
        val leastSignificantBits = getLong()
        UUID(mostSignificantBits, leastSignificantBits)
    }

/* For putting it into the database */
fun IntArray.toByteArray(): ByteArray =
    ByteBuffer.allocate(size * Int.SIZE_BYTES).also { buf ->
        buf.asIntBuffer().put(this)
    }.array()

/* For getting it out of the database */
fun ByteArray.toIntArray(): IntArray {
    if (this.size % Int.SIZE_BYTES != 0)
        throw IllegalArgumentException("Size must be divisible by ${Int.SIZE_BYTES}")

    return ByteBuffer.wrap(this).run {
        IntArray(remaining() / 4).also { array ->
            asIntBuffer().get(array)
        }
    }
}