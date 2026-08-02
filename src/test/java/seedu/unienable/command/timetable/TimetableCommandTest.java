package seedu.unienable.command.timetable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.unienable.logic.ActivityManager;
import seedu.unienable.logic.timetable.TimetableService;
import seedu.unienable.model.classes.EnergyRating;
import seedu.unienable.model.classes.FixedActivity;
import seedu.unienable.model.classes.SensoryRating;
import seedu.unienable.model.enums.ActivityCategory;
import seedu.unienable.model.enums.ActivityOrder;
import seedu.unienable.model.timetable.TimetableMode;

class TimetableCommandTest {
    private static final LocalDate MONDAY = LocalDate.of(2026, 8, 17);

    @Test
    public void execute_emptyPeriod_returnsHelpfulTimetable() {
        TimetableCommand command = new TimetableCommand(new ActivityManager(),
                TimetableService.resolveDay(MONDAY), TimetableMode.NORMAL);

        String feedback = command.execute().getFeedback();

        assertTrue(feedback.startsWith("Daily Timetable"));
        assertTrue(feedback.contains("No fixed activities."));
    }

    @Test
    public void execute_neverMutatesManagerOrActivity() throws Exception {
        FixedActivity activity = new FixedActivity(1, "Lecture", ActivityCategory.ACADEMIC,
                MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0), EnergyRating.of(2),
                SensoryRating.of(3), null, null);
        ActivityManager manager = new ActivityManager();
        manager.loadAll(List.of(activity));
        int nextIdBefore = manager.getNextId();
        ActivityOrder orderBefore = manager.getDefaultOrder();
        TimetableCommand command = new TimetableCommand(manager,
                TimetableService.resolveDay(MONDAY), TimetableMode.DETAIL);

        command.execute();

        assertEquals(1, manager.size());
        assertEquals(nextIdBefore, manager.getNextId());
        assertEquals(orderBefore, manager.getDefaultOrder());
        assertEquals("Lecture", activity.getDescription());
        assertTrue(!activity.isComplete());
    }
}
