package io.dico.dicore.exceptions.checkedfunctions;

import io.dico.dicore.exceptions.ExceptionHandler;

import java.util.function.Function;

/**
 * checked mimic of {@link Function}
 *
 * @param <TParam>
 * @param <TResult>
 * @param <TException>
 */
@FunctionalInterface
public interface CheckedFunction<TParam, TResult, TException extends Throwable>
        extends CheckedFunctionalObject<TResult, TException>, Function<TParam, TResult> {
    
    /**
     * the functional method
     *
     * @param t the input
     * @return the function output
     * @throws TException if an exception occurs
     */
    TResult checkedApply(TParam t) throws TException;
    
    /**
     * unchecked wrapper for {@link #checkedApply(Object)}
     * If a {@link TException} occurs, an unchecked one might be thrown by {@link #resultOnError(Throwable, Object...)}
     *
     * @param t the input
     * @return the function output, or the result from {@link #resultOnError(Throwable, Object...)} if an exception was thrown
     * @see #checkedApply(Object)
     * @see #resultOnError(Throwable, Object...)
     */
    @Override
    default TResult apply(TParam t) {
        try {
            return checkedApply(t);
        } catch (Throwable ex) {
            return handleGenericException(ex, t);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    default CheckedFunction<TParam, TResult, TException> handleExceptionsWith(ExceptionHandler handler) {
        return new CheckedFunction<TParam, TResult, TException>() {
            @Override
            public TResult checkedApply(TParam t) throws TException {
                return CheckedFunction.this.checkedApply(t);
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
            public CheckedFunction<TParam, TResult, TException> handleExceptionsWith(ExceptionHandler handler) {
                return CheckedFunction.this.handleExceptionsWith(handler);
            }
        };
    }
    
}
