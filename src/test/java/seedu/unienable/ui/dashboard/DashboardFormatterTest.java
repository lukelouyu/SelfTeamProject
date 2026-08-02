package seedu.unienable.ui.dashboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.Map;
import java.util.OptionalInt;

import org.junit.jupiter.api.Test;

import seedu.unienable.model.dashboard.DashboardPeriod;
import seedu.unienable.model.dashboard.DashboardSummary;
import seedu.unienable.model.dashboard.RatingSummary;
import seedu.unienable.model.enums.ActivityCategory;

class DashboardFormatterTest {
    private static DashboardPeriod dayPeriod(String label, int day) {
        return new DashboardPeriod(label, LocalDateTime.of(2026, 8, day, 0, 0),
                LocalDateTime.of(2026, 8, day + 1, 0, 0));
    }

    private static Map<ActivityCategory, Integer> categoryCounts(int academic, int cca, int work, int others) {
        Map<ActivityCategory, Integer> counts = new EnumMap<>(ActivityCategory.class);
        counts.put(ActivityCategory.ACADEMIC, academic);
        counts.put(ActivityCategory.CCA, cca);
        counts.put(ActivityCategory.WORK_INTERNSHIP, work);
        counts.put(ActivityCategory.OTHERS, others);
        return counts;
    }

    @Test
    public void format_defaultOutput_exactText() {
        RatingSummary energy = new RatingSummary(24, 3, true, 2.4, 5, new int[] { 1, 2, 3, 2, 2 });
        RatingSummary sensory = new RatingSummary(19, 2, true, 1.9, 4, new int[] { 2, 3, 2, 2, 1 });
        DashboardSummary summary = new DashboardSummary(dayPeriod("Today", 17), 10, 510, 930, 0, energy, sensory,
                10, 6, OptionalInt.of(60), 7, 3, categoryCounts(5, 2, 1, 2));

        String result = DashboardFormatter.format(summary, false);

        assertEquals("Dashboard: Today\n"
                + "Period: 2026-08-17\n"
                + "\n"
                + "Activities: 10\n"
                + "Planned workload: 8h 30m\n"
                + "Nominal buffer: 15h 30m\n"
                + "\n"
                + "Energy demand: 24 points\n"
                + "High-energy activities: 3\n"
                + "\n"
                + "Sensory load: 19 points\n"
                + "High-sensory activities: 2\n"
                + "\n"
                + "Completion  [######----] 60% (6/10)", result);
    }

    @Test
    public void format_overloadedPeriod_showsOverloadedByLineExactText() {
        RatingSummary energy = new RatingSummary(89, 11, true, 3.2, 5, new int[] { 1, 2, 3, 2, 2 });
        RatingSummary sensory = new RatingSummary(76, 9, true, 2.7, 5, new int[] { 2, 3, 2, 2, 1 });
        DashboardPeriod week = new DashboardPeriod("This week", LocalDateTime.of(2026, 8, 17, 0, 0),
                LocalDateTime.of(2026, 8, 24, 0, 0));
        DashboardSummary summary = new DashboardSummary(week, 28, 10230, 0, 150, energy, sensory, 20, 14,
                OptionalInt.of(70), 20, 8, categoryCounts(15, 5, 4, 4));

        String result = DashboardFormatter.format(summary, false);

        assertEquals("Dashboard: This week\n"
                + "Period: 2026-08-17 to 2026-08-23\n"
                + "\n"
                + "Activities: 28\n"
                + "Planned workload: 170h 30m\n"
                + "Nominal buffer: 0h 00m\n"
                + "Overloaded by: 2h 30m\n"
                + "\n"
                + "Energy demand: 89 points\n"
                + "High-energy activities: 11\n"
                + "\n"
                + "Sensory load: 76 points\n"
                + "High-sensory activities: 9\n"
                + "\n"
                + "Completion  [#######---] 70% (14/20)", result);
        assertTrue(result.contains("Nominal buffer"));
        assertFalse(result.contains("Free time"));
    }

    @Test
    public void format_nothingDueYet_exactMessage() {
        RatingSummary energy = new RatingSummary(12, 1, true, 3.0, 4, new int[] { 0, 1, 2, 0, 1 });
        RatingSummary sensory = new RatingSummary(10, 1, true, 2.5, 4, new int[] { 0, 2, 1, 1, 0 });
        DashboardSummary summary = new DashboardSummary(dayPeriod("Tomorrow", 18), 4, 300, 1140, 0, energy, sensory,
                0, 0, OptionalInt.empty(), 3, 1, categoryCounts(2, 1, 0, 1));

        String result = DashboardFormatter.format(summary, false);

        assertEquals("Dashboard: Tomorrow\n"
                + "Period: 2026-08-18\n"
                + "\n"
                + "Activities: 4\n"
                + "Planned workload: 5h 00m\n"
                + "Nominal buffer: 19h 00m\n"
                + "\n"
                + "Energy demand: 12 points\n"
                + "High-energy activities: 1\n"
                + "\n"
                + "Sensory load: 10 points\n"
                + "High-sensory activities: 1\n"
                + "\n"
                + "Completion: No activities are due yet.", result);
    }

