package io.dico.dicore.exceptions.checkedfunctions;

import io.dico.dicore.exceptions.ExceptionHandler;

import java.util.function.BiFunction;

/**
 * checked mimic of {@link BiFunction}
 *
 * @param <TParam1>
 * @param <TParam2>
 * @param <TResult>
 * @param <TException>
 */
@FunctionalInterface
public interface CheckedBiFunction<TParam1, TParam2, TResult, TException extends Throwable>
        extends CheckedFunctionalObject<TResult, TException>, BiFunction<TParam1, TParam2, TResult> {
    
    /**
     * the functional method
     *
     * @param t the first input
     * @param u the second input
     * @return the function output
     * @throws TException if an exception occurs
     */
    TResult checkedApply(TParam1 t, TParam2 u) throws TException;
    
    /**
     * unchecked wrapper for {@link #checkedApply(Object, Object)}
     * If a {@link TException} occurs, an unchecked one might be thrown by {@link #resultOnError(Throwable, Object...)}
     *
     * @param t the first input
     * @param u the second input
     * @return the function output, or the result from {@link #resultOnError(Throwable, Object...)} if an exception was thrown
     * @see #checkedApply(Object, Object)
     * @see #resultOnError(Throwable, Object...)
     */
    @Override
    default TResult apply(TParam1 t, TParam2 u) {
        try {
            return checkedApply(t, u);
        } catch (Throwable ex) {
            return handleGenericException(ex, t, u);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    default CheckedBiFunction<TParam1, TParam2, TResult, TException> handleExceptionsWith(ExceptionHandler handler) {
        return new CheckedBiFunction<TParam1, TParam2, TResult, TException>() {
            @Override
            public TResult checkedApply(TParam1 t, TParam2 u) throws TException {
                return CheckedBiFunction.this.checkedApply(t, u);
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
            public CheckedBiFunction<TParam1, TParam2, TResult, TException> handleExceptionsWith(ExceptionHandler handler) {
                return CheckedBiFunction.this.handleExceptionsWith(handler);
            }
        };
    }
}
