package seedu.unienable.command.activity.crud;

import java.time.LocalDateTime;
import java.time.LocalTime;

import seedu.unienable.command.Command;
import seedu.unienable.command.CommandEffect;
import seedu.unienable.command.CommandResult;
import seedu.unienable.command.Confirmable;
import seedu.unienable.command.Confirmation;
import seedu.unienable.command.PreExecutionValidatable;

import seedu.unienable.exception.DuplicateActivityException;
import seedu.unienable.exception.InvalidDateTimeException;
import seedu.unienable.exception.InvalidIndexException;
import seedu.unienable.logic.ActivityManager;
import seedu.unienable.logic.validation.ActivityTimeValidator;
import seedu.unienable.model.classes.Activity;
import seedu.unienable.model.classes.FixedActivity;
import seedu.unienable.model.classes.FlexibleActivity;
import seedu.unienable.ui.MessageFormatter;

/**
 * Replaces an activity with a fully-rebuilt version reflecting the requested field changes. The
 * replacement is expected to already carry the same ID and the original completion status, so
 * this command only needs to hand it to ActivityManager.replace() for atomic validation and swap.
 */
public class EditCommand extends Command implements Confirmable, PreExecutionValidatable {
    private final ActivityManager activityManager;
    private final int id;
    private final Activity newActivity;
    private final boolean requiresFreshStartTimeCheck;

    /**
     * Creates an EditCommand.
     *
     * @param activityManager the manager holding the activity to replace
     * @param id the stable ID of the activity to replace
     * @param newActivity the fully-rebuilt replacement, carrying the same ID and completion status
     * @param requiresFreshStartTimeCheck whether this edit actively supplied a new date/start
     *     time, so {@link #validateBeforeExecution} must re-check "not in the past" against a
     *     freshly-sampled {@code now} - as opposed to an edit that leaves timing untouched, whose
     *     (unchanged, possibly already-past) carried-over start time must never be rejected
     */
    public EditCommand(ActivityManager activityManager, int id, Activity newActivity,
            boolean requiresFreshStartTimeCheck) {
        this.activityManager = activityManager;
        this.id = id;
        this.newActivity = newActivity;
        this.requiresFreshStartTimeCheck = requiresFreshStartTimeCheck;
    }

    @Override
    public CommandEffect getEffect() {
        return CommandEffect.MUTATING;
    }

    /** Returns the stable ID of the activity this command will update, for the UI loop's preview. */
    public int getId() {
        return id;
    }

    /** Returns the proposed replacement activity, for the UI loop's preview. */
    public Activity getNewActivity() {
        return newActivity;
    }

    @Override
    public Confirmation getConfirmation() throws InvalidIndexException {
        Activity oldActivity = activityManager.getById(id);
        String diff = MessageFormatter.formatChanges(oldActivity, newActivity);
        if (diff.isEmpty()) {
            return Confirmation.cancel("No changes to activity [" + id + "].");
        }
        return Confirmation.ask(diff + "\nSave changes? (y/n)");
    }

    /**
     * Re-checks "not in the past" against a freshly-sampled {@code now}, but only when this edit
     * actively supplied a new date/start-time value - {@code EditCommandParser} only checked this
     * once at dispatch time, before the confirmation prompt was shown, and real time may have
     * advanced past the newly-requested start while the user was still answering "Save changes?
     * (y/n)". An edit that never touches timing is never re-checked, since its (unchanged) start
     * time carrying over from the original activity is allowed to already be in the past.
     *
     * @throws InvalidDateTimeException if the actively-supplied start time is no longer after now
     */
    @Override
    public void validateBeforeExecution(LocalDateTime now) throws InvalidDateTimeException {
        if (!requiresFreshStartTimeCheck) {
            return;
        }
        LocalTime start = newActivity instanceof FixedActivity
                ? ((FixedActivity) newActivity).getStartTime()
                : ((FlexibleActivity) newActivity).getEarliestStart();
        ActivityTimeValidator.requireNotPastIfToday(start, newActivity.getDate(), now);
    }

    @Override
    public CommandResult execute() throws InvalidIndexException, DuplicateActivityException {
        activityManager.replace(id, newActivity);
        return new CommandResult("Activity [" + id + "] has been updated.");
    }
}
