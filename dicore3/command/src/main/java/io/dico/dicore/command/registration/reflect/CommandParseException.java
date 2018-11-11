package io.dico.dicore.command.registration.reflect;

/**
 * Thrown if an error occurs while 'parsing' a reflection command method
 * Other errors can be thrown too in there that may not be directly relevant to a parsing error.
 */
public class CommandParseException extends Exception {

    public CommandParseException() {
    }

    public CommandParseException(String message) {
        super(message);
    }

    public CommandParseException(String message, Throwable cause) {
        super(message, cause);
    }

    public CommandParseException(Throwable cause) {
        super(cause);
    }

    public CommandParseException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
