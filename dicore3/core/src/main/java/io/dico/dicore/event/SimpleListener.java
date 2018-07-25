package io.dico.dicore.event;

public interface SimpleListener<T> {
    
    void accept(T event);
    
}