    @Test
    public void format_emptyPeriod_exactMessage() {
        RatingSummary empty = new RatingSummary(0, 0, false, 0.0, 0, new int[5]);
        DashboardSummary summary = new DashboardSummary(dayPeriod("Tomorrow", 18), 0, 0, 1440, 0, empty, empty,
                0, 0, OptionalInt.empty(), 0, 0, categoryCounts(0, 0, 0, 0));

        String result = DashboardFormatter.format(summary, false);

        assertEquals("Dashboard: Tomorrow\n"
                + "Period: 2026-08-18\n"
                + "\n"
                + "No activities found for the selected period.", result);
    }

    @Test
    public void format_detailMode_exactSectionText() {
        RatingSummary energy = new RatingSummary(24, 3, true, 2.4, 5, new int[] { 1, 2, 3, 2, 2 });
        RatingSummary sensory = new RatingSummary(19, 2, true, 1.9, 4, new int[] { 2, 3, 2, 2, 1 });
        DashboardSummary summary = new DashboardSummary(dayPeriod("Today", 17), 10, 510, 930, 0, energy, sensory,
                10, 6, OptionalInt.of(60), 7, 3, categoryCounts(5, 2, 1, 2));

        String result = DashboardFormatter.format(summary, true);

        assertTrue(result.endsWith("Fixed activities: 7\n"
                + "Flexible activities: 3\n"
                + "\n"
                + "Category breakdown\n"
                + "ACADEMIC: 5\n"
                + "CCA: 2\n"
                + "WORK_INTERNSHIP: 1\n"
                + "OTHERS: 2\n"
                + "\n"
                + "Energy distribution\n"
                + "1 [###-------] 1\n"
                + "2 [#######---] 2\n"
                + "3 [##########] 3\n"
                + "4 [#######---] 2\n"
                + "5 [#######---] 2\n"
                + "Average energy: 2.4/5\n"
                + "Highest energy: 5/5\n"
                + "\n"
                + "Sensory distribution\n"
                + "1 [#######---] 2\n"
                + "2 [##########] 3\n"
                + "3 [#######---] 2\n"
                + "4 [#######---] 2\n"
                + "5 [###-------] 1\n"
                + "Average sensory: 1.9/5\n"
                + "Highest sensory: 4/5"));
    }

    @Test
    public void format_allZeroDistribution_showsAllHyphenBars() {
        RatingSummary energy = new RatingSummary(0, 0, true, 0.0, 0, new int[5]);
        RatingSummary sensory = new RatingSummary(0, 0, true, 0.0, 0, new int[5]);
        DashboardSummary summary = new DashboardSummary(dayPeriod("Today", 17), 1, 0, 1440, 0, energy, sensory,
                0, 0, OptionalInt.empty(), 1, 0, categoryCounts(1, 0, 0, 0));

        String result = DashboardFormatter.format(summary, true);

        assertTrue(result.contains("1 [----------] 0"));
        assertTrue(result.contains("5 [----------] 0"));
    }

    @Test
    public void format_emptyRatingSummary_showsUnavailableNotZero() {
        RatingSummary noData = new RatingSummary(0, 0, false, 0.0, 0, new int[5]);
        DashboardSummary summary = new DashboardSummary(dayPeriod("Today", 17), 1, 0, 1440, 0, noData, noData,
                0, 0, OptionalInt.empty(), 1, 0, categoryCounts(1, 0, 0, 0));

        String result = DashboardFormatter.format(summary, true);

        assertTrue(result.contains("Average energy: N/A"));
        assertTrue(result.contains("Highest energy: N/A"));
        assertTrue(result.contains("Average sensory: N/A"));
        assertTrue(result.contains("Highest sensory: N/A"));
        assertFalse(result.contains("Average energy: 0.0"));
    }

    @Test
    public void format_neverShowsPercentageBarForEnergyOrSensoryTotals() {
        RatingSummary energy = new RatingSummary(24, 3, true, 2.4, 5, new int[] { 1, 2, 3, 2, 2 });
        RatingSummary sensory = new RatingSummary(19, 2, true, 1.9, 4, new int[] { 2, 3, 2, 2, 1 });
        DashboardSummary summary = new DashboardSummary(dayPeriod("Today", 17), 10, 510, 930, 0, energy, sensory,
                10, 6, OptionalInt.of(60), 7, 3, categoryCounts(5, 2, 1, 2));

        String result = DashboardFormatter.format(summary, false);

        assertFalse(result.contains("Energy demand: 24 points ["));
        assertFalse(result.contains("Sensory load: 19 points ["));
    }
}
