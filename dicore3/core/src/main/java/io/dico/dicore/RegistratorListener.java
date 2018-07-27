package io.dico.dicore;

import org.bukkit.event.Event;

import java.util.function.Consumer;

public interface RegistratorListener<T extends Event> extends Consumer<T> {
    @Override
    void accept(T event);
}
