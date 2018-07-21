package io.dico.parcels2.storage

import kotlinx.coroutines.experimental.CoroutineDispatcher
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.CoroutineStart
import kotlinx.coroutines.experimental.asCoroutineDispatcher
import kotlinx.coroutines.experimental.channels.ProducerJob
import kotlinx.coroutines.experimental.channels.produce
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/*
interface Storage {

    val name: String

    val syncDispatcher: CoroutineDispatcher

    val asyncDispatcher: CoroutineDispatcher

    fun init(): CompletableFuture<Unit>

    fun shutdown(): CompletableFuture<Unit>

    fun readPlotData(plotFor: Plot): CompletableFuture<PlotData?>

    fun readPlotData(plotsFor: Sequence<Plot>, channelCapacity: Int): ProducerJob<Pair<Plot, PlotData?>>

    fun getOwnedPlots(user: PlotOwner): CompletableFuture<List<SerializablePlot>>

    fun setPlotOwner(plotFor: Plot, owner: PlotOwner?): CompletableFuture<Unit>

    fun setPlotPlayerState(plotFor: Plot, player: UUID, state: Boolean?): CompletableFuture<Unit>

    fun setPlotAllowsInteractInventory(plot: Plot, value: Boolean): CompletableFuture<Unit>

    fun setPlotAllowsInteractInputs(plot: Plot, value: Boolean): CompletableFuture<Unit>

}

class StorageWithBacking internal constructor(val backing: Backing) : Storage {
    override val name get() = backing.name
    override val syncDispatcher = Executor { it.run() }.asCoroutineDispatcher()
    override val asyncDispatcher = Executors.newFixedThreadPool(4) { Thread(it, "AbstractStorageThread") }.asCoroutineDispatcher()

    private fun <T> future(block: suspend CoroutineScope.() -> T) = kotlinx.coroutines.experimental.future.future(asyncDispatcher, CoroutineStart.ATOMIC, block)

    override fun init(): CompletableFuture<Unit> = future { backing.init() }

    override fun shutdown(): CompletableFuture<Unit> = future { backing.shutdown() }

    override fun readPlotData(plotFor: Plot) = future { backing.readPlotData(plotFor) }

    override fun readPlotData(plotsFor: Sequence<Plot>, channelCapacity: Int) =
            produce<Pair<Plot, PlotData?>>(asyncDispatcher, capacity = channelCapacity) { backing.producePlotData(this, plotsFor) }

    override fun getOwnedPlots(user: PlotOwner) = future { backing.getOwnedPlots(user) }

    override fun setPlotOwner(plotFor: Plot, owner: PlotOwner?) = future { backing.setPlotOwner(plotFor, owner) }

    override fun setPlotPlayerState(plotFor: Plot, player: UUID, state: Boolean?) = future { backing.setPlotPlayerState(plotFor, player, state) }

    override fun setPlotAllowsInteractInventory(plot: Plot, value: Boolean) = future { backing.setPlotAllowsInteractInventory(plot, value) }

    override fun setPlotAllowsInteractInputs(plot: Plot, value: Boolean) = future { backing.setPlotAllowsInteractInputs(plot, value) }
}
        */