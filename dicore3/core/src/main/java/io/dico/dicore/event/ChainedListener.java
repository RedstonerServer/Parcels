package io.dico.dicore.event;

import io.dico.dicore.InterfaceChain;

public interface ChainedListener<T> extends InterfaceChain<SimpleListener<T>, ChainedListener<T>>, SimpleListener<T> {
    
    @Override
    default ChainedListener<T> getEmptyInstance() {
        return ChainedListeners.empty();
    }
    
    @Override
    default ChainedListener<T> withElement(SimpleListener<T> element) {
        if (element == null) {
            return this;
        }
        
        int count = getElementCount() + 1;
        return new ChainedListener<T>() {
            @Override
            public void accept(T event) {
                try {
                    ChainedListener.this.accept(event);
                } finally {
                    element.accept(event);
                }
            }
            
            @Override
            public ChainedListener<T> withoutLastNode() {
                return ChainedListener.this;
            }
            
            @Override
            public SimpleListener<T> getDelegateOfLastNode() {
                return element;
            }
            
            @Override
            public int getElementCount() {
                return count;
            }
        };
    }
    
}
