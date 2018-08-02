package io.dico.parcels2.storage.migration

import io.dico.parcels2.storage.Storage

interface Migration {
    fun migrateTo(storage: Storage)
}

