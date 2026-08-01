package seedu.unienable.command.recur;

import java.util.ArrayList;
import java.util.List;

import seedu.unienable.command.Command;
import seedu.unienable.command.CommandResult;
import seedu.unienable.command.Confirmable;
import seedu.unienable.command.Confirmation;
import seedu.unienable.exception.DuplicateActivityException;
import seedu.unienable.logic.ActivityManager;
import seedu.unienable.model.classes.FixedActivity;
import seedu.unienable.model.recur.RecurrencePlan;
import seedu.unienable.model.recur.RecurrencePlan.PlannedOccurrence;
import seedu.unienable.ui.recur.RecurrenceFormatter;

/** Adds every prevalidated occurrence in a recurrence plan as an independent fixed activity. */
public class RecurCommand extends Command implements Confirmable {
    private final ActivityManager activityManager;
    private final RecurrencePlan plan;

    /** Creates a recurrence command from a side-effect-free, prevalidated plan. */
    public RecurCommand(ActivityManager activityManager, RecurrencePlan plan) {
        this.activityManager = activityManager;
        this.plan = plan;
    }

    /** Returns the complete plan for confirmation and formatting. */
    public RecurrencePlan getPlan() {
        return plan;
    }

    @Override
    public Confirmation getConfirmation() {
        if (!plan.hasOccurrencesToCreate()) {
            return Confirmation.cancel(RecurrenceFormatter.formatNothingToCreate(plan));
        }
        return Confirmation.ask(RecurrenceFormatter.formatPreview(plan));
    }

    @Override
    public CommandResult execute() throws DuplicateActivityException {
        List<FixedActivity> activities = new ArrayList<>();
        for (PlannedOccurrence occurrence : plan.getOccurrencesToCreate()) {
            activities.add(occurrence.getActivity());
        }
        activityManager.addAllAtomically(activities);
        return new CommandResult(RecurrenceFormatter.formatSuccess(plan));
    }
}
