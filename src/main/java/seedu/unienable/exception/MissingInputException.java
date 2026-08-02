package seedu.unienable.exception;

/** Signals that a required field is missing from a command. */
public class MissingInputException extends UniEnableException {
    private static final long serialVersionUID = 1L;

    /**
     * Creates a MissingInputException with the given detail message.
     *
     * @param message the detail message shown to the user
     */
    public MissingInputException(String message) {
        super(message);
    }

    @Override
    public String getErrorCategory() {
        return "Missing input";
    }
}
