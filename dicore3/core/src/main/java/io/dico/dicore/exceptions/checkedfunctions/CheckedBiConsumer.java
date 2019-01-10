package io.dico.dicore.exceptions.checkedfunctions;

import io.dico.dicore.exceptions.ExceptionHandler;

import java.util.function.BiConsumer;

/**
 * checked mimic of {@link BiConsumer}
 *
 * @param <TParam1>
 * @param <TParam2>
 * @param <TException>
 */
@FunctionalInterface
public interface CheckedBiConsumer<TParam1, TParam2, TException extends Throwable>
        extends CheckedFunctionalObject<Void, TException>, BiConsumer<TParam1, TParam2> {
    
    /**
     * The consuming action
     *
     * @param t the first argument to consume
     * @param u the second argument to consume
     * @throws TException if an exception occurs
     */
    void checkedAccept(TParam1 t, TParam2 u) throws TException;
    
    /**
     * unchecked wrapper for {@link #checkedAccept(Object, Object)}
     * If a {@link TException} occurs, an unchecked one might be thrown by {@link #resultOnError(Throwable, Object...)}
     *
     * @param t the first input
     * @param u the second input
     * @see #checkedAccept(Object, Object)
     * @see #resultOnError(Throwable, Object...)
     */
    @Override
    default void accept(TParam1 t, TParam2 u) {
        try {
            checkedAccept(t, u);
        } catch (Throwable ex) {
            handleGenericException(ex, t, u);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    default CheckedBiConsumer<TParam1, TParam2, TException> handleExceptionsWith(ExceptionHandler handler) {
        return new CheckedBiConsumer<TParam1, TParam2, TException>() {
            @Override
            public void checkedAccept(TParam1 t, TParam2 u) throws TException {
                CheckedBiConsumer.this.checkedAccept(t, u);
            }
    
            @Override
            public Void handleGenericException(Throwable thrown, Object... args) {
                handler.handleGenericException(thrown, args);
                return null;
            }
    
            @Override
            public CheckedBiConsumer<TParam1, TParam2, TException> handleExceptionsWith(ExceptionHandler handler) {
                return CheckedBiConsumer.this.handleExceptionsWith(handler);
            }
        };
    }
    
}
