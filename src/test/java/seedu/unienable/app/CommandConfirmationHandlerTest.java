package seedu.unienable.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import org.junit.jupiter.api.Test;

import seedu.unienable.command.CommandResult;
import seedu.unienable.command.Confirmable;
import seedu.unienable.command.Confirmation;
import seedu.unienable.command.MenuConfirmable;
import seedu.unienable.command.MenuOutcome;
import seedu.unienable.command.ReadOnlyCommand;
import seedu.unienable.exception.InvalidIndexException;
import seedu.unienable.exception.UniEnableException;
import seedu.unienable.ui.Ui;

/**
 * Exercises the generic {@link Confirmable}-driven flow using a locally-defined test command that
 * has no relationship to any real domain command - demonstrating the point of the OCP refactor:
 * a brand-new confirmable command type works correctly without CommandConfirmationHandler knowing
 * anything about it.
 */
class CommandConfirmationHandlerTest {
    private static final class NotConfirmable extends ReadOnlyCommand {
        @Override
        public CommandResult execute() {
            return new CommandResult("done");
        }
    }

    private static final class FixedConfirmation extends ReadOnlyCommand implements Confirmable {
        private final Confirmation confirmation;

        private FixedConfirmation(Confirmation confirmation) {
            this.confirmation = confirmation;
        }

        @Override
        public Confirmation getConfirmation() {
            return confirmation;
        }

        @Override
        public CommandResult execute() {
            return new CommandResult("done");
        }
    }

    private static final class ThrowsOnConfirmation extends ReadOnlyCommand implements Confirmable {
        @Override
        public Confirmation getConfirmation() throws UniEnableException {
            throw new InvalidIndexException("referenced ID does not exist.");
        }

        @Override
        public CommandResult execute() {
            return new CommandResult("done");
        }
    }

    // Its prompt is settable (including to null) so this one class covers both the normal menu
    // flow and the F3 null-prompt contract-violation guard.
    private static final class FixedMenu extends ReadOnlyCommand implements MenuConfirmable {
        private final String prompt;
        private final MenuOutcome outcome;

        private FixedMenu(String prompt, MenuOutcome outcome) {
            this.prompt = prompt;
            this.outcome = outcome;
        }

        @Override
        public String getMenuPrompt() {
            return prompt;
        }

        @Override
        public MenuOutcome applyMenuAnswer(String rawAnswer) {
            return outcome;
        }

        @Override
        public CommandResult execute() {
            return new CommandResult("done");
        }
    }

    private static String captureOutput(ThrowingRunnable runnable) throws Exception {
        ByteArrayOutputStream capturedOutput = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(capturedOutput, true, StandardCharsets.UTF_8));
        try {
            runnable.run();
        } finally {
            System.setOut(originalOut);
        }
        return capturedOutput.toString(StandardCharsets.UTF_8);
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    @Test
    public void confirmIfNeeded_notConfirmable_returnsTrueWithNoPromptShown() throws Exception {
        Scanner scanner = new Scanner(new ByteArrayInputStream(new byte[0]));
        boolean[] result = new boolean[1];

        String output = captureOutput(() -> {
            CommandConfirmationHandler handler = new CommandConfirmationHandler(new Ui(), scanner);
            result[0] = handler.confirmIfNeeded(new NotConfirmable());
        });

        assertTrue(result[0]);
        assertEquals("", output);
    }

    @Test
    public void confirmIfNeeded_proceed_returnsTrueWithNoPromptShown() throws Exception {
        Scanner scanner = new Scanner(new ByteArrayInputStream(new byte[0]));
        boolean[] result = new boolean[1];

        String output = captureOutput(() -> {
            CommandConfirmationHandler handler = new CommandConfirmationHandler(new Ui(), scanner);
            result[0] = handler.confirmIfNeeded(new FixedConfirmation(Confirmation.proceed()));
        });

        assertTrue(result[0]);
        assertEquals("", output);
    }

    @Test
    public void confirmIfNeeded_cancel_returnsFalseAndShowsOnlyTheGivenMessage() throws Exception {
        Scanner scanner = new Scanner(new ByteArrayInputStream(new byte[0]));
        boolean[] result = new boolean[1];

        String output = captureOutput(() -> {
            CommandConfirmationHandler handler = new CommandConfirmationHandler(new Ui(), scanner);
            result[0] = handler.confirmIfNeeded(new FixedConfirmation(Confirmation.cancel("No changes.")));
        });

        assertFalse(result[0]);
        assertTrue(output.contains("No changes."));
        assertFalse(output.contains("Cancelled. No changes were made."));
    }

