package io.dico.parcels2.blockvisitor

import io.dico.dicore.task.IteratorTask

abstract class BlockVisitor<T>(iterator: Iterator<T>?) : IteratorTask<T>(iterator)
