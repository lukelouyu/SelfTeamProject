package seedu.unienable.command.activity.general;

import seedu.unienable.command.CommandResult;

import static org.junit.jupiter.api.Assertions.assertFalse;
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

class UnmarkCommandTest {
    @Test
    public void execute_existingId_marksIncompleteAndFormatsConfirmation() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.add(new FlexibleActivity(manager.getNextId(), "Finish assignment 1", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(10, 0), LocalTime.of(18, 0), 90,
                EnergyRating.of(5), SensoryRating.of(2), null, null));
        manager.mark(1);

        CommandResult result = new UnmarkCommand(manager, 1).execute();

        assertFalse(manager.getById(1).isComplete());
        String feedback = result.getFeedback();
        assertTrue(feedback.contains("Activity [1] is now incomplete:"));
        assertTrue(feedback.contains("[ ][L] Finish assignment 1"));
    }

    @Test
    public void execute_alreadyIncomplete_isAllowed() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.add(new FlexibleActivity(manager.getNextId(), "Finish assignment 1", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(10, 0), LocalTime.of(18, 0), 90,
                EnergyRating.of(5), SensoryRating.of(2), null, null));

        CommandResult result = new UnmarkCommand(manager, 1).execute();

        assertFalse(manager.getById(1).isComplete());
        assertTrue(result.getFeedback().contains("Activity [1] is now incomplete:"));
    }

    @Test
    public void execute_unknownId_throwsInvalidIndexException() {
        ActivityManager manager = new ActivityManager();

        assertThrows(InvalidIndexException.class, () -> new UnmarkCommand(manager, 999).execute());
    }
}
