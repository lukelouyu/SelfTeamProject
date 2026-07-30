package seedu.unienable.exception;

/** Signals that a date or time value could not be parsed or is out of range. */
public class InvalidDateTimeException extends UniEnableException {
    public InvalidDateTimeException(String message) {
        super(message);
    }

    @Override
    public String getErrorCategory() {
        return "Invalid input";
    }
}
