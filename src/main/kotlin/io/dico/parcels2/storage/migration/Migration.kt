package io.dico.parcels2.storage.migration

import io.dico.parcels2.storage.Storage
import kotlinx.coroutines.experimental.Job

interface Migration {
    fun migrateTo(storage: Storage): Job
}

