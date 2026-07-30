package seedu.unienable.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;

import seedu.unienable.model.classes.EnergyRating;
import seedu.unienable.model.classes.FixedActivity;
import seedu.unienable.model.classes.FlexibleActivity;
import seedu.unienable.model.classes.SensoryRating;
import seedu.unienable.model.enums.ActivityCategory;

class MessageFormatterTest {
    @Test
    public void formatView_fixedActivityWithTopicAndNote_matchesGuideExample() throws Exception {
        FixedActivity activity = new FixedActivity(12, "CG3207 lecture", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(11, 0),
                EnergyRating.of(4), SensoryRating.of(3), "CG3207", "Bring laptop");

        String expected = "Activity [12]\n"
                + "Description : CG3207 lecture\n"
                + "Status      : Incomplete\n"
                + "Type        : FIXED\n"
                + "Date        : 2026-08-15\n"
                + "Start       : 09:00\n"
                + "End         : 11:00\n"
                + "Category    : ACADEMIC\n"
                + "Topic       : CG3207\n"
                + "Energy      : 4/5\n"
                + "Sensory     : 3/5\n"
                + "Note        : Bring laptop";

        assertEquals(expected, MessageFormatter.formatView(activity));
    }

    @Test
    public void formatView_noTopicOrNote_omitsThoseLines() throws Exception {
        FixedActivity activity = new FixedActivity(13, "Consultation", ActivityCategory.OTHERS,
                LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(10, 0),
                EnergyRating.of(2), SensoryRating.of(2), null, null);

        String result = MessageFormatter.formatView(activity);

        assertFalse(result.contains("Topic"));
        assertFalse(result.contains("Note"));
    }

    @Test
    public void formatView_completeActivity_showsCompleteStatus() throws Exception {
        FixedActivity activity = new FixedActivity(1, "Task", ActivityCategory.OTHERS,
                LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(10, 0),
                EnergyRating.of(2), SensoryRating.of(2), null, null);
        activity.mark();

        assertTrue(MessageFormatter.formatView(activity).contains("Status      : Complete"));
    }

    @Test
    public void formatView_flexibleActivity_usesEarliestLatestDurationLabels() throws Exception {
        FlexibleActivity activity = new FlexibleActivity(13, "Finish assignment 1", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(10, 0), LocalTime.of(18, 0), 90,
                EnergyRating.of(5), SensoryRating.of(2), "CG3207", null);

        String result = MessageFormatter.formatView(activity);

        assertTrue(result.contains("Earliest    : 10:00"));
        assertTrue(result.contains("Latest      : 18:00"));
        assertTrue(result.contains("Duration    : 90 min"));
    }

    @Test
    public void formatConcise_fixedActivityWithTopic_matchesGuideExample() throws Exception {
        FixedActivity activity = new FixedActivity(12, "CG3207 lecture", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(11, 0),
                EnergyRating.of(4), SensoryRating.of(3), "CG3207", null);

        String expected = "[12][ ][F] 2026-08-15 09:00–11:00 | CG3207 lecture\n"
                + "             ACADEMIC / CG3207 | E4 | S3";

        assertEquals(expected, MessageFormatter.formatConcise(activity));
    }

    @Test
    public void formatConcise_flexibleActivityWithTopic_includesDuration() throws Exception {
        FlexibleActivity activity = new FlexibleActivity(13, "Finish assignment 1", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(10, 0), LocalTime.of(18, 0), 90,
                EnergyRating.of(5), SensoryRating.of(2), "CG3207", null);

        String expected = "[13][ ][L] 2026-08-15 10:00–18:00 | Finish assignment 1\n"
                + "             ACADEMIC / CG3207 | 90 min | E5 | S2";

        assertEquals(expected, MessageFormatter.formatConcise(activity));
    }

    @Test
    public void formatConcise_completeActivityNoTopic_omitsTopicSuffix() throws Exception {
        FixedActivity activity = new FixedActivity(14, "Project briefing", ActivityCategory.CCA,
                LocalDate.of(2026, 8, 15), LocalTime.of(14, 0), LocalTime.of(15, 0),
                EnergyRating.of(2), SensoryRating.of(3), null, null);
        activity.mark();

        String expected = "[14][X][F] 2026-08-15 14:00–15:00 | Project briefing\n"
                + "             CCA | E2 | S3";

        assertEquals(expected, MessageFormatter.formatConcise(activity));
    }

    @Test
    public void formatDetail_fixedActivity_matchesGuideExample() throws Exception {
        FixedActivity activity = new FixedActivity(12, "CG3207 lecture", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(11, 0),
                EnergyRating.of(4), SensoryRating.of(3), "CG3207", "Bring laptop");

        String expected = "[12] CG3207 lecture\n"
                + "Status: Incomplete | Type: FIXED | Date: 2026-08-15\n"
                + "Time: 09:00–11:00 | Category: ACADEMIC | Topic: CG3207\n"
                + "Energy: 4/5 | Sensory: 3/5 | Note: Bring laptop";

        assertEquals(expected, MessageFormatter.formatDetail(activity));
    }

    @Test
    public void formatDetail_flexibleActivity_matchesGuideExample() throws Exception {
        FlexibleActivity activity = new FlexibleActivity(13, "Finish assignment 1", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(10, 0), LocalTime.of(18, 0), 90,
                EnergyRating.of(5), SensoryRating.of(2), "CG3207", null);

        String expected = "[13] Finish assignment 1\n"
                + "Status: Incomplete | Type: FLEXIBLE | Date: 2026-08-15\n"
                + "Window: 10:00–18:00 | Duration: 90 min\n"
                + "Category: ACADEMIC | Topic: CG3207\n"
                + "Energy: 5/5 | Sensory: 2/5 | Note: None";

        assertEquals(expected, MessageFormatter.formatDetail(activity));
    }
}
