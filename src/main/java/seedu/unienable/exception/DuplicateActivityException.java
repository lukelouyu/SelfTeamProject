package seedu.unienable.exception;

/** Signals that an activity duplicates or overlaps an existing one. */
public class DuplicateActivityException extends UniEnableException {
    private static final long serialVersionUID = 1L;

    /**
     * Creates a DuplicateActivityException with the given detail message.
     *
     * @param message the detail message shown to the user
     */
    public DuplicateActivityException(String message) {
        super(message);
    }

    @Override
    public String getErrorCategory() {
        return "Conflict";
    }
}
