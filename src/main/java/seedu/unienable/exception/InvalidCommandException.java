package seedu.unienable.exception;

/** Signals that the entered command word or syntax is not recognised. */
public class InvalidCommandException extends UniEnableException {
    public InvalidCommandException(String message) {
        super(message);
    }

    @Override
    public String getErrorCategory() {
        return "Invalid input";
    }
}
