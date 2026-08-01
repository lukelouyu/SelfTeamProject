package seedu.unienable.command.activity.general;

import seedu.unienable.command.CommandResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;

import seedu.unienable.logic.ActivityManager;
import seedu.unienable.model.classes.EnergyRating;
import seedu.unienable.model.classes.FixedActivity;
import seedu.unienable.model.classes.SensoryRating;
import seedu.unienable.model.enums.ActivityCategory;

class NextCommandTest {
    @Test
    public void execute_hasNextActivity_formatsItWithOverdueCount() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.add(new FixedActivity(manager.getNextId(), "CG3207 lecture", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(11, 0),
                EnergyRating.of(4), SensoryRating.of(3), "CG3207", null));
        LocalDateTime now = LocalDateTime.of(2026, 8, 15, 10, 0);

        CommandResult result = new NextCommand(manager, now).execute();

        String feedback = result.getFeedback();
        assertTrue(feedback.contains("Your next relevant activity is:"));
        assertTrue(feedback.contains("CG3207 lecture"));
        assertTrue(feedback.contains("Overdue incomplete activities: 0"));
    }

    @Test
    public void execute_noQualifyingActivity_reportsNoUpcoming() {
        ActivityManager manager = new ActivityManager();
        LocalDateTime now = LocalDateTime.of(2026, 8, 15, 9, 0);

        CommandResult result = new NextCommand(manager, now).execute();

        assertEquals("You have no upcoming relevant activities.\n\nOverdue incomplete activities: 0",
                result.getFeedback());
    }

    @Test
    public void execute_reportsOverdueCount() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.add(new FixedActivity(manager.getNextId(), "Missed", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(6, 0), LocalTime.of(7, 0),
                EnergyRating.of(3), SensoryRating.of(2), null, null));
        LocalDateTime now = LocalDateTime.of(2026, 8, 15, 9, 0);

        CommandResult result = new NextCommand(manager, now).execute();

        assertTrue(result.getFeedback().contains("Overdue incomplete activities: 1"));
    }
}
