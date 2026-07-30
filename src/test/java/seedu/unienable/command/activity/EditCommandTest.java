package seedu.unienable.command.activity;

import seedu.unienable.command.CommandResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;

import seedu.unienable.exception.DuplicateActivityException;
import seedu.unienable.exception.InvalidIndexException;
import seedu.unienable.logic.ActivityManager;
import seedu.unienable.model.classes.EnergyRating;
import seedu.unienable.model.classes.FixedActivity;
import seedu.unienable.model.classes.SensoryRating;
import seedu.unienable.model.enums.ActivityCategory;

class EditCommandTest {
    private static FixedActivity newFixedActivity(int id, String description, LocalTime start, LocalTime end)
            throws Exception {
        return new FixedActivity(id, description, ActivityCategory.ACADEMIC, LocalDate.of(2026, 8, 15),
                start, end, EnergyRating.of(4), SensoryRating.of(3), null, null);
    }

    @Test
    public void execute_validReplacement_updatesActivityAndFormatsConfirmation() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.add(newFixedActivity(manager.getNextId(), "Old", LocalTime.of(9, 0), LocalTime.of(10, 0)));
        FixedActivity replacement = newFixedActivity(1, "New", LocalTime.of(9, 0), LocalTime.of(10, 0));

        CommandResult result = new EditCommand(manager, 1, replacement).execute();

        assertEquals("New", manager.getById(1).getDescription());
        assertTrue(result.getFeedback().contains("Activity [1] has been updated."));
    }

    @Test
    public void execute_unknownId_throwsInvalidIndexException() throws Exception {
        ActivityManager manager = new ActivityManager();
        FixedActivity replacement = newFixedActivity(999, "Ghost", LocalTime.of(9, 0), LocalTime.of(10, 0));

        assertThrows(InvalidIndexException.class, () -> new EditCommand(manager, 999, replacement).execute());
    }

    @Test
    public void execute_replacementOverlapsAnother_throwsDuplicateActivityException() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.add(newFixedActivity(manager.getNextId(), "First", LocalTime.of(9, 0), LocalTime.of(10, 0)));
        manager.add(newFixedActivity(manager.getNextId(), "Second", LocalTime.of(14, 0), LocalTime.of(15, 0)));
        FixedActivity replacement = newFixedActivity(2, "Second", LocalTime.of(9, 30), LocalTime.of(10, 30));

        assertThrows(DuplicateActivityException.class, () -> new EditCommand(manager, 2, replacement).execute());
    }

    @Test
    public void getId_returnsConstructorId() throws Exception {
        FixedActivity replacement = newFixedActivity(5, "New", LocalTime.of(9, 0), LocalTime.of(10, 0));

        EditCommand command = new EditCommand(new ActivityManager(), 5, replacement);

        assertEquals(5, command.getId());
    }

    @Test
    public void getNewActivity_returnsConstructorReplacement() throws Exception {
        FixedActivity replacement = newFixedActivity(5, "New", LocalTime.of(9, 0), LocalTime.of(10, 0));

        EditCommand command = new EditCommand(new ActivityManager(), 5, replacement);

        assertEquals(replacement, command.getNewActivity());
    }
}
