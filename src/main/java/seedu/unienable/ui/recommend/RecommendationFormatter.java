package seedu.unienable.ui.recommend;

import seedu.unienable.model.recommend.RecommendationPreview;
import seedu.unienable.model.recommend.RecommendationProposal;
import seedu.unienable.model.recommend.RecommendedPlacement;
import seedu.unienable.model.timetable.TimetableMode;
import seedu.unienable.ui.dashboard.DashboardFormatter;
import seedu.unienable.ui.timetable.TimetableFormatter;

/** Formats recommendation preview, view, adopt, and cancel results. */
public final class RecommendationFormatter {
    private RecommendationFormatter() {
    }

    /**
     * Formats an already-calculated recommendation preview. preview's timetable and dashboard are
     * taken as given - this method performs no business calculation of its own, matching this
     * project's UI-formats/logic-calculates layering.
     */
    public static String formatPreview(RecommendationPreview preview) {
        RecommendationProposal proposal = preview.getProposal();
        StringBuilder result = new StringBuilder("Recommended timetable preview\n");
        result.append("Period: ").append(proposal.getTimetablePeriod().getLabel());
        if (proposal.getPlacements().isEmpty()) {
            result.append("\n\nNo eligible flexible activities could be scheduled in the selected period.");
        } else {
            result.append("\n\nProposed placements:");
            for (RecommendedPlacement placement : proposal.getPlacements()) {
                result.append("\n- [").append(placement.activityId()).append("] ")
                        .append(placement.date()).append(' ')
                        .append(placement.startTime()).append(" -> ").append(placement.endTime())
                        .append(" | ").append(placement.description());
                if (placement.tomatoSuggested()) {
                    result.append("\n  Study suggestion: Consider using 25-minute focus blocks with short breaks.");
                }
            }
        }
        if (!proposal.getUnscheduledActivityIds().isEmpty()) {
            result.append("\n\nUnscheduled activity IDs: ").append(proposal.getUnscheduledActivityIds());
        }

        result.append("\n\n").append(TimetableFormatter.format(preview.getTimetable(), TimetableMode.DETAIL));
        result.append("\n\n").append(DashboardFormatter.format(preview.getDashboard(), true));
        return result.toString();
    }

    /** Formats the no-proposal view error. */
    public static String formatMissingProposal() {
        return "No recommendation proposal is currently active. Generate one with recommend.";
    }

    /** Formats the successful adopt result. */
    public static String formatAdoptSuccess(RecommendationProposal proposal) {
        return "Recommendation adopted.\nScheduled flexible activities: " + proposal.getPlacements().size()
                + "\nUnscheduled flexible activities: " + proposal.getUnscheduledActivityIds().size();
    }

    /** Formats the successful cancel result. */
    public static String formatCancel() {
        return "Recommendation proposal discarded. No changes were made.";
    }
}
