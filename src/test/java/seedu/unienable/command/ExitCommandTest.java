package seedu.unienable.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ExitCommandTest {
    @Test
    public void execute_returnsGoodbyeMessageAndShouldExit() {
        CommandResult result = new ExitCommand().execute();

        assertEquals(ExitCommand.GOODBYE_MESSAGE, result.getFeedback());
        assertTrue(result.isShouldExit());
    }
}
