package seedu.unienable.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class UniEnableExceptionTest {
    @Test
    public void uniEnableException_messageAndCause_arePreserved() {
        Throwable cause = new RuntimeException("root cause");
        UniEnableException exception = new UniEnableException("something failed", cause);

        assertEquals("something failed", exception.getMessage());
        assertSame(cause, exception.getCause());
        assertEquals("Error", exception.getErrorCategory());
    }

    @Test
    public void invalidCommandException_hasInvalidInputCategory() {
        assertEquals("Invalid input", new InvalidCommandException("bad command").getErrorCategory());
    }

    @Test
    public void invalidActivityException_hasInvalidInputCategory() {
        assertEquals("Invalid input", new InvalidActivityException("bad field").getErrorCategory());
    }

    @Test
    public void invalidDateTimeException_hasInvalidInputCategory() {
        assertEquals("Invalid input", new InvalidDateTimeException("bad date").getErrorCategory());
    }

    @Test
    public void invalidIndexException_hasNotFoundCategory() {
        assertEquals("Not found", new InvalidIndexException("no such ID").getErrorCategory());
    }

    @Test
    public void duplicateActivityException_hasConflictCategory() {
        assertEquals("Conflict", new DuplicateActivityException("overlaps").getErrorCategory());
    }

    @Test
    public void storageException_hasStorageErrorCategory() {
        assertEquals("Storage error", new StorageException("disk failed").getErrorCategory());
    }
}
