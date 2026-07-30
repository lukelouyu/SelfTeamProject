package seedu.unienable.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MissingInputExceptionTest {
    @Test
    public void getErrorCategory_returnsMissingInput() {
        MissingInputException exception = new MissingInputException("energy is required.");

        assertEquals("Missing input", exception.getErrorCategory());
        assertEquals("energy is required.", exception.getMessage());
    }
}
