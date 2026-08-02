package seedu.unienable.exception;

/** Signals that a date or time value could not be parsed or is out of range. */
public class InvalidDateTimeException extends UniEnableException {
    private static final long serialVersionUID = 1L;

    /**
     * Creates an InvalidDateTimeException with the given detail message.
     *
     * @param message the detail message shown to the user
     */
    public InvalidDateTimeException(String message) {
        super(message);
    }

    @Override
    public String getErrorCategory() {
        return "Invalid input";
    }
}
