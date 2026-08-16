package seedu.unienable.parser.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

import seedu.unienable.exception.InvalidCommandException;
import seedu.unienable.exception.MissingInputException;

class ArgumentTokenizerTest {

    @Test
    public void tokenize_addFixedActivityArgs_matchesFieldParserFieldByField() throws Exception {
        // Equivalence check: for a realistic v1.0 "add" argument string, ArgumentTokenizer's
        // output must match what the existing FieldParser-based extraction already produces,
        // field by field - proving the new declarative path is a safe drop-in for this shape of
        // input, even though no v1.0 command has been switched over to it.
        String args = "n/CG3207 lecture c/ACADEMIC date/2026-08-10 type/FIXED from/1000 to/1200 "
                + "energy/LOW sensory/LOW topic/CG3207 note/bring laptop";

        Map<String, String> tokens = ArgumentTokenizer.tokenize(args,
                ArgumentMarker.required("n/"), ArgumentMarker.required("c/"), ArgumentMarker.required("date/"),
                ArgumentMarker.required("type/"), ArgumentMarker.required("from/"), ArgumentMarker.required("to/"),
                ArgumentMarker.required("energy/"), ArgumentMarker.required("sensory/"),
                ArgumentMarker.optional("topic/"), ArgumentMarker.optional("note/"));

        assertEquals(FieldParser.extractField(args, "n/", "c/"), tokens.get("n/"));
        assertEquals(FieldParser.extractField(args, "c/", "date/"), tokens.get("c/"));
        assertEquals(FieldParser.extractField(args, "date/", "type/"), tokens.get("date/"));
        assertEquals(FieldParser.extractField(args, "type/", "from/"), tokens.get("type/"));
        assertEquals(FieldParser.extractField(args, "from/", "to/"), tokens.get("from/"));
        assertEquals(FieldParser.extractField(args, "to/", "energy/"), tokens.get("to/"));
        assertEquals(FieldParser.extractField(args, "energy/", "sensory/"), tokens.get("energy/"));
        assertEquals(FieldParser.extractField(args, "sensory/", "topic/"), tokens.get("sensory/"));
        assertEquals(FieldParser.extractField(args, "topic/", "note/"), tokens.get("topic/"));
        assertEquals(FieldParser.extractField(args, "note/", null), tokens.get("note/"));
    }

    @Test
    public void tokenize_reorderedMarkers_matchesFieldParserRegardlessOfOrder() throws Exception {
        String args = "topic/CG3207 n/CG3207 lecture note/bring laptop c/ACADEMIC";

        Map<String, String> tokens = ArgumentTokenizer.tokenize(args,
                ArgumentMarker.required("n/"), ArgumentMarker.required("c/"),
                ArgumentMarker.optional("topic/"), ArgumentMarker.optional("note/"));

        assertEquals(FieldParser.extractField(args, "topic/", "n/"), tokens.get("topic/"));
        assertEquals(FieldParser.extractField(args, "n/", "note/"), tokens.get("n/"));
        assertEquals(FieldParser.extractField(args, "note/", "c/"), tokens.get("note/"));
        assertEquals(FieldParser.extractField(args, "c/", null), tokens.get("c/"));
    }

    @Test
    public void tokenize_markerThatIsTrailingSubstringOfAnotherMarker_isNotMistakenlyMatched() throws Exception {
        // "topic/" ends with "c/"; without boundary checking, "c/" could wrongly be matched
        // inside "topic/CS2113" and steal "CS2113" as a bogus value for c/ - the same regression
        // FieldParserTest guards against for the old extraction path.
        String args = "n/Test topic/CS2113";

        Map<String, String> tokens = ArgumentTokenizer.tokenize(args,
                ArgumentMarker.required("n/"), ArgumentMarker.optional("c/"), ArgumentMarker.optional("topic/"));

        assertFalse(tokens.containsKey("c/"));
        assertEquals("CS2113", tokens.get("topic/"));
    }

    @Test
    public void tokenize_quotedValue_preservesSpacesAndStripsQuotes() throws Exception {
        String args = "n/\"CG3207 lecture\" c/ACADEMIC";

        Map<String, String> tokens = ArgumentTokenizer.tokenize(args,
                ArgumentMarker.required("n/"), ArgumentMarker.required("c/"));

        assertEquals("CG3207 lecture", tokens.get("n/"));
        assertEquals("ACADEMIC", tokens.get("c/"));
    }

