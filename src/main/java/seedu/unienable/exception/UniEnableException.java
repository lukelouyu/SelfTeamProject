package seedu.unienable.exception;

/** Signals that a UniEnable operation could not be completed. */
public class UniEnableException extends Exception {
    /**
     * Creates a UniEnableException with the given detail message.
     *
     * @param message the detail message shown to the user
     */
    public UniEnableException(String message) {
        super(message);
    }

    /**
     * Creates a UniEnableException with the given detail message and underlying cause.
     *
     * @param message the detail message shown to the user
     * @param cause the underlying exception that triggered this one
     */
    public UniEnableException(String message, Throwable cause) {
        super(message, cause);
    }

    /** Returns the category label shown in the "[Error] CATEGORY: message" output. */
    public String getErrorCategory() {
        return "Error";
    }
}