    @Test
    public void confirmIfNeeded_askAnsweredY_returnsTrue() throws Exception {
        Scanner scanner = new Scanner(new ByteArrayInputStream("y\n".getBytes(StandardCharsets.UTF_8)));
        boolean[] result = new boolean[1];

        String output = captureOutput(() -> {
            CommandConfirmationHandler handler = new CommandConfirmationHandler(new Ui(), scanner);
            result[0] = handler.confirmIfNeeded(new FixedConfirmation(Confirmation.ask("Continue? (y/n)")));
        });

        assertTrue(result[0]);
        assertTrue(output.contains("Continue? (y/n)"));
    }

    @Test
    public void confirmIfNeeded_askAnsweredN_returnsFalseAndShowsCancelledMessage() throws Exception {
        Scanner scanner = new Scanner(new ByteArrayInputStream("n\n".getBytes(StandardCharsets.UTF_8)));
        boolean[] result = new boolean[1];

        String output = captureOutput(() -> {
            CommandConfirmationHandler handler = new CommandConfirmationHandler(new Ui(), scanner);
            result[0] = handler.confirmIfNeeded(new FixedConfirmation(Confirmation.ask("Continue? (y/n)")));
        });

        assertFalse(result[0]);
        assertTrue(output.contains("Cancelled. No changes were made."));
    }

    @Test
    public void confirmIfNeeded_askAtEndOfInput_defaultsToNAndReturnsFalse() throws Exception {
        Scanner scanner = new Scanner(new ByteArrayInputStream(new byte[0]));
        boolean[] result = new boolean[1];

        String output = captureOutput(() -> {
            CommandConfirmationHandler handler = new CommandConfirmationHandler(new Ui(), scanner);
            result[0] = handler.confirmIfNeeded(new FixedConfirmation(Confirmation.ask("Continue? (y/n)")));
        });

        assertFalse(result[0]);
        assertTrue(output.contains("Cancelled. No changes were made."));
    }

    @Test
    public void confirmIfNeeded_getConfirmationThrows_propagatesException() {
        Scanner scanner = new Scanner(new ByteArrayInputStream(new byte[0]));
        CommandConfirmationHandler handler = new CommandConfirmationHandler(new Ui(), scanner);

        assertThrows(InvalidIndexException.class, () -> handler.confirmIfNeeded(new ThrowsOnConfirmation()));
    }

    @Test
    public void confirmIfNeeded_menuProceeds_showsPromptAndReturnsTrue() throws Exception {
        Scanner scanner = new Scanner(new ByteArrayInputStream("1\n".getBytes(StandardCharsets.UTF_8)));
        boolean[] result = new boolean[1];

        String output = captureOutput(() -> {
            CommandConfirmationHandler handler = new CommandConfirmationHandler(new Ui(), scanner);
            result[0] = handler.confirmIfNeeded(new FixedMenu("Pick 1, 2, or 3:", MenuOutcome.proceed()));
        });

        assertTrue(result[0]);
        assertTrue(output.contains("Pick 1, 2, or 3:"));
    }

    @Test
    public void confirmIfNeeded_menuCancels_showsOutcomeMessageAndReturnsFalse() throws Exception {
        Scanner scanner = new Scanner(new ByteArrayInputStream("3\n".getBytes(StandardCharsets.UTF_8)));
        boolean[] result = new boolean[1];

        String output = captureOutput(() -> {
            CommandConfirmationHandler handler = new CommandConfirmationHandler(new Ui(), scanner);
            result[0] = handler.confirmIfNeeded(
                    new FixedMenu("Pick 1, 2, or 3:", MenuOutcome.cancel("Cancelled. No changes were made.")));
        });

        assertFalse(result[0]);
        assertTrue(output.contains("Cancelled. No changes were made."));
    }

    @Test
    public void confirmIfNeeded_menuPromptNull_throwsInsteadOfSilentlyAutoConfirming() {
        // F3 (regression audit, 2026-08-16): getMenuPrompt() returning null used to mean "nothing
        // to confirm, proceed immediately" - the exact mechanism DEFECT-01 exploited. The contract
        // is now non-null; a command that violates it must fail loudly here, not silently bypass
        // confirmation the way the removed null-check used to.
        Scanner scanner = new Scanner(new ByteArrayInputStream(new byte[0]));
        CommandConfirmationHandler handler = new CommandConfirmationHandler(new Ui(), scanner);

        assertThrows(NullPointerException.class,
                () -> handler.confirmIfNeeded(new FixedMenu(null, MenuOutcome.proceed())));
    }
}
