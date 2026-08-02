package seedu.unienable.parser.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

class FieldParserTest {
    // Every marker used by any parser in the app, as of this session. Kept as one flat list
    // (rather than per-parser sets) because the boundary check in indexOfMarker() is global: a
    // collision only matters if the two colliding markers could realistically appear in the same
    // command's argument text, but it costs nothing to check the full vocabulary and is simpler
    // to keep in sync than reasoning about which markers are ever mixed together.
    private static final String[] ALL_KNOWN_MARKERS = {
        "n/", "c/", "date/", "type/", "from/", "to/", "earliest/", "latest/", "dur/", "energy/",
        "sensory/", "topic/", "note/", "view/", "status/", "order/", "k/", "old/", "new/", "shelter/",
    };

    @Test
    public void indexOfMarker_noKnownMarkerIsMistakenlyMatchedInsideAnother() {
        // Systematic version of the "topic/" vs "c/" regression: for every pair of known markers
        // where one is a trailing substring of the other (e.g. "c/" inside "topic/"), the shorter
        // marker must not match when it appears embedded in the longer one with nothing else
        // preceding it in the text (so there is no whitespace boundary to legitimise the match).
        for (String longerMarker : ALL_KNOWN_MARKERS) {
            for (String shorterMarker : ALL_KNOWN_MARKERS) {
                if (shorterMarker.equals(longerMarker) || !longerMarker.endsWith(shorterMarker)) {
                    continue;
                }
                int result = FieldParser.indexOfMarker(longerMarker, shorterMarker, 0);
                assertTrue(result == -1, "\"" + shorterMarker + "\" was matched inside \"" + longerMarker
                        + "\" at index " + result + " with no preceding whitespace boundary");
            }
        }
    }

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

    @Test
    public void indexOfMarker_uppercaseMarkerInText_isMatched() {
        assertEquals(0, FieldParser.indexOfMarker("N/CG3207 lecture", "n/", 0));
    }

    @Test
    public void indexOfMarker_mixedCaseMarkerInText_isMatched() {
        String input = "n/Lecture C/ACADEMIC";

        assertEquals(10, FieldParser.indexOfMarker(input, "c/", 0));
    }

    @Test
    public void extractField_uppercaseStartMarker_returnsTrimmedValue() {
        String input = "N/CG3207 lecture C/ACADEMIC";

        assertEquals("CG3207 lecture", FieldParser.extractField(input, "n/", "c/"));
    }

    @Test
    public void extractField_uppercaseMarkerStillRespectsBoundary() {
        // "TOPIC/" ends in the substring "C/"; the boundary check must still hold when the
        // marker in the text is uppercase.
        String input = "TOPIC/CS2113";

        assertNull(FieldParser.extractField(input, "c/", null));
    }

    @Test
    public void extractPresentFields_singleField_returnsItsValue() {
        Map<String, String> fields = FieldParser.extractPresentFields("dur/60", "n/", "dur/", "energy/");

        assertEquals(Map.of("dur/", "60"), fields);
    }

    @Test
    public void extractPresentFields_multipleFieldsAnyOrder_boundsEachByNextPresentMarker() {
        Map<String, String> fields = FieldParser.extractPresentFields(
                "energy/4 n/New activity name sensory/2", "n/", "energy/", "sensory/");

        assertEquals("4", fields.get("energy/"));
        assertEquals("New activity name", fields.get("n/"));
        assertEquals("2", fields.get("sensory/"));
    }

    @Test
    public void extractPresentFields_absentMarkers_areOmittedFromResult() {
        Map<String, String> fields = FieldParser.extractPresentFields(
                "note/Bring headphones", "n/", "note/", "topic/");

        assertEquals(1, fields.size());
        assertEquals("Bring headphones", fields.get("note/"));
    }

    @Test
    public void extractPresentFields_noMarkersPresent_returnsEmptyMap() {
        assertTrue(FieldParser.extractPresentFields("nothing relevant here", "n/", "c/").isEmpty());
    }
}
