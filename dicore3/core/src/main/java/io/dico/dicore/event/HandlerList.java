package io.dico.dicore.event;

import org.bukkit.event.Cancellable;
import org.bukkit.event.EventPriority;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

public final class HandlerList<T> {
    private final List<Listener<T>> source = new ArrayList<>();
    private Listener<T>[] listeners = newArray(0);

    public void refresh() {
        source.sort(Comparator.comparingInt(l -> l.getPriority().ordinal()));
        listeners = source.toArray(newArray(source.size()));
    }

    @SuppressWarnings("unchecked")
    private static <T> Listener<T>[] newArray(int length) {
        return new Listener[length];
    }

    public void register(Listener<T> listener) {
        if (!source.contains(listener) && source.add(listener)) {
            refresh();
        }
    }
    
    public ListenerHandle getListenerHandle(Listener<T> listener) {
        return new ListenerHandle() {
            @Override
            public void register() {
                HandlerList.this.register(listener);
            }
    
            @Override
            public void unregister() {
                HandlerList.this.unregister(listener);
            }
        };
    }

    public void register(EventPriority priority, Consumer<T> listener) {
        register(new Listener<T>() {
            @Override
            public EventPriority getPriority() {
                return priority;
            }

            @Override
            public void accept(T event) {
                listener.accept(event);
            }
        });
    }

    public List<Listener<T>> getRegistrations() {
        return Collections.unmodifiableList(source);
    }

    public void unregister(Listener<T> listener) {
        if (source.remove(listener)) {
            refresh();
        }
    }

    public void callEvent(T event) {
        if (event instanceof Cancellable) {
            Cancellable c = (Cancellable) event;
            boolean cancelled = c.isCancelled();
            for (Listener<T> listener : listeners) {
                if (listener.listensToCancelledState(cancelled)) {
                    //EnchantsPlugin.getInstance().debug("Listener acceptance: " + listener.getClass().getSimpleName());
                    listener.accept(event);
                    cancelled = c.isCancelled();
                } /*else {
                    EnchantsPlugin.getInstance().debug("Listener does not listen to cancelled state of " + cancelled + ": " + listener.getClass().getSimpleName());
                }*/
            }
        } else {
            for (Listener<T> listener : listeners) {
                //EnchantsPlugin.getInstance().debug("Listener acceptance: " + listener.getClass().getSimpleName());
                listener.accept(event);
            }
        }
    }

}
