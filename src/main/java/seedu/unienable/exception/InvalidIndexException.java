package seedu.unienable.exception;

/** Signals that a referenced stable activity ID does not exist. */
public class InvalidIndexException extends UniEnableException {
    /**
     * Creates an InvalidIndexException with the given detail message.
     *
     * @param message the detail message shown to the user
     */
    public InvalidIndexException(String message) {
        super(message);
    }

    @Override
    public String getErrorCategory() {
        return "Not found";
    }
}
