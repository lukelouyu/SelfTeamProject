package seedu.unienable.command.recur;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.unienable.command.CommandResult;
import seedu.unienable.exception.DuplicateActivityException;
import seedu.unienable.logic.ActivityManager;
import seedu.unienable.logic.recur.RecurrencePlanner;
import seedu.unienable.model.classes.FixedActivity;
import seedu.unienable.model.recur.RecurrencePlan;
import seedu.unienable.testutil.recur.RecurrenceTestData;

class RecurCommandTest {
    @Test
    public void execute_sparsePlan_addsAllOccurrencesWithSequentialIds() throws Exception {
        ActivityManager manager = new ActivityManager();
        FixedActivity source = RecurrenceTestData.cg3201Lab(manager.getNextId());
        manager.add(source);
        RecurrencePlan plan = new RecurrencePlanner().plan(source, List.of(3, 7, 9, 11),
                RecurrenceTestData.calendar(), manager);

        CommandResult result = new RecurCommand(manager, plan).execute();

        assertEquals(4, manager.size());
        assertEquals(5, manager.getNextId());
        assertEquals(2, manager.getAll().get(1).getId());
        assertEquals(4, manager.getAll().get(3).getId());
        assertTrue(result.getFeedback().contains("Created 3 recurring sessions"));
    }

    @Test
    public void planAgain_afterExecution_hasNothingToCreateAndConsumesNoId() throws Exception {
        ActivityManager manager = new ActivityManager();
        FixedActivity source = RecurrenceTestData.cg3201Lab(manager.getNextId());
        manager.add(source);
        RecurrencePlanner planner = new RecurrencePlanner();
        RecurrencePlan first = planner.plan(source, List.of(3, 7, 9, 11),
                RecurrenceTestData.calendar(), manager);
        new RecurCommand(manager, first).execute();
        int nextId = manager.getNextId();

        RecurrencePlan repeated = planner.plan(source, List.of(3, 7, 9, 11),
                RecurrenceTestData.calendar(), manager);

        assertTrue(!repeated.hasOccurrencesToCreate());
        assertEquals(nextId, manager.getNextId());
    }

    @Test
    public void execute_managerChangesAfterPlanning_revalidatesAndAddsNothing() throws Exception {
        ActivityManager manager = new ActivityManager();
        FixedActivity source = RecurrenceTestData.cg3201Lab(manager.getNextId());
        manager.add(source);
        RecurrencePlan plan = new RecurrencePlanner().plan(source, List.of(3, 7, 9, 11),
                RecurrenceTestData.calendar(), manager);
        int nextIdBeforeExecution = manager.getNextId();
        source.setDate(plan.getOccurrencesToCreate().get(0).getActivity().getDate());

        DuplicateActivityException exception = assertThrows(DuplicateActivityException.class,
                () -> new RecurCommand(manager, plan).execute());

        assertEquals("An identical activity already exists.", exception.getMessage());
        assertEquals(1, manager.size());
        assertEquals(nextIdBeforeExecution, manager.getNextId());
    }
}
