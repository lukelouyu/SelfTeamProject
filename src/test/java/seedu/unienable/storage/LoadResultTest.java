package seedu.unienable.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class LoadResultTest {
    @Test
    public void getters_returnConstructorValues() {
        LoadResult<String> result = new LoadResult<>(List.of("a", "b"), List.of("warning 1"));

        assertEquals(List.of("a", "b"), result.getRecords());
        assertEquals(List.of("warning 1"), result.getWarnings());
    }

    @Test
    public void hasWarnings_reflectsWarningList() {
        assertTrue(new LoadResult<>(List.of(), List.of("warning 1")).hasWarnings());
        assertFalse(new LoadResult<>(List.of(), List.of()).hasWarnings());
    }

    @Test
    public void getRecords_isUnmodifiable() {
        LoadResult<String> result = new LoadResult<>(List.of("a"), List.of());

        assertThrows(UnsupportedOperationException.class, () -> result.getRecords().add("b"));
    }
}
