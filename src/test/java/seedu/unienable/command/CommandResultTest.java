package seedu.unienable.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CommandResultTest {
    @Test
    public void twoArgConstructor_setsFeedbackAndShouldExit() {
        CommandResult result = new CommandResult("done", true);

        assertEquals("done", result.getFeedback());
        assertTrue(result.isShouldExit());
    }

    @Test
    public void oneArgConstructor_defaultsShouldExitToFalse() {
        CommandResult result = new CommandResult("done");

        assertEquals("done", result.getFeedback());
        assertFalse(result.isShouldExit());
    }
}
