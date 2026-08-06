package seedu.unienable.command.recommend;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import seedu.unienable.command.ReadOnlyCommand;
import seedu.unienable.command.CommandResult;
import seedu.unienable.logic.ActivityManager;
import seedu.unienable.logic.preference.PreferenceManager;
import seedu.unienable.logic.recommend.RecommendationManager;
import seedu.unienable.logic.recommend.RecommendationService;
import seedu.unienable.model.classes.Activity;
import seedu.unienable.model.recommend.RecommendationProposal;
import seedu.unienable.ui.recommend.RecommendationFormatter;

/** Generates and stores one in-memory recommendation preview without persisting it. */
public class RecommendGenerateCommand extends ReadOnlyCommand {
    private final ActivityManager activityManager;
    private final PreferenceManager preferenceManager;
    private final RecommendationManager recommendationManager;
    private final LocalDateTime now;
    private final LocalDate date;
    private final boolean nextWeek;

    /** Creates a command for a this-week (date null) or one-day (date supplied) proposal. */
    public RecommendGenerateCommand(ActivityManager activityManager, PreferenceManager preferenceManager,
            RecommendationManager recommendationManager, LocalDateTime now, LocalDate date) {
        this(activityManager, preferenceManager, recommendationManager, now, date, false);
    }

    /**
     * Creates a command for a this-week, next-week, or one-day proposal. date takes priority over
     * nextWeek if both are somehow supplied; nextWeek is only consulted when date is null.
     */
    public RecommendGenerateCommand(ActivityManager activityManager, PreferenceManager preferenceManager,
            RecommendationManager recommendationManager, LocalDateTime now, LocalDate date, boolean nextWeek) {
        this.activityManager = activityManager;
        this.preferenceManager = preferenceManager;
        this.recommendationManager = recommendationManager;
        this.now = now;
        this.date = date;
        this.nextWeek = nextWeek;
    }

    @Override
    public CommandResult execute() {
        RecommendationProposal proposal;
        if (date != null) {
            proposal = RecommendationService.recommendDate(activityManager, preferenceManager.getProfile(),
                    date, now);
        } else if (nextWeek) {
            proposal = RecommendationService.recommendNextWeek(activityManager, preferenceManager.getProfile(), now);
        } else {
            proposal = RecommendationService.recommendThisWeek(activityManager, preferenceManager.getProfile(), now);
        }
        recommendationManager.setProposal(proposal);
        List<Activity> previewActivities = RecommendationService.applyPreview(activityManager.getAll(), proposal);
        return new CommandResult(RecommendationFormatter.formatPreview(proposal, previewActivities));
    }
}
