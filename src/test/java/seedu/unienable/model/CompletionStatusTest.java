package seedu.unienable.model;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.junit.jupiter.api.Test;

class CompletionStatusTest {
    @Test
    public void values_matchDocumentedStatusesInOrder() {
        assertArrayEquals(
                new CompletionStatus[] { CompletionStatus.INCOMPLETE, CompletionStatus.COMPLETE },
                CompletionStatus.values());
    }
}
