package seedu.unienable.command.recommend;

import java.time.LocalDateTime;

import seedu.unienable.command.ReadOnlyCommand;
import seedu.unienable.command.CommandResult;
import seedu.unienable.exception.InvalidCommandException;
import seedu.unienable.logic.ActivityManager;
import seedu.unienable.logic.recommend.RecommendationManager;
import seedu.unienable.logic.recommend.RecommendationService;
import seedu.unienable.model.recommend.RecommendationPreview;
import seedu.unienable.model.recommend.RecommendationProposal;
import seedu.unienable.ui.recommend.RecommendationFormatter;

/** Re-displays the current in-memory recommendation proposal without recomputing it. */
public class RecommendViewCommand extends ReadOnlyCommand {
    private final ActivityManager activityManager;
    private final RecommendationManager recommendationManager;
    private final LocalDateTime now;

    /**
     * Creates a command that renders the active proposal against copied activity data.
     *
     * @param activityManager manager supplying the current activities
     * @param recommendationManager manager holding the active proposal
     * @param now the actual injected current date and time, used as the re-displayed dashboard's
     *     completion-eligibility basis - the same now a plain {@code dashboard} command would use
     */
    public RecommendViewCommand(ActivityManager activityManager, RecommendationManager recommendationManager,
            LocalDateTime now) {
        this.activityManager = activityManager;
        this.recommendationManager = recommendationManager;
        this.now = now;
    }

    @Override
    public CommandResult execute() throws InvalidCommandException {
        RecommendationProposal proposal = recommendationManager.getProposal()
                .orElseThrow(() -> new InvalidCommandException(RecommendationFormatter.formatMissingProposal()));
        RecommendationPreview preview = RecommendationService.buildPreview(activityManager, proposal, now);
        return new CommandResult(RecommendationFormatter.formatPreview(preview));
    }
}
