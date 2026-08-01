package seedu.unienable.command.accessibility.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

class ValidationReportFormatterTest {
    @Test
    public void format_noWarnings_reportsNoIssuesFound() {
        assertEquals("facilities.txt: no issues found.", ValidationReportFormatter.format("facilities.txt",
                List.of()));
    }

    @Test
    public void format_oneWarning_usesSingularGrammar() {
        String formatted = ValidationReportFormatter.format("facilities.txt",
                List.of("Line 2 was skipped: blank id"));

        assertEquals("facilities.txt: 1 issue found:\nLine 2 was skipped: blank id", formatted);
    }

    @Test
    public void format_multipleWarnings_usesPluralGrammarAndListsEachOnItsOwnLine() {
        String formatted = ValidationReportFormatter.format("connections.txt",
                List.of("Line 1 was skipped: bad", "Line 3 was skipped: bad"));

        assertEquals("connections.txt: 2 issues found:\nLine 1 was skipped: bad\nLine 3 was skipped: bad",
                formatted);
    }
}
