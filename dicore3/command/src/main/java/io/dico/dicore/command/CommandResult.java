package io.dico.dicore.command;

/**
 * This enum is intended to provide some constants for default messages.
 * Can be returned by a reflective command.
 * Currently, no constants have an actual message.
 * Prone to removal in the future because of lack of usefullness.
 */
public enum CommandResult {
    SUCCESS(null),
    QUIET_ERROR(null);

    private final String message;

    CommandResult(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

}
