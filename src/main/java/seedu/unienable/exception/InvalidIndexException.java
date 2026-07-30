package seedu.unienable.exception;

/** Signals that a referenced stable activity ID does not exist. */
public class InvalidIndexException extends UniEnableException {
    public InvalidIndexException(String message) {
        super(message);
    }

    @Override
    public String getErrorCategory() {
        return "Not found";
    }
}
