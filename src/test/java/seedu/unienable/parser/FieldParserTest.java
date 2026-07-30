package seedu.unienable.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class FieldParserTest {
    @Test
    public void extractField_withEndMarker_returnsTrimmedValue() {
        String input = "n/CG3207 lecture c/ACADEMIC";

        assertEquals("CG3207 lecture", FieldParser.extractField(input, "n/", "c/"));
    }

    @Test
    public void extractField_endMarkerMissing_readsUntilEndOfInput() {
        String input = "n/CG3207 lecture";

        assertEquals("CG3207 lecture", FieldParser.extractField(input, "n/", "c/"));
    }

    @Test
    public void extractField_nullEndMarker_readsUntilEndOfInput() {
        String input = "n/CG3207 lecture c/ACADEMIC";

        assertEquals("CG3207 lecture c/ACADEMIC", FieldParser.extractField(input, "n/", null));
    }

    @Test
    public void extractField_startMarkerNotFound_returnsNull() {
        String input = "c/ACADEMIC";

        assertNull(FieldParser.extractField(input, "n/", "c/"));
    }
}
