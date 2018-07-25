package io.dico.dicore;

/**
 * A chainable object
 * <p>
 * It is not possible to declare another upper bound for type parameter Subtype.
 * However, it is required that it also extends the type parameter Element.
 *
 * @param <Subtype> the interface that is chainable
 * @param <Element> the element of the chain, this is a supertype of the subtype
 */
@SuppressWarnings("unchecked")
public interface InterfaceChain<Element, Subtype extends InterfaceChain<Element, Subtype>> {
    
    /**
     * returns the empty InterfaceChain instance.
     *
     * @return the empty InterfaceChain instance
     */
    Subtype getEmptyInstance();
    
    /**
     * returns a InterfaceChain with the last added element detached.
     * <p>
     * if this InterfaceChain is the empty instance, the empty instance is returned.
     *
     * @return a InterfaceChain with the last added element detached
     * @implNote for the purpose of lambdas, the default implementation also returns the empty instance.
     */
    default Subtype withoutLastNode() {
        return getEmptyInstance();
    }
    
    /**
     * returns the element that was inserted as the last element
     * <p>
     * For instance, calling this method on the result of calling {@link InterfaceChain#withElement(Object)}
     * would return the given element.
     *
     * @return the element that was inserted as the last element
     * @implNote for the purpose of lambdas, the default implementation returns this object,
     * which is required to implement the Element type parameter.
     */
    default Element getDelegateOfLastNode() {
        //noinspection unchecked
        return (Element) this;
    }
    
    /**
     * @return The number of elements chained from this InterfaceChain.
     * @implNote for the purpose of lambdas, the default implementation returns 1.
     */
    default int getElementCount() {
        return 1;
    }
    
    /**
     * Get a new InterfaceChain that includes the given Element.
     * <p>
     * The default implementation of the Subtype should look like this:
     * <pre> {@code
     * if (element == null) {
     *     return this;
     * }
     *
     * int count = getElementCount() + 1;
     * return new Subtype() {
     *     \@Override
     *     public void exampleElementMethod() {
     *         try {
     *             Subtype.this.exampleElementMethod();
     *         } finally {
     *             element.exampleElementMethod();
     *         }
     *     }
     *
     *     \@Override
     *     public Subtype withoutLastNode() {
     *         return Subtype.this;
     *     }
     *
     *     \@Override
     *     public Element getDelegateOfLastNode() {
     *         return element;
     *     }
     *
     *     \@Override
     *     public int getElementCount() {
     *         return count;
     *     }
     * };
     * }
     * </pre>
     *
     * @param element A new element to insert at the end of this InterfaceChain.
     * @return a new InterfaceChain that includes the given Element.
     */
    Subtype withElement(Element element);
    
    /**
     * Append each of the elements to this InterfaceChain
     *
     * @param elements the elements to append
     * @return a new InterfaceChain with the elements appended
     */
    default Subtype withElements(Element... elements) {
        Subtype result = (Subtype) this;
        for (Element element : elements) {
            result = result.withElement(element);
        }
        return result;
    }
    
    /*
    Example Subtypes implementation
    
        public class Subtypes {
            
            private Subtypes() {
            
            }
            
            private static final Subtype empty = new Subtype() {
                @Override
                public void exampleElementMethod() {
                
                }
            
                @Override
                public Subtype withElement(Element other) {
                    return Subtypes.singleton(other);
                }
            
                @Override
                public int getElementCount() {
                    return 0;
                }
            
                @Override
                public Element getDelegateOfLastNode() {
                    return null;
                }
            };
            
            public static Subtype empty() {
                return empty;
            }
            
            public static Subtype singleton(Element element) {
                if (element instanceof Subtype) {
                    return (Subtype) element;
                }
                if (element == null) {
                    return empty();
                }
                return new Subtype() {
                    @Override
                    public void exampleElementMethod() {
                        element.exampleElementMethod();
                    }
                    
                    @Override
                    public Element getDelegateOfLastNode() {
                        return element;
                    }
                };
            }
            
        }

     */
    
}
