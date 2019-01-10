package io.dico.dicore.exceptions.checkedfunctions;

import io.dico.dicore.exceptions.ExceptionHandler;

/**
 * Base interface for all checked functional interfaces
 * Most subinterfaces will mimic interfaces in the package {@link java.util.function}
 * <p>
 * Checked functional interfaces are functions with throws declarations.
 * The name comes from the fact that they can throw <b>checked</b> exceptions.
 * <p>
 * They extend their non-checked counterparts, whose methods are implemented by
 * returning the result of {@link #resultOnError(TException, Object...)} when an exception is thrown
 * <p>
 * Made public to allow more specialized checked functional interfaces to subclass it.
 * Making primitive versions shouldn't provide a significant performance increase because we're checking for exceptions,
 * the performance impact of which is probably a few magnitudes larger. Don't quote me on this.
 *
 * @param <TResult>    The return type of this functional interface's method
 * @param <TException> The type of exception that might be thrown
 */
public interface CheckedFunctionalObject<TResult, TException extends Throwable> extends ExceptionHandler {
    
    /**
     * {@inheritDoc}
     *
     * @param ex The exception to be handled
     */
    @Override
    default void handle(Throwable ex) {
        handleGenericException(ex);
    }
    
    /**
     * {@inheritDoc}
     * <p>
     * Method to handle exceptions thrown by the default implementations of subinterfaces.
     * Since you can't catch a type parameter as exception, this code is in place to take care of it.
     * <p>
     * If the thrown exception is not a TException, an unchecked version is thrown by calling this method.
     *
     * @param thrown The thrown exception
     * @param args   the arguments supplied to the method that threw the exception, if any. These are not guaranteed to be given if parameters are present.
     * @return The result computed by {@link #resultOnError(Throwable, Object...)}
     * @see #resultOnError(Throwable, Object...)
     */
    @Override
    @SuppressWarnings("unchecked")
    default TResult handleGenericException(Throwable thrown, Object... args) {
        
        // check if the throwable is a TException
        TException castedException;
        try {
            castedException = (TException) thrown;
        } catch (ClassCastException ex) {
            // if not, throw an unchecked version of it
            ExceptionHandler.UNCHECKED.handleGenericException(thrown);
            // this code is never reached.
            return null;
        }
        
        // if it is a TException, use resultOnError to compute result.
        return resultOnError(castedException, args);
    }
    
    /**
     * This method handles the exceptions thrown by the checked method of this object, when called by its unchecked wrapper.
     *
     * @param ex   The exception thrown
     * @param args The (typed) arguments passed, if any
     * @return The result to return, if any
     */
    default TResult resultOnError(TException ex, Object... args) {
        return null;
    }
    
    /**
     * Creates a new functional object that uses the given exception handler to handle exceptions.
     *
     * @param handler The handler to handle exceptions
     * @return a new functional object that uses the given exception handler to handle exceptions
     */
    CheckedFunctionalObject<TResult, TException> handleExceptionsWith(ExceptionHandler handler);
    
}
