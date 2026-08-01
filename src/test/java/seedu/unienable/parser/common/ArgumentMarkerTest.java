package seedu.unienable.parser.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ArgumentMarkerTest {
    @Test
    public void required_returnsMarkerFlaggedRequired() {
        ArgumentMarker marker = ArgumentMarker.required("n/");

        assertEquals("n/", marker.getPrefix());
        assertTrue(marker.isRequired());
    }

    @Test
    public void optional_returnsMarkerFlaggedNotRequired() {
        ArgumentMarker marker = ArgumentMarker.optional("note/");

        assertEquals("note/", marker.getPrefix());
        assertFalse(marker.isRequired());
    }
}
