package seedu.unienable.parser.recommend;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;

import seedu.unienable.command.Command;
import seedu.unienable.command.recommend.RecommendAdoptCommand;
import seedu.unienable.command.recommend.RecommendCancelCommand;
import seedu.unienable.command.recommend.RecommendGenerateCommand;
import seedu.unienable.command.recommend.RecommendViewCommand;
import seedu.unienable.exception.InvalidCommandException;
import seedu.unienable.exception.InvalidDateTimeException;
import seedu.unienable.exception.MissingInputException;
import seedu.unienable.logic.ActivityManager;
import seedu.unienable.logic.RelativeDateResolver;
import seedu.unienable.logic.preference.PreferenceManager;
import seedu.unienable.logic.recommend.RecommendationManager;
import seedu.unienable.logic.recommend.RecommendationService;
import seedu.unienable.model.recommend.RecommendationProposal;
import seedu.unienable.parser.common.DateTimeParser;

/** Parses the recommend command family. */
public class RecommendCommandParser {
    /** Parses recommend generation, view, adopt, and cancel commands. */
    public Command parse(ActivityManager activityManager, PreferenceManager preferenceManager,
            RecommendationManager recommendationManager, LocalDateTime now, String args)
            throws MissingInputException, InvalidCommandException, InvalidDateTimeException {
        String trimmed = args.trim();
        if (trimmed.isEmpty()) {
            return new RecommendGenerateCommand(activityManager, preferenceManager, recommendationManager, now, null);
        }
        if ("this week".equalsIgnoreCase(trimmed)) {
            return new RecommendGenerateCommand(activityManager, preferenceManager, recommendationManager, now, null);
        }
        if ("next week".equalsIgnoreCase(trimmed)) {
            return new RecommendGenerateCommand(activityManager, preferenceManager, recommendationManager, now,
                    null, true);
        }
        if ("today".equalsIgnoreCase(trimmed)) {
            return new RecommendGenerateCommand(activityManager, preferenceManager, recommendationManager, now,
                    RelativeDateResolver.today(now));
        }
        if ("tomorrow".equalsIgnoreCase(trimmed)) {
            return new RecommendGenerateCommand(activityManager, preferenceManager, recommendationManager, now,
                    RelativeDateResolver.tomorrow(now));
        }
        if ("view".equalsIgnoreCase(trimmed)) {
            return new RecommendViewCommand(activityManager, recommendationManager);
        }
        if ("cancel".equalsIgnoreCase(trimmed)) {
            return new RecommendCancelCommand(recommendationManager);
        }
        if ("adopt".equalsIgnoreCase(trimmed)) {
            RecommendationProposal proposal = recommendationManager.getProposal()
                    .orElseThrow(() -> new InvalidCommandException(
                            "No recommendation proposal is currently active. Generate one with recommend."));
            if (RecommendationService.hasElapsedPlacement(proposal, now)) {
                throw new InvalidCommandException("This recommendation proposal is stale - one or more "
                        + "proposed start times have already passed. Generate a new recommendation with "
                        + "recommend.");
            }
            if (RecommendationService.hasOutOfPreferredRangePlacement(proposal, preferenceManager.getProfile())) {
                throw new InvalidCommandException("This recommendation proposal no longer fits your current "
                        + "preferred daily start/end - preferences changed since it was generated. Generate a "
                        + "new recommendation with recommend.");
            }
            return new RecommendAdoptCommand(activityManager, proposal);
        }
        String normalized = trimmed.toLowerCase(Locale.ROOT);
        String dateMarker = normalized.startsWith("date/") ? "date/"
                : normalized.startsWith("day/") ? "day/" : null;
        if (dateMarker != null) {
            String dateText = trimmed.substring(dateMarker.length()).trim();
            if (dateText.contains(" ")) {
                throw new InvalidCommandException(
                        "\"recommend " + dateMarker + "YYYY-MM-DD\" does not take extra arguments.");
            }
            LocalDate date = DateTimeParser.parseNotBeforeDate(dateText, now.toLocalDate());
            return new RecommendGenerateCommand(activityManager, preferenceManager, recommendationManager, now, date);
        }
        throw new InvalidCommandException("Unknown recommend command \"" + trimmed
                + "\". Enter guide recommend for help.");
    }
}
