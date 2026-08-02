package seedu.unienable.exception;

/** Signals that a local data file could not be read or written. */
public class StorageException extends UniEnableException {
    private static final long serialVersionUID = 1L;

    /**
     * Creates a StorageException with the given detail message.
     *
     * @param message the detail message shown to the user
     */
    public StorageException(String message) {
        super(message);
    }

    /**
     * Creates a StorageException with the given detail message and underlying cause.
     *
     * @param message the detail message shown to the user
     * @param cause the underlying exception that triggered this one
     */
    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public String getErrorCategory() {
        return "Storage error";
    }
}
