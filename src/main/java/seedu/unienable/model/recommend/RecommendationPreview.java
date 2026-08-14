package seedu.unienable.model.recommend;

import seedu.unienable.model.dashboard.DashboardSummary;
import seedu.unienable.model.timetable.TimetableView;

/**
 * Immutable bundle of everything a recommendation preview needs to display: the proposal itself
 * plus the already-calculated timetable and dashboard projections over the proposal's preview
 * activities. Calculated once in the logic layer ({@code RecommendationService.buildPreview}) so
 * that {@code RecommendationFormatter} only ever converts already-computed data into text.
 */
public final class RecommendationPreview {
    private final RecommendationProposal proposal;
    private final TimetableView timetable;
    private final DashboardSummary dashboard;

    /**
     * Creates a preview bundle from already-calculated components.
     *
     * @param proposal the proposal being previewed
     * @param timetable the timetable projection over the proposal's preview activities
     * @param dashboard the dashboard summary over the proposal's preview activities
     */
    public RecommendationPreview(RecommendationProposal proposal, TimetableView timetable,
            DashboardSummary dashboard) {
        this.proposal = proposal;
        this.timetable = timetable;
        this.dashboard = dashboard;
    }

    public RecommendationProposal getProposal() {
        return proposal;
    }

    public TimetableView getTimetable() {
        return timetable;
    }

    public DashboardSummary getDashboard() {
        return dashboard;
    }
}
