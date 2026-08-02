package seedu.unienable.model.recommend;

import java.util.List;

import seedu.unienable.model.dashboard.DashboardPeriod;
import seedu.unienable.model.timetable.TimetablePeriod;

/** Immutable in-memory recommendation preview. */
public final class RecommendationProposal {
    private final TimetablePeriod timetablePeriod;
    private final DashboardPeriod dashboardPeriod;
    private final List<RecommendedPlacement> placements;
    private final List<Integer> unscheduledActivityIds;

    public RecommendationProposal(TimetablePeriod timetablePeriod, DashboardPeriod dashboardPeriod,
            List<RecommendedPlacement> placements, List<Integer> unscheduledActivityIds) {
        this.timetablePeriod = timetablePeriod;
        this.dashboardPeriod = dashboardPeriod;
        this.placements = List.copyOf(placements);
        this.unscheduledActivityIds = List.copyOf(unscheduledActivityIds);
    }

    public TimetablePeriod getTimetablePeriod() {
        return timetablePeriod;
    }

    public DashboardPeriod getDashboardPeriod() {
        return dashboardPeriod;
    }

    public List<RecommendedPlacement> getPlacements() {
        return placements;
    }

    public List<Integer> getUnscheduledActivityIds() {
        return unscheduledActivityIds;
    }

    public boolean hasPlacements() {
        return !placements.isEmpty();
    }
}
