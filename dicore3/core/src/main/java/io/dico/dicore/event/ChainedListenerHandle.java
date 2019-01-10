package io.dico.dicore.event;

import io.dico.dicore.InterfaceChain;

public interface ChainedListenerHandle extends InterfaceChain<ListenerHandle, ChainedListenerHandle>, ListenerHandle {
    
    @Override
    default ChainedListenerHandle getEmptyInstance() {
        return ChainedListenerHandles.empty();
    }
    
    @Override
    default ChainedListenerHandle withElement(ListenerHandle element) {
        if (element == null) {
            return this;
        }
        
        int count = getElementCount() + 1;
        return new ChainedListenerHandle() {
            @Override
            public void register() {
                try {
                    ChainedListenerHandle.this.register();
                } finally {
                    element.register();
                }
            }
            
            @Override
            public void unregister() {
                try {
                    ChainedListenerHandle.this.unregister();
                } finally {
                    element.unregister();
                }
            }
            
            @Override
            public ChainedListenerHandle withoutLastNode() {
                return ChainedListenerHandle.this;
            }
            
            @Override
            public ListenerHandle getDelegateOfLastNode() {
                return element;
            }
            
            @Override
            public int getElementCount() {
                return count;
            }
        };
    }
    
}
