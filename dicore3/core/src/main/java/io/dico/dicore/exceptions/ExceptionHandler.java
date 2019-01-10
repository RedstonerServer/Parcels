package io.dico.dicore.exceptions;

import io.dico.dicore.exceptions.checkedfunctions.CheckedRunnable;
import io.dico.dicore.exceptions.checkedfunctions.CheckedSupplier;
import io.dico.dicore.exceptions.checkedfunctions.CheckedFunctionalObject;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.SQLException;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

@FunctionalInterface
public interface ExceptionHandler {
    
    /**
     * Handle the given exception according to this handler's implementation
     *
     * @param ex   The exception to be handled
     * @throws NullPointerException if ex is null, unless the implementation specifies otherwise
     * @throws Error                ex if ex is an instance of Error, unless the implementation specifies otherwise
     */
    void handle(Throwable ex);
    
    /**
     * Handle the given exception according to this handler's implementation
     * This method is intended for use by {@link CheckedFunctionalObject} and subinterfaces.
     * It supplies exception handlers the option to acquire more information, by overriding this method and calling it from {@link #handle(Throwable)}
     *
     * @param ex   The exception to be handled
     * @param args Any arguments passed, this is used by {@link CheckedFunctionalObject} and subinterfaces.
     * @return {@code null} (unless specified otherwise by the implementation)
     * @throws NullPointerException if ex is null, unless the implementation specifies otherwise
     * @throws Error                ex if ex is an instance of Error, unless the implementation specifies otherwise
     */
    default Object handleGenericException(Throwable ex, Object... args) {
        handle(ex);
        return null;
    }
    
    /**
     * @return true if this {@link ExceptionHandler}'s {@link #handleGenericException(Throwable, Object...)} method is <b>never</b> expected to throw
     * an unchecked exception other than {@link Error}
     */
    default boolean isSafe() {
        return true;
    }
    
    /**
     * Runs the given checked action, handling any thrown exceptions using this exception handler.
     * <p>
     * Any exceptions thrown by this handler are delegated to the caller.
     *
     * @param action The action to run
     * @throws NullPointerException if action is null
     */
    default void runSafe(CheckedRunnable<? extends Throwable> action) {
        Objects.requireNonNull(action);
        try {
            action.checkedRun();
        } catch (Throwable ex) {
            handle(ex);
        }
    }
    
    /**
     * Computes the result of the given checked supplier, handling any thrown exceptions using this exception handler.
     * <p>
     * Any exceptions thrown by this handler are delegated to the caller.
     *
     * @param action The supplier whose result to compute
     * @param <T>    generic type parameter for the supplier and the result type of this method
     * @return The result of this computation, or null if an error occurred
     * @throws NullPointerException if action is null
     */
    
    default <T> T supplySafe(CheckedSupplier<T, ? extends Throwable> action) {
        Objects.requireNonNull(action);
        try {
            return action.checkedGet();
        } catch (Throwable ex) {
            handle(ex);
            return null;
        }
    }
    
    /**
     * @param action The action to wrap
     * @return A runnable that wraps the given action using this handler's {@link #runSafe(CheckedRunnable)} method.
     * @see #runSafe(CheckedRunnable)
     */
    default Runnable safeRunnable(CheckedRunnable<? extends Throwable> action) {
        return () -> runSafe(action);
    }
    
    /**
     * @param supplier The computation to wrap
     * @return A supplier that wraps the given computation using this handler's {@link #supplySafe(CheckedSupplier)} method.
     * @see #supplySafe(CheckedSupplier)
     */
    default <T> Supplier<T> safeSupplier(CheckedSupplier<T, ? extends Throwable> supplier) {
        return () -> supplySafe(supplier);
    }
    
    /**
     * Logs the given exception as an error to {@code out}
     * <p>
     * Format: Error occurred while {@code failedActivityDescription}, followed by additional details and a stack trace
     *
     * @param out                       The consumer to accept the error message, for instance {@code {@link java.util.logging.Logger logger}::warning}.
     * @param failedActivityDescription A description of the activity that was being executed when the exception was thrown
     * @param ex                        The exception that was thrown
     * @throws NullPointerException if any argument is null
     */
    static void log(Consumer<String> out, String failedActivityDescription, Throwable ex) {
        if (out == null || failedActivityDescription == null || ex == null) {
            throw new NullPointerException();
        }
        
        StringWriter msg = new StringWriter(1024);
        msg.append("Error occurred while ").append(failedActivityDescription).append(':');
        
        if (ex instanceof SQLException) {
            SQLException sqlex = (SQLException) ex;
            msg.append('\n').append("Error code: ").append(Integer.toString(sqlex.getErrorCode()));
            msg.append('\n').append("SQL State: ").append(sqlex.getSQLState());
        }
        
        msg.append('\n').append("=======START STACK=======");
        try (PrintWriter pw = new PrintWriter(msg)) {
            ex.printStackTrace(pw);
        }
        msg.append('\n').append("========END STACK========");
        
        out.accept(msg.toString());
    }
    
    /**
     * @param activityDescription The activity description
     * @return An ExceptionHandler that prints to {@link System#out}
     * @see #log(Consumer, String)
     */
    static ExceptionHandler log(String activityDescription) {
        // A method reference would cache the current value in System.out
        // This would update if the value in System.out is changed (by for example, applying a PrintStream that handles colours).
        return log(msg -> System.out.println(msg), activityDescription);
    }
    
    /**
     * @param out                 The Consumer to be passed to {@link #log(Consumer, String, Throwable)}
     * @param activityDescription The activity description to be passed to {@link #log(Consumer, String, Throwable)}
     * @return An ExceptionHandler that passes exceptions with given arguments to {@link #log(Consumer, String, Throwable)}
     * @see #log(Consumer, String, Throwable)
     */
    static ExceptionHandler log(Consumer<String> out, String activityDescription) {
        return ex -> log(out, activityDescription, ex);
    }
    
    /**
     * This ExceptionHandler turns any Throwable into an unchecked exception, then throws it again.
     */
    ExceptionHandler UNCHECKED = new ExceptionHandler() {
        @Override
        public void handle(Throwable ex) {
            errorFilter(ex);
            if (ex instanceof RuntimeException) {
                throw (RuntimeException) ex;
            }
            throw new RuntimeException(ex);
        }
    
        @Override
        public boolean isSafe() {
            return false;
        }
    };
    
    /**
     * This ExceptionHandler suppresses all exceptions,
     * apart from {@link Error} because that is not usually supposed to be caught
     */
    ExceptionHandler SUPPRESS = ExceptionHandler::errorFilter;
    
    /**
     * This ExceptionHandler calls {@link Throwable#printStackTrace()} unless it is a {@link NullPointerException} or {@link Error}.
     */
    ExceptionHandler PRINT_UNLESS_NP = ex -> {
        errorFilter(ex);
        if (!(ex instanceof NullPointerException)) {
            ex.printStackTrace();
        }
    };
    
    static void errorFilter(Throwable ex) {
        if (ex instanceof Error) {
            throw (Error) ex;
        } else if (ex == null) {
            throw new NullPointerException();
        }
    }
    
}
