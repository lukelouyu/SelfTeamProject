package seedu.unienable.exception;

/** Signals that a local data file could not be read or written. */
public class StorageException extends UniEnableException {
    public StorageException(String message) {
        super(message);
    }

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public String getErrorCategory() {
        return "Storage error";
    }
}
