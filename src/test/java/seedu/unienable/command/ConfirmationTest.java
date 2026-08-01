package seedu.unienable.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ConfirmationTest {
    @Test
    public void proceed_isProceedOnlyAndHasNoMessage() {
        Confirmation confirmation = Confirmation.proceed();

        assertTrue(confirmation.isProceed());
        assertFalse(confirmation.isCancel());
        assertFalse(confirmation.isAsk());
        assertNull(confirmation.getMessage());
    }

    @Test
    public void cancel_isCancelOnlyAndCarriesMessage() {
        Confirmation confirmation = Confirmation.cancel("No changes to activity [1].");

        assertFalse(confirmation.isProceed());
        assertTrue(confirmation.isCancel());
        assertFalse(confirmation.isAsk());
        assertEquals("No changes to activity [1].", confirmation.getMessage());
    }

    @Test
    public void ask_isAskOnlyAndCarriesPrompt() {
        Confirmation confirmation = Confirmation.ask("Continue? (y/n)");

        assertFalse(confirmation.isProceed());
        assertFalse(confirmation.isCancel());
        assertTrue(confirmation.isAsk());
        assertEquals("Continue? (y/n)", confirmation.getMessage());
    }
}
