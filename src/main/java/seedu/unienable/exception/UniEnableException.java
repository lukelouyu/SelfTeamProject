package seedu.unienable.exception;

/** Signals that a UniEnable operation could not be completed. */
public class UniEnableException extends Exception {
    public UniEnableException(String message) {
        super(message);
    }

    public UniEnableException(String message, Throwable cause) {
        super(message, cause);
    }

    /** Returns the category label shown in the "[Error] CATEGORY: message" output. */
    public String getErrorCategory() {
        return "Error";
    }
}
