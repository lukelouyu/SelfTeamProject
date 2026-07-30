package seedu.unienable.command.activity;

import seedu.unienable.command.CommandResult;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import seedu.unienable.logic.ActivityManager;
import seedu.unienable.model.enums.ActivityOrder;

class OrderViewCommandTest {
    @Test
    public void execute_defaultChronological_matchesGuideWording() {
        ActivityManager manager = new ActivityManager();

        CommandResult result = new OrderViewCommand(manager).execute();

        assertEquals("Saved default activity order: chronological\n"
                + "Activities are ordered by date, then start time, then ID.", result.getFeedback());
    }

    @Test
    public void execute_afterSettingInput_reflectsNewOrder() {
        ActivityManager manager = new ActivityManager();
        manager.setDefaultOrder(ActivityOrder.INPUT);

        CommandResult result = new OrderViewCommand(manager).execute();

        assertEquals("Saved default activity order: input\n"
                + "Activities are ordered by input (creation) order.", result.getFeedback());
    }
}
