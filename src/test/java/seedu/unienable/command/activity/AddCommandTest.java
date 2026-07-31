package seedu.unienable.command.activity;

import seedu.unienable.command.CommandResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;

import seedu.unienable.exception.DuplicateActivityException;
import seedu.unienable.logic.ActivityManager;
import seedu.unienable.model.classes.EnergyRating;
import seedu.unienable.model.classes.FixedActivity;
import seedu.unienable.model.classes.FlexibleActivity;
import seedu.unienable.model.classes.SensoryRating;
import seedu.unienable.model.enums.ActivityCategory;

class AddCommandTest {
    @Test
    public void execute_fixedActivity_addsAndFormatsConfirmation() throws Exception {
        ActivityManager manager = new ActivityManager();
        FixedActivity activity = new FixedActivity(manager.getNextId(), "CG3207 lecture", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(11, 0),
                EnergyRating.of(4), SensoryRating.of(3), "CG3207", null);

        CommandResult result = new AddCommand(manager, activity).execute();

        assertEquals(1, manager.size());
        String feedback = result.getFeedback();
        assertTrue(feedback.contains("Got it. Activity [1] has been added:"));
        assertTrue(feedback.contains("[ ][F] 2026-08-15 09:00 -> 11:00 | CG3207 lecture"));
        assertTrue(feedback.contains("ACADEMIC / CG3207 | Energy 4/5 | Sensory 3/5"));
        assertTrue(feedback.contains("You now have 1 activity."));
    }

    @Test
    public void execute_flexibleActivity_addsAndFormatsConfirmation() throws Exception {
        ActivityManager manager = new ActivityManager();
        FlexibleActivity activity = new FlexibleActivity(manager.getNextId(), "Finish assignment 1",
                ActivityCategory.ACADEMIC, LocalDate.of(2026, 8, 15), LocalTime.of(10, 0), LocalTime.of(18, 0),
                90, EnergyRating.of(5), SensoryRating.of(2), "CG3207", null);

        CommandResult result = new AddCommand(manager, activity).execute();

        String feedback = result.getFeedback();
        assertTrue(feedback.contains("[ ][L] 2026-08-15 10:00 -> 18:00 | Finish assignment 1"));
        assertTrue(feedback.contains("Duration 90 min | ACADEMIC / CG3207"));
        assertTrue(feedback.contains("Energy 5/5 | Sensory 2/5"));
    }

    @Test
    public void execute_noTopic_omitsTopicSuffix() throws Exception {
        ActivityManager manager = new ActivityManager();
        FixedActivity activity = new FixedActivity(manager.getNextId(), "Consultation", ActivityCategory.OTHERS,
                LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(10, 0),
                EnergyRating.of(2), SensoryRating.of(2), null, null);

        CommandResult result = new AddCommand(manager, activity).execute();

        assertTrue(result.getFeedback().contains("OTHERS | Energy 2/5"));
    }

    @Test
    public void execute_duplicateActivity_propagatesDuplicateActivityException() throws Exception {
        ActivityManager manager = new ActivityManager();
        FixedActivity first = new FixedActivity(manager.getNextId(), "CG3207 lecture", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(11, 0),
                EnergyRating.of(4), SensoryRating.of(3), null, null);
        new AddCommand(manager, first).execute();
        FixedActivity duplicate = new FixedActivity(manager.getNextId(), "CG3207 lecture", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(11, 0),
                EnergyRating.of(4), SensoryRating.of(3), null, null);

        assertThrows(DuplicateActivityException.class, () -> new AddCommand(manager, duplicate).execute());
    }
}
