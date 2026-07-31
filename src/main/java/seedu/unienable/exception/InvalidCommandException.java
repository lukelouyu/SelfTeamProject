package seedu.unienable.exception;

/** Signals that the entered command word or syntax is not recognised. */
public class InvalidCommandException extends UniEnableException {
    /**
     * Creates an InvalidCommandException with the given detail message.
     *
     * @param message the detail message shown to the user
     */
    public InvalidCommandException(String message) {
        super(message);
    }

    @Override
    public String getErrorCategory() {
        return "Invalid input";
    }
}
