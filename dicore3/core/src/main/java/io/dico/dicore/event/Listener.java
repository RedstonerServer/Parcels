package io.dico.dicore.event;

import org.bukkit.event.EventPriority;

public interface Listener<T> {
    
    default EventPriority getPriority() {
        return EventPriority.NORMAL;
    }
    
    default boolean listensToCancelledState(boolean cancelled) {
        return !cancelled;
    }
    
    void accept(T event);
    
}
