package seedu.unienable.command;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import seedu.unienable.logic.ActivityManager;
import seedu.unienable.model.enums.ActivityOrder;

class OrderSetCommandTest {
    @Test
    public void execute_input_updatesManagerAndMatchesGuideWording() {
        ActivityManager manager = new ActivityManager();

        CommandResult result = new OrderSetCommand(manager, ActivityOrder.INPUT).execute();

        assertEquals(ActivityOrder.INPUT, manager.getDefaultOrder());
        assertEquals("Default activity order updated: input\n"
                + "Future list and find results will use creation order.", result.getFeedback());
    }

    @Test
    public void execute_time_matchesGuideWording() {
        ActivityManager manager = new ActivityManager();

        CommandResult result = new OrderSetCommand(manager, ActivityOrder.TIME).execute();

        assertEquals("Default activity order updated: time\n"
                + "Future results will use time, then stable activity ID.", result.getFeedback());
    }

    @Test
    public void execute_chronological_matchesGuideWording() {
        ActivityManager manager = new ActivityManager();

        CommandResult result = new OrderSetCommand(manager, ActivityOrder.CHRONOLOGICAL).execute();

        assertEquals("Default activity order updated: chronological\n"
                + "Future results will use date, time, then stable activity ID.", result.getFeedback());
    }
}
