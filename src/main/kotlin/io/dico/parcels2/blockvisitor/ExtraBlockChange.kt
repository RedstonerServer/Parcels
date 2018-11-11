package io.dico.parcels2.blockvisitor

import org.bukkit.block.Block
import org.bukkit.block.BlockState
import org.bukkit.block.Sign
import kotlin.reflect.KClass

interface ExtraBlockChange {
    fun update(block: Block)
}

abstract class BlockStateChange<T : BlockState> : ExtraBlockChange {
    abstract val stateClass: KClass<T>

    abstract fun update(state: T)

    override fun update(block: Block) {
        val state = block.state
        if (stateClass.isInstance(state)) {
            @Suppress("UNCHECKED_CAST")
            update(state as T)
        }
    }
}

class SignStateChange(state: Sign) : BlockStateChange<Sign>() {
    val lines = state.lines

    override val stateClass: KClass<Sign>
        get() = Sign::class

    override fun update(state: Sign) {
        for (i in lines.indices) {
            val line = lines[i]
            state.setLine(i, line)
        }
    }
}
