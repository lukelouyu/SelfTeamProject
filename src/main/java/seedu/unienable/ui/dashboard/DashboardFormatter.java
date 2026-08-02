package seedu.unienable.ui.dashboard;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import seedu.unienable.model.dashboard.DashboardPeriod;
import seedu.unienable.model.dashboard.DashboardSummary;
import seedu.unienable.model.dashboard.RatingSummary;
import seedu.unienable.model.enums.ActivityCategory;

/**
 * Formats a {@link DashboardSummary} into deterministic, terminal-independent text: ASCII bars
 * only ({@code #}/{@code -}, fixed 10-cell width), no colour, no Unicode, no width detection.
 * Pure formatting - every value shown here is decided by {@code DashboardService}; this class
 * makes no calculation of its own.
 */
public final class DashboardFormatter {
    private static final int BAR_WIDTH = 10;

    private DashboardFormatter() {
    }

    /**
     * Formats a dashboard summary.
     *
     * @param summary the calculated summary
     * @param detail whether to include the detail section (fixed/flexible counts, category
     *     breakdown, and per-rating distribution/average/highest)
     * @return the formatted dashboard report
     */
    public static String format(DashboardSummary summary, boolean detail) {
        List<String> lines = new ArrayList<>();
        lines.add("Dashboard: " + summary.getPeriod().getLabel());
        lines.add("Period: " + rangeText(summary.getPeriod()));
        lines.add("");

        if (summary.getTotalActivityCount() == 0) {
            lines.add("No activities found for the selected period.");
            return String.join("\n", lines);
        }

        lines.add("Activities: " + summary.getTotalActivityCount());
        lines.add("Planned workload: " + formatDuration(summary.getPlannedWorkloadMinutes()));
        lines.add("Nominal buffer: " + formatDuration(summary.getNominalBufferMinutes()));
        if (summary.isOverloaded()) {
            lines.add("Overloaded by: " + formatDuration(summary.getOverloadMinutes()));
        }
        lines.add("");
        lines.add("Energy demand: " + summary.getEnergy().getTotal() + " points");
        lines.add("High-energy activities: " + summary.getEnergy().getHighCount());
        lines.add("");
        lines.add("Sensory load: " + summary.getSensory().getTotal() + " points");
        lines.add("High-sensory activities: " + summary.getSensory().getHighCount());
        lines.add("");
        lines.add(formatCompletionLine(summary));

        if (detail) {
            lines.add("");
            lines.addAll(detailLines(summary));
        }
        return String.join("\n", lines);
    }

    private static String rangeText(DashboardPeriod period) {
        LocalDate startDate = period.getStart().toLocalDate();
        LocalDate lastIncludedDate = period.getEnd().toLocalDate().minusDays(1);
        if (startDate.equals(lastIncludedDate)) {
            return startDate.toString();
        }
        return startDate + " to " + lastIncludedDate;
    }

    private static String formatDuration(long minutes) {
        long hours = minutes / 60;
        long mins = minutes % 60;
        return hours + "h " + String.format(Locale.ROOT, "%02d", mins) + "m";
    }

    private static String formatCompletionLine(DashboardSummary summary) {
        if (summary.getCompletionPercentage().isEmpty()) {
            return "Completion: No activities are due yet.";
        }
        int percentage = summary.getCompletionPercentage().getAsInt();
        int filled = (int) Math.round(percentage * (double) BAR_WIDTH / 100);
        return "Completion  [" + bar(filled) + "] " + percentage + "% ("
                + summary.getCompletedEligibleCount() + "/" + summary.getEligibleCount() + ")";
    }

    private static List<String> detailLines(DashboardSummary summary) {
        List<String> lines = new ArrayList<>();
        lines.add("Fixed activities: " + summary.getFixedCount());
        lines.add("Flexible activities: " + summary.getFlexibleCount());
        lines.add("");
        lines.add("Category breakdown");
        for (ActivityCategory category : ActivityCategory.values()) {
            lines.add(category + ": " + summary.getCategoryCounts().get(category));
        }
        lines.add("");
        lines.addAll(ratingDetailLines("Energy", "energy", summary.getEnergy()));
        lines.add("");
        lines.addAll(ratingDetailLines("Sensory", "sensory", summary.getSensory()));
        return lines;
    }

    private static List<String> ratingDetailLines(String heading, String label, RatingSummary rating) {
        List<String> lines = new ArrayList<>();
        lines.add(heading + " distribution");
        int[] distribution = rating.getDistribution();
        int max = 0;
        for (int count : distribution) {
            max = Math.max(max, count);
        }
        for (int i = 0; i < distribution.length; i++) {
            int filled = max == 0 ? 0 : (int) Math.round(distribution[i] * (double) BAR_WIDTH / max);
            lines.add((i + 1) + " [" + bar(filled) + "] " + distribution[i]);
        }
        if (rating.hasData()) {
            lines.add("Average " + label + ": " + String.format(Locale.ROOT, "%.1f", rating.getAverage()) + "/5");
            lines.add("Highest " + label + ": " + rating.getHighest() + "/5");
        } else {
            lines.add("Average " + label + ": N/A");
            lines.add("Highest " + label + ": N/A");
        }
        return lines;
    }

    private static String bar(int filledCells) {
        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < BAR_WIDTH; i++) {
            bar.append(i < filledCells ? '#' : '-');
        }
        return bar.toString();
    }
}
