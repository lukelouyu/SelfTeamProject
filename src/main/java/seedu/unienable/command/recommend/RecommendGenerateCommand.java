package seedu.unienable.command.recommend;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import seedu.unienable.command.Command;
import seedu.unienable.command.CommandResult;
import seedu.unienable.logic.ActivityManager;
import seedu.unienable.logic.preference.PreferenceManager;
import seedu.unienable.logic.recommend.RecommendationManager;
import seedu.unienable.logic.recommend.RecommendationService;
import seedu.unienable.model.classes.Activity;
import seedu.unienable.model.recommend.RecommendationProposal;
import seedu.unienable.ui.recommend.RecommendationFormatter;

/** Generates and stores one in-memory recommendation preview without persisting it. */
public class RecommendGenerateCommand extends Command {
    private final ActivityManager activityManager;
    private final PreferenceManager preferenceManager;
    private final RecommendationManager recommendationManager;
    private final LocalDateTime now;
    private final LocalDate date;

    public RecommendGenerateCommand(ActivityManager activityManager, PreferenceManager preferenceManager,
            RecommendationManager recommendationManager, LocalDateTime now, LocalDate date) {
        this.activityManager = activityManager;
        this.preferenceManager = preferenceManager;
        this.recommendationManager = recommendationManager;
        this.now = now;
        this.date = date;
    }

    @Override
    public CommandResult execute() {
        RecommendationProposal proposal = date == null
                ? RecommendationService.recommendThisWeek(activityManager, preferenceManager.getProfile(), now)
                : RecommendationService.recommendDate(activityManager, preferenceManager.getProfile(), date);
        recommendationManager.setProposal(proposal);
        List<Activity> previewActivities = RecommendationService.applyPreview(activityManager.getAll(), proposal);
        return new CommandResult(RecommendationFormatter.formatPreview(proposal, previewActivities));
    }
}
