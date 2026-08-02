package seedu.unienable.command.recommend;

import java.util.List;

import seedu.unienable.command.Command;
import seedu.unienable.command.CommandResult;
import seedu.unienable.exception.InvalidCommandException;
import seedu.unienable.logic.ActivityManager;
import seedu.unienable.logic.recommend.RecommendationManager;
import seedu.unienable.logic.recommend.RecommendationService;
import seedu.unienable.model.classes.Activity;
import seedu.unienable.model.recommend.RecommendationProposal;
import seedu.unienable.ui.recommend.RecommendationFormatter;

/** Re-displays the current in-memory recommendation proposal without recomputing it. */
public class RecommendViewCommand extends Command {
    private final ActivityManager activityManager;
    private final RecommendationManager recommendationManager;

    public RecommendViewCommand(ActivityManager activityManager, RecommendationManager recommendationManager) {
        this.activityManager = activityManager;
        this.recommendationManager = recommendationManager;
    }

    @Override
    public CommandResult execute() throws InvalidCommandException {
        RecommendationProposal proposal = recommendationManager.getProposal()
                .orElseThrow(() -> new InvalidCommandException(RecommendationFormatter.formatMissingProposal()));
        List<Activity> previewActivities = RecommendationService.applyPreview(activityManager.getAll(), proposal);
        return new CommandResult(RecommendationFormatter.formatPreview(proposal, previewActivities));
    }
}
