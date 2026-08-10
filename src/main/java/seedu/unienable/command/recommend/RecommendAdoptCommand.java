package seedu.unienable.command.recommend;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import seedu.unienable.command.Command;
import seedu.unienable.command.CommandEffect;
import seedu.unienable.command.CommandResult;
import seedu.unienable.command.Confirmable;
import seedu.unienable.command.Confirmation;
import seedu.unienable.command.PreExecutionValidatable;
import seedu.unienable.exception.InvalidCommandException;
import seedu.unienable.exception.InvalidIndexException;
import seedu.unienable.logic.ActivityManager;
import seedu.unienable.logic.recommend.RecommendationService;
import seedu.unienable.model.classes.Activity;
import seedu.unienable.model.classes.FlexibleActivity;
import seedu.unienable.model.recommend.RecommendationProposal;
import seedu.unienable.model.recommend.RecommendedPlacement;
import seedu.unienable.ui.recommend.RecommendationFormatter;

/** Adopts the current recommendation proposal after explicit confirmation. */
public class RecommendAdoptCommand extends Command implements Confirmable, PreExecutionValidatable {
    private final ActivityManager activityManager;
    private final RecommendationProposal proposal;

    /**
     * Creates a command that adopts the supplied proposal into the activity manager.
     *
     * @param activityManager the manager containing the proposed activities
     * @param proposal the immutable proposal to adopt
     */
    public RecommendAdoptCommand(ActivityManager activityManager, RecommendationProposal proposal) {
        this.activityManager = activityManager;
        this.proposal = proposal;
    }

    @Override
    public CommandEffect getEffect() {
        return CommandEffect.MUTATING;
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

    /**
     * Re-checks staleness against a freshly-sampled {@code now}, since {@code
     * RecommendCommandParser} only checked this once at dispatch time, before the confirmation
     * prompt was shown - real time may have advanced past one or more proposed starts while the
     * user was still answering "Proceed with adoption? (y/n)".
     *
     * @throws InvalidCommandException if the proposal has gone stale since it was parsed
     */
    @Override
    public void validateBeforeExecution(LocalDateTime now) throws InvalidCommandException {
        if (RecommendationService.hasElapsedPlacement(proposal, now)) {
            throw new InvalidCommandException("This recommendation proposal is stale - one or more "
                    + "proposed start times have already passed. Generate a new recommendation with "
                    + "recommend.");
        }
    }

    @Override
    public CommandResult execute() throws InvalidCommandException, InvalidIndexException {
        List<AdoptionTarget> targets = new ArrayList<>();
        Set<Integer> seenActivityIds = new HashSet<>();
        for (RecommendedPlacement placement : proposal.getPlacements()) {
            if (!seenActivityIds.add(placement.activityId())) {
                throw new InvalidCommandException("The recommendation contains more than one placement for "
                        + "activity [" + placement.activityId() + "].");
            }
            Activity activity = activityManager.getById(placement.activityId());
            if (!(activity instanceof FlexibleActivity)) {
                throw new InvalidCommandException("Activity [" + placement.activityId()
                        + "] is no longer an adoptable flexible activity.");
            }
            FlexibleActivity flexible = (FlexibleActivity) activity;
            if (!activity.getDate().equals(placement.date())
                    || placement.startTime() == null
                    || placement.endTime() == null
                    || !placement.startTime().plusMinutes(flexible.getDurationMinutes())
                            .equals(placement.endTime())
                    || !flexible.canAdoptAt(placement.startTime())) {
                throw new InvalidCommandException("The proposed placement for activity ["
                        + placement.activityId() + "] no longer fits its flexible window.");
            }
            targets.add(new AdoptionTarget(flexible, placement.startTime()));
        }
        for (AdoptionTarget target : targets) {
            target.activity().setAdoptedStartTime(target.startTime());
        }
        return new CommandResult(RecommendationFormatter.formatAdoptSuccess(proposal));
    }

    private record AdoptionTarget(FlexibleActivity activity, java.time.LocalTime startTime) {
    }
}
