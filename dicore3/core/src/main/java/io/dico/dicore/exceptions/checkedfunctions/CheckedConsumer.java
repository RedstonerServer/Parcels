package io.dico.dicore.exceptions.checkedfunctions;

import io.dico.dicore.exceptions.ExceptionHandler;

import java.util.function.Consumer;

/**
 * checked mimic of {@link Consumer}
 *
 * @param <TParam>
 * @param <TException>
 */
@FunctionalInterface
public interface CheckedConsumer<TParam, TException extends Throwable>
        extends CheckedFunctionalObject<Void, TException>, Consumer<TParam> {
    
    /**
     * The consuming action
     *
     * @param t the argument to consume
     * @throws TException if an error occurs
     */
    void checkedAccept(TParam t) throws TException;
    
    /**
     * Unchecked version of {@link #checkedAccept(Object)}
     * If a {@link TException} occurs, an unchecked one might be thrown by {@link #resultOnError(Throwable, Object...)}
     *
     * @param t the argument to consume
     * @see #checkedAccept(Object)
     * @see #resultOnError(Throwable, Object...)
     */
    @Override
    default void accept(TParam t) {
        try {
            checkedAccept(t);
        } catch (Throwable ex) {
            handleGenericException(ex, t);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    default CheckedConsumer<TParam, TException> handleExceptionsWith(ExceptionHandler handler) {
        return new CheckedConsumer<TParam, TException>() {
            @Override
            public void checkedAccept(TParam t) throws TException {
                CheckedConsumer.this.checkedAccept(t);
            }
            
            @Override
            @SuppressWarnings("unchecked")
            public Void handleGenericException(Throwable thrown, Object... args) {
                handler.handleGenericException(thrown, args);
                return null;
            }
            
            @Override
            public CheckedConsumer<TParam, TException> handleExceptionsWith(ExceptionHandler handler) {
                return CheckedConsumer.this.handleExceptionsWith(handler);
            }
        };
    }
    
}
