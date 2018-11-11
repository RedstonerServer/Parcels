package io.dico.dicore.exceptions.checkedfunctions;

import io.dico.dicore.exceptions.ExceptionHandler;

/**
 * checked mimic of {@link Runnable}
 *
 * @param <TException>
 */
@FunctionalInterface
public interface CheckedRunnable<TException extends Throwable>
        extends CheckedFunctionalObject<Void, TException>, Runnable {
    
    /**
     * The runnable action
     *
     * @throws TException if an exception occurs
     */
    void checkedRun() throws TException;
    
    /**
     * Unchecked version of {@link #checkedRun()}
     * If a {@link TException} occurs, an unchecked one might be thrown by {@link #resultOnError(Throwable, Object...)}
     *
     * @see #checkedRun()
     * @see #resultOnError(Throwable, Object...)
     */
    @Override
    default void run() {
        try {
            checkedRun();
        } catch (Throwable ex) {
            handleGenericException(ex);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    default CheckedRunnable<TException> handleExceptionsWith(ExceptionHandler handler) {
        return new CheckedRunnable<TException>() {
            @Override
            public void checkedRun() throws TException {
                CheckedRunnable.this.checkedRun();
            }
            
            @Override
            @SuppressWarnings("unchecked")
            public Void handleGenericException(Throwable thrown, Object... args) {
                handler.handleGenericException(thrown, args);
                return null;
            }
            
            @Override
            public CheckedRunnable<TException> handleExceptionsWith(ExceptionHandler handler) {
                return CheckedRunnable.this.handleExceptionsWith(handler);
            }
        };
    }
    
}
