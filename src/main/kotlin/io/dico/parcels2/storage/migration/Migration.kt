package io.dico.parcels2.storage.migration

import io.dico.parcels2.storage.Storage
import kotlinx.coroutines.Job

interface Migration {
    fun migrateTo(storage: Storage): Job
}

