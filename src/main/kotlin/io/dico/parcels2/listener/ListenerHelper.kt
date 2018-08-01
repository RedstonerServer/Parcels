package io.dico.parcels2.listener

import io.dico.dicore.RegistratorListener
import io.dico.parcels2.ParcelsPlugin
import org.bukkit.event.Event

interface HasPlugin {
    val plugin: ParcelsPlugin
}

inline fun <reified T: Event> HasPlugin.listener(crossinline block: suspend (T) -> Unit) = RegistratorListener<T> { event ->









}


