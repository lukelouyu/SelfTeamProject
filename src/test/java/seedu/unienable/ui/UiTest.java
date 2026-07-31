package seedu.unienable.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;

import org.junit.jupiter.api.Test;

class UiTest {
    @Test
    public void showWelcome_printsUniEnableGreeting() {
        ByteArrayOutputStream capturedOutput = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(capturedOutput));

        try {
            new Ui().showWelcome();
        } finally {
            System.setOut(originalOut);
        }

        String output = capturedOutput.toString();
        assertTrue(output.contains("Welcome to UniEnable"));
        assertTrue(output.contains("Uni Friend"));
        assertTrue(output.contains("guide"));
    }

    @Test
    public void showFramed_wrapsTextBetweenHorizontalLines() {
        ByteArrayOutputStream capturedOutput = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(capturedOutput));

        try {
            new Ui().showFramed("Activity [1] has been deleted.");
        } finally {
            System.setOut(originalOut);
        }

        String[] lines = capturedOutput.toString().split("\\r?\\n");
        assertTrue(lines[0].matches("_+"));
        assertTrue(lines[1].equals("Activity [1] has been deleted."));
        assertTrue(lines[2].matches("_+"));
    }

    @Test
    public void showLoadWarnings_hasWarnings_showsFramedFileNameAndEachWarning() {
        ByteArrayOutputStream capturedOutput = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(capturedOutput));

        try {
            new Ui().showLoadWarnings("activities.txt", List.of("Line 2 was skipped: invalid category"));
        } finally {
            System.setOut(originalOut);
        }

        String[] lines = capturedOutput.toString().split("\\r?\\n");
        assertTrue(lines[0].matches("_+"));
        assertEquals("[Warning] Partial data loaded: activities.txt", lines[1]);
        assertEquals("Line 2 was skipped: invalid category", lines[2]);
        assertTrue(lines[3].matches("_+"));
    }

    @Test
    public void showLoadWarnings_multipleWarnings_showsOneLinePerWarning() {
        ByteArrayOutputStream capturedOutput = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(capturedOutput));

        try {
            new Ui().showLoadWarnings("topics.txt", List.of("Line 1 was skipped: bad", "Line 3 was skipped: bad"));
        } finally {
            System.setOut(originalOut);
        }

        String[] lines = capturedOutput.toString().split("\\r?\\n");
        assertEquals("[Warning] Partial data loaded: topics.txt", lines[1]);
        assertEquals("Line 1 was skipped: bad", lines[2]);
        assertEquals("Line 3 was skipped: bad", lines[3]);
    }

    @Test
    public void showLoadWarnings_noWarnings_showsNothing() {
        ByteArrayOutputStream capturedOutput = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(capturedOutput));

        try {
            new Ui().showLoadWarnings("settings.txt", List.of());
        } finally {
            System.setOut(originalOut);
        }

        assertEquals("", capturedOutput.toString());
    }
}