    @Test
    public void tokenize_quotedValueContainingMarkerLikeText_isNotSplit() throws Exception {
        String args = "note/\"see c/o front desk\" n/Meeting";

        Map<String, String> tokens = ArgumentTokenizer.tokenize(args,
                ArgumentMarker.required("n/"), ArgumentMarker.optional("note/"));

        assertEquals("see c/o front desk", tokens.get("note/"));
        assertEquals("Meeting", tokens.get("n/"));
    }

    @Test
    public void tokenize_quotedValueWithTrailingWhitespaceOnly_succeeds() throws Exception {
        String args = "n/\"CG3207 lecture\"   c/ACADEMIC";

        Map<String, String> tokens = ArgumentTokenizer.tokenize(args,
                ArgumentMarker.required("n/"), ArgumentMarker.required("c/"));

        assertEquals("CG3207 lecture", tokens.get("n/"));
        assertEquals("ACADEMIC", tokens.get("c/"));
    }

    @Test
    public void tokenize_quotedValueTrailingText_throwsInvalidCommandException() {
        String args = "n/\"CG3207 lecture\" garbage c/ACADEMIC";

        assertThrows(InvalidCommandException.class, () -> ArgumentTokenizer.tokenize(args,
                ArgumentMarker.required("n/"), ArgumentMarker.required("c/")));
    }

    @Test
    public void tokenize_quotedValueImmediatelyFollowedByText_throwsInvalidCommandException() {
        String args = "n/\"CG3207\"x c/ACADEMIC";

        assertThrows(InvalidCommandException.class, () -> ArgumentTokenizer.tokenize(args,
                ArgumentMarker.required("n/"), ArgumentMarker.required("c/")));
    }

    @Test
    public void tokenize_quotedValueTrailingTextAtEndOfInput_throwsInvalidCommandException() {
        String args = "n/\"CG3207 lecture\" garbage";

        assertThrows(InvalidCommandException.class, () -> ArgumentTokenizer.tokenize(args,
                ArgumentMarker.required("n/")));
    }

    @Test
    public void tokenize_unterminatedQuote_throwsInvalidCommandException() {
        String args = "n/\"CG3207 lecture c/ACADEMIC";

        assertThrows(InvalidCommandException.class, () -> ArgumentTokenizer.tokenize(args,
                ArgumentMarker.required("n/"), ArgumentMarker.required("c/")));
    }

    @Test
    public void tokenize_missingRequiredMarker_throwsMissingInputExceptionNamingIt() {
        String args = "n/CG3207 lecture";

        MissingInputException exception = assertThrows(MissingInputException.class, () ->
                ArgumentTokenizer.tokenize(args, ArgumentMarker.required("n/"), ArgumentMarker.required("c/")));
        assertTrue(exception.getMessage().contains("c/"));
    }

    @Test
    public void tokenize_missingOptionalMarker_omittedFromResult() throws Exception {
        String args = "n/CG3207 lecture c/ACADEMIC";

        Map<String, String> tokens = ArgumentTokenizer.tokenize(args,
                ArgumentMarker.required("n/"), ArgumentMarker.required("c/"), ArgumentMarker.optional("note/"));

        assertFalse(tokens.containsKey("note/"));
    }

    @Test
    public void tokenize_undeclaredMarkerShapedToken_throwsInvalidCommandException() {
        String args = "n/CG3207 x/oops c/ACADEMIC";

        assertThrows(InvalidCommandException.class, () -> ArgumentTokenizer.tokenize(args,
                ArgumentMarker.required("n/"), ArgumentMarker.required("c/")));
    }

    @Test
    public void tokenize_leadingUnrecognisedText_throwsInvalidCommandException() {
        String args = "oops n/CG3207 lecture c/ACADEMIC";

        assertThrows(InvalidCommandException.class, () -> ArgumentTokenizer.tokenize(args,
                ArgumentMarker.required("n/"), ArgumentMarker.required("c/")));
    }

    @Test
    public void tokenize_uppercaseMarkerInText_matchesDeclaredCaseKey() throws Exception {
        String args = "N/CG3207 lecture C/ACADEMIC";

        Map<String, String> tokens = ArgumentTokenizer.tokenize(args,
                ArgumentMarker.required("n/"), ArgumentMarker.required("c/"));

        assertEquals("CG3207 lecture", tokens.get("n/"));
        assertEquals("ACADEMIC", tokens.get("c/"));
    }

    @Test
    public void tokenize_duplicateDeclaredMarker_throwsInsteadOfLastValueWinning() {
        String args = "n/First n/Second";

        assertThrows(InvalidCommandException.class, () -> ArgumentTokenizer.tokenize(args,
                ArgumentMarker.required("n/")));
    }
}
