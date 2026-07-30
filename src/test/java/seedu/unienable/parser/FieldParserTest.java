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

    @Test
    public void extractField_markerEmbeddedInsideAnotherMarker_isNotMistakenForThatField() {
        // "topic/" ends in the substring "c/"; without boundary checking, extractField(..., "c/",
        // null) would wrongly match inside "topic/" and return "CS2113" as a bogus category value.
        String input = "topic/CS2113";

        assertNull(FieldParser.extractField(input, "c/", null));
    }

    @Test
    public void extractField_topicMarkerWhoseNameContainsAnotherMarker_extractsItsFullValue() {
        String input = "topic/CS2113";

        assertEquals("CS2113", FieldParser.extractField(input, "topic/", null));
    }

    @Test
    public void indexOfMarker_markerAtStartOfText_isMatched() {
        assertEquals(0, FieldParser.indexOfMarker("c/ACADEMIC", "c/", 0));
    }

    @Test
    public void indexOfMarker_markerAfterWhitespace_isMatched() {
        String input = "n/Lecture c/ACADEMIC";

        assertEquals(10, FieldParser.indexOfMarker(input, "c/", 0));
    }

    @Test
    public void indexOfMarker_markerEmbeddedWithNoPrecedingWhitespace_isNotMatched() {
        assertEquals(-1, FieldParser.indexOfMarker("topic/CS2113", "c/", 0));
    }
}
