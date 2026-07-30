package seedu.unienable.exception;

/** Signals that an activity's fields are missing or fail validation. */
public class InvalidActivityException extends UniEnableException {
    public InvalidActivityException(String message) {
        super(message);
    }

    @Override
    public String getErrorCategory() {
        return "Invalid input";
    }
}
