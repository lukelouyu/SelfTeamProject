package seedu.unienable.ui;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

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
}
