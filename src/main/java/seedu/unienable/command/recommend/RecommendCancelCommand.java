package seedu.unienable.command.recommend;

import seedu.unienable.command.Command;
import seedu.unienable.command.CommandResult;
import seedu.unienable.exception.InvalidCommandException;
import seedu.unienable.logic.recommend.RecommendationManager;
import seedu.unienable.ui.recommend.RecommendationFormatter;

/** Discards the current in-memory recommendation proposal without mutating persisted state. */
public class RecommendCancelCommand extends Command {
    private final RecommendationManager recommendationManager;

    public RecommendCancelCommand(RecommendationManager recommendationManager) {
        this.recommendationManager = recommendationManager;
    }

    @Override
    public CommandResult execute() throws InvalidCommandException {
        if (!recommendationManager.hasProposal()) {
            throw new InvalidCommandException(RecommendationFormatter.formatMissingProposal());
        }
        recommendationManager.clear();
        return new CommandResult(RecommendationFormatter.formatCancel());
    }
}
