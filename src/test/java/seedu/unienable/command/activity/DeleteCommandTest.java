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
import seedu.unienable.model.classes.FixedActivity;
import seedu.unienable.model.classes.SensoryRating;
import seedu.unienable.model.enums.ActivityCategory;

class DeleteCommandTest {
    @Test
    public void execute_existingId_removesActivityAndFormatsConfirmation() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.add(new FixedActivity(manager.getNextId(), "Internship briefing", ActivityCategory.WORK_INTERNSHIP,
                LocalDate.of(2026, 8, 16), LocalTime.of(10, 0), LocalTime.of(11, 0),
                EnergyRating.of(3), SensoryRating.of(2), null, null));

        CommandResult result = new DeleteCommand(manager, 1).execute();

        assertEquals(0, manager.size());
        String feedback = result.getFeedback();
        assertTrue(feedback.contains("Activity [1] has been deleted."));
        assertTrue(feedback.contains("You now have 0 activities."));
    }

    @Test
    public void execute_oneActivityRemaining_usesSingularGrammar() throws Exception {
        // Regression test: the count suffix previously said "1 activities" unconditionally.
        ActivityManager manager = new ActivityManager();
        manager.add(new FixedActivity(manager.getNextId(), "Keep", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 16), LocalTime.of(9, 0), LocalTime.of(10, 0),
                EnergyRating.of(2), SensoryRating.of(2), null, null));
        manager.add(new FixedActivity(manager.getNextId(), "Remove", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 16), LocalTime.of(10, 0), LocalTime.of(11, 0),
                EnergyRating.of(2), SensoryRating.of(2), null, null));

        CommandResult result = new DeleteCommand(manager, 2).execute();

        assertTrue(result.getFeedback().contains("You now have 1 activity."));
    }

    @Test
    public void execute_unknownId_throwsInvalidIndexException() {
        ActivityManager manager = new ActivityManager();

        assertThrows(InvalidIndexException.class, () -> new DeleteCommand(manager, 999).execute());
    }

    @Test
    public void getId_returnsConstructorId() {
        DeleteCommand command = new DeleteCommand(new ActivityManager(), 7);

        assertEquals(7, command.getId());
    }
}
