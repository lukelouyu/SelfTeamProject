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
}
