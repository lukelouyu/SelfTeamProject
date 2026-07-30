package seedu.unienable.exception;

/** Signals that a required field is missing from a command. */
public class MissingInputException extends UniEnableException {
    public MissingInputException(String message) {
        super(message);
    }

    @Override
    public String getErrorCategory() {
        return "Missing input";
    }
}
