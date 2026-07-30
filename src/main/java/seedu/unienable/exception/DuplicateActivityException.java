package seedu.unienable.exception;

/** Signals that an activity duplicates or overlaps an existing one. */
public class DuplicateActivityException extends UniEnableException {
    public DuplicateActivityException(String message) {
        super(message);
    }

    @Override
    public String getErrorCategory() {
        return "Conflict";
    }
}
