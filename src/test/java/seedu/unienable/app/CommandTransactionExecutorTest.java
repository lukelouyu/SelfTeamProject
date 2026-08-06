package seedu.unienable.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;

import seedu.unienable.command.Command;
import seedu.unienable.command.CommandEffect;
import seedu.unienable.command.CommandResult;
import seedu.unienable.exception.InvalidIndexException;
import seedu.unienable.logic.ActivityManager;
import seedu.unienable.logic.TopicManager;
import seedu.unienable.logic.preference.PreferenceManager;
import seedu.unienable.model.classes.EnergyRating;
import seedu.unienable.model.classes.FixedActivity;
import seedu.unienable.model.classes.SensoryRating;
import seedu.unienable.model.enums.ActivityCategory;

class CommandTransactionExecutorTest {
    @Test
    public void execute_runtimeExceptionAfterMutation_restoresCompletePreCommandState() throws Exception {
        ActivityManager activityManager = new ActivityManager();
        FixedActivity activity = new FixedActivity(1, "Lecture", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 17), LocalTime.of(9, 0), LocalTime.of(10, 0),
                EnergyRating.of(2), SensoryRating.of(2), null, null);
        activityManager.add(activity);
        TopicManager topicManager = new TopicManager(activityManager);
        PreferenceManager preferenceManager = new PreferenceManager();
        CommandTransactionExecutor executor = new CommandTransactionExecutor(
                activityManager, topicManager, preferenceManager);
        Command mutatesThenThrows = new Command() {
            @Override
            public CommandEffect getEffect() {
                return CommandEffect.MUTATING;
            }

            @Override
            public CommandResult execute() throws InvalidIndexException {
                activityManager.mark(1);
                throw new IllegalStateException("simulated failure after mutation");
            }
        };

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> executor.execute(mutatesThenThrows));

        assertEquals("simulated failure after mutation", exception.getMessage());
        assertFalse(activityManager.getById(1).isComplete());
        assertEquals(2, activityManager.getNextId());
    }
}
