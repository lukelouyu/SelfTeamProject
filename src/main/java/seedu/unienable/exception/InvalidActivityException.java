package seedu.unienable.exception;

/** Signals that an activity's fields are missing or fail validation. */
public class InvalidActivityException extends UniEnableException {
    /**
     * Creates an InvalidActivityException with the given detail message.
     *
     * @param message the detail message shown to the user
     */
    public InvalidActivityException(String message) {
        super(message);
    }

    @Override
    public String getErrorCategory() {
        return "Invalid input";
    }
}
