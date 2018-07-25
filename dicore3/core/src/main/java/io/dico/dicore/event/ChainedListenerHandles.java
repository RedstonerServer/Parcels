package io.dico.dicore.event;

public class ChainedListenerHandles {
    
    private ChainedListenerHandles() {
    
    }

    private static final ChainedListenerHandle empty = new ChainedListenerHandle() {
        @Override
        public void register() {
        
        }
        
        public void unregister() {
        
        }
        
        @Override
        public ChainedListenerHandle withElement(ListenerHandle other) {
            return ChainedListenerHandles.singleton(other);
        }
        
        @Override
        public int getElementCount() {
            return 0;
        }
        
        @Override
        public ListenerHandle getDelegateOfLastNode() {
            return null;
        }
    };
    
    public static ChainedListenerHandle empty() {
        return empty;
    }
    
    public static ChainedListenerHandle singleton(ListenerHandle element) {
        if (element instanceof ChainedListenerHandle) {
            return (ChainedListenerHandle) element;
        }
        if (element == null) {
            return empty();
        }
        return new ChainedListenerHandle() {
            @Override
            public void register() {
                element.register();
            }
            
            public void unregister() {
                element.unregister();
            }
            
            @Override
            public ListenerHandle getDelegateOfLastNode() {
                return element;
            }
        };
    }
    
}
