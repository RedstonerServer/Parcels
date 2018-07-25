package io.dico.dicore.command;

public class CommandException extends Exception {

    public CommandException() {
    }

    public CommandException(String message) {
        super(message);
    }

    public CommandException(String message, Throwable cause) {
        super(message, cause);
    }

    public CommandException(Throwable cause) {
        super(cause);
    }

    public static CommandException missingArgument(String parameterName) {
        return new CommandException("Missing argument for " + parameterName);
    }

    public static CommandException invalidArgument(String parameterName, String syntaxHelp) {
        return new CommandException("Invalid input for " + parameterName + ", should be " + syntaxHelp);
    }

}
