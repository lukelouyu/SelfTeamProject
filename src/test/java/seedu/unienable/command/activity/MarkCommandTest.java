package seedu.unienable.command.activity;

import seedu.unienable.command.CommandResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;

import seedu.unienable.exception.InvalidIndexException;
import seedu.unienable.logic.ActivityManager;
import seedu.unienable.model.classes.EnergyRating;
import seedu.unienable.model.classes.FlexibleActivity;
import seedu.unienable.model.classes.SensoryRating;
import seedu.unienable.model.enums.ActivityCategory;

class MarkCommandTest {
    @Test
    public void execute_existingId_marksCompleteAndFormatsConfirmation() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.add(new FlexibleActivity(manager.getNextId(), "Finish assignment 1", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(10, 0), LocalTime.of(18, 0), 90,
                EnergyRating.of(5), SensoryRating.of(2), null, null));

        CommandResult result = new MarkCommand(manager, 1).execute();

        assertTrue(manager.getById(1).isComplete());
        String feedback = result.getFeedback();
        assertTrue(feedback.contains("Nice! Activity [1] is now complete:"));
        assertTrue(feedback.contains("[X][L] Finish assignment 1"));
    }

    @Test
    public void execute_alreadyComplete_isAllowed() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.add(new FlexibleActivity(manager.getNextId(), "Finish assignment 1", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(10, 0), LocalTime.of(18, 0), 90,
                EnergyRating.of(5), SensoryRating.of(2), null, null));
        new MarkCommand(manager, 1).execute();

        CommandResult result = new MarkCommand(manager, 1).execute();

        assertEquals(true, manager.getById(1).isComplete());
        assertTrue(result.getFeedback().contains("Nice! Activity [1] is now complete:"));
    }

    @Test
    public void execute_unknownId_throwsInvalidIndexException() {
        ActivityManager manager = new ActivityManager();

        assertThrows(InvalidIndexException.class, () -> new MarkCommand(manager, 999).execute());
    }
}
