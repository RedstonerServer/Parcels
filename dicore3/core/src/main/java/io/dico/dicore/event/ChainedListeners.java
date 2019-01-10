package io.dico.dicore.event;

@SuppressWarnings("unchecked")
public class ChainedListeners {
    
    private static final ChainedListener<?> empty = new ChainedListener<Object>() {
        @Override
        public void accept(Object event) {
        
        }
        
        @Override
        public ChainedListener<Object> withElement(SimpleListener other) {
            return ChainedListeners.singleton(other);
        }
        
        @Override
        public int getElementCount() {
            return 0;
        }
        
        @Override
        public SimpleListener<Object> getDelegateOfLastNode() {
            return null;
        }
    };
    
    private ChainedListeners() {
    
    }
    
    public static <T> ChainedListener<T> empty() {
        return (ChainedListener<T>) empty;
    }
    
    public static <T> ChainedListener<T> singleton(SimpleListener<T> element) {
        if (element instanceof ChainedListener) {
            return (ChainedListener<T>) element;
        }
        if (element == null) {
            return empty();
        }
        return new ChainedListener<T>() {
            @Override
            public void accept(T event) {
                element.accept(event);
            }
            
            @Override
            public SimpleListener getDelegateOfLastNode() {
                return element;
            }
        };
    }
    
}
