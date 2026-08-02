package seedu.unienable.command.recommend;

import seedu.unienable.command.Command;
import seedu.unienable.command.CommandResult;
import seedu.unienable.command.Confirmable;
import seedu.unienable.command.Confirmation;
import seedu.unienable.exception.InvalidCommandException;
import seedu.unienable.exception.InvalidIndexException;
import seedu.unienable.logic.ActivityManager;
import seedu.unienable.model.classes.Activity;
import seedu.unienable.model.classes.FlexibleActivity;
import seedu.unienable.model.recommend.RecommendationProposal;
import seedu.unienable.model.recommend.RecommendedPlacement;
import seedu.unienable.ui.recommend.RecommendationFormatter;

/** Adopts the current recommendation proposal after explicit confirmation. */
public class RecommendAdoptCommand extends Command implements Confirmable {
    private final ActivityManager activityManager;
    private final RecommendationProposal proposal;

    public RecommendAdoptCommand(ActivityManager activityManager, RecommendationProposal proposal) {
        this.activityManager = activityManager;
        this.proposal = proposal;
    }

    @Override
    public Confirmation getConfirmation() {
        if (!proposal.hasPlacements()) {
            return Confirmation.cancel("The current proposal has no placements to adopt.");
        }
        return Confirmation.ask("Adopt this recommendation?\nScheduled flexible activities: "
                + proposal.getPlacements().size() + "\nUnscheduled flexible activities: "
                + proposal.getUnscheduledActivityIds().size() + "\nProceed with adoption? (y/n)");
    }

    @Override
    public CommandResult execute() throws InvalidCommandException, InvalidIndexException {
        for (RecommendedPlacement placement : proposal.getPlacements()) {
            Activity activity = activityManager.getById(placement.activityId());
            if (!(activity instanceof FlexibleActivity)) {
                throw new InvalidCommandException("Activity [" + placement.activityId()
                        + "] is no longer an adoptable flexible activity.");
            }
            ((FlexibleActivity) activity).setAdoptedStartTime(placement.startTime());
        }
        return new CommandResult(RecommendationFormatter.formatAdoptSuccess(proposal));
    }
}
