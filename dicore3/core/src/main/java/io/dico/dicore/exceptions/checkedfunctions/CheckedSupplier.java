package io.dico.dicore.exceptions.checkedfunctions;

import io.dico.dicore.exceptions.ExceptionHandler;

import java.util.function.Supplier;

/**
 * checked mimic of {@link Supplier}
 *
 * @param <TResult>
 * @param <TException>
 */
@FunctionalInterface
public interface CheckedSupplier<TResult, TException extends Throwable>
        extends CheckedFunctionalObject<TResult, TException>, Supplier<TResult> {
    
    /**
     * The computation
     *
     * @return the result of this computation
     * @throws TException if an error occurs
     */
    TResult checkedGet() throws TException;
    
    /**
     * Unchecked version of {@link #checkedGet()}
     * If a {@link TException} occurs, an unchecked one might be thrown by {@link #resultOnError(Throwable, Object...)}
     *
     * @return the result of this computation
     * @see #checkedGet()
     * @see #resultOnError(Throwable, Object...)
     */
    @Override
    default TResult get() {
        try {
            return checkedGet();
        } catch (Throwable ex) {
            return handleGenericException(ex);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    default CheckedSupplier<TResult, TException> handleExceptionsWith(ExceptionHandler handler) {
        return new CheckedSupplier<TResult, TException>() {
            @Override
            public TResult checkedGet() throws TException {
                return CheckedSupplier.this.checkedGet();
            }
            
            @Override
            @SuppressWarnings("unchecked")
            public TResult handleGenericException(Throwable thrown, Object... args) {
                Object result = handler.handleGenericException(thrown, args);
                try {
                    return (TResult) result;
                } catch (Exception ex) {
                    return null;
                }
            }
            
            @Override
            public CheckedSupplier<TResult, TException> handleExceptionsWith(ExceptionHandler handler) {
                return CheckedSupplier.this.handleExceptionsWith(handler);
            }
        };
    }
}
