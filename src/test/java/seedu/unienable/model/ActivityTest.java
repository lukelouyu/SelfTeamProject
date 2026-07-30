package seedu.unienable.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class ActivityTest {
    private static final class StubActivity extends Activity {
        StubActivity(int id, String description, ActivityCategory category, LocalDate date,
                EnergyRating energyRating, SensoryRating sensoryRating, String topic, String note) {
            super(id, description, category, date, energyRating, sensoryRating, topic, note);
        }

        @Override
        public ScheduleType getScheduleType() {
            return ScheduleType.FIXED;
        }
    }

    private static Activity newStubActivity() throws Exception {
        return new StubActivity(12, "CG3207 lecture", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), EnergyRating.of(4), SensoryRating.of(3), "CG3207", "Bring laptop");
    }

    @Test
    public void newActivity_startsIncomplete() throws Exception {
        Activity activity = newStubActivity();

        assertFalse(activity.isComplete());
        assertEquals(CompletionStatus.INCOMPLETE, activity.getStatus());
    }

    @Test
    public void mark_setsStatusComplete() throws Exception {
        Activity activity = newStubActivity();

        activity.mark();

        assertTrue(activity.isComplete());
        assertEquals(CompletionStatus.COMPLETE, activity.getStatus());
    }

    @Test
    public void unmark_afterMark_setsStatusIncomplete() throws Exception {
        Activity activity = newStubActivity();
        activity.mark();

        activity.unmark();

        assertFalse(activity.isComplete());
    }

    @Test
    public void getters_returnConstructorValues() throws Exception {
        Activity activity = newStubActivity();

        assertEquals(12, activity.getId());
        assertEquals("CG3207 lecture", activity.getDescription());
        assertEquals(ActivityCategory.ACADEMIC, activity.getCategory());
        assertEquals(LocalDate.of(2026, 8, 15), activity.getDate());
        assertEquals(4, activity.getEnergyRating().getValue());
        assertEquals(3, activity.getSensoryRating().getValue());
        assertEquals("CG3207", activity.getTopic());
        assertEquals("Bring laptop", activity.getNote());
    }

    @Test
    public void getters_allowNullTopicAndNote() throws Exception {
        Activity activity = new StubActivity(13, "Untitled task", ActivityCategory.OTHERS,
                LocalDate.of(2026, 8, 16), EnergyRating.of(1), SensoryRating.of(1), null, null);

        assertNull(activity.getTopic());
        assertNull(activity.getNote());
    }

    @Test
    public void setDescription_updatesDescription() throws Exception {
        Activity activity = newStubActivity();

        activity.setDescription("Updated lecture");

        assertEquals("Updated lecture", activity.getDescription());
    }

    @Test
    public void setCategory_updatesCategory() throws Exception {
        Activity activity = newStubActivity();

        activity.setCategory(ActivityCategory.CCA);

        assertEquals(ActivityCategory.CCA, activity.getCategory());
    }

    @Test
    public void setDate_updatesDate() throws Exception {
        Activity activity = newStubActivity();

        activity.setDate(LocalDate.of(2026, 9, 1));

        assertEquals(LocalDate.of(2026, 9, 1), activity.getDate());
    }

    @Test
    public void setEnergyRating_updatesEnergyRating() throws Exception {
        Activity activity = newStubActivity();

        activity.setEnergyRating(EnergyRating.of(1));

        assertEquals(1, activity.getEnergyRating().getValue());
    }

    @Test
    public void setSensoryRating_updatesSensoryRating() throws Exception {
        Activity activity = newStubActivity();

        activity.setSensoryRating(SensoryRating.of(5));

        assertEquals(5, activity.getSensoryRating().getValue());
    }

    @Test
    public void setTopic_updatesTopic() throws Exception {
        Activity activity = newStubActivity();

        activity.setTopic("CS2113");

        assertEquals("CS2113", activity.getTopic());
    }

    @Test
    public void setTopic_null_clearsTopic() throws Exception {
        Activity activity = newStubActivity();

        activity.setTopic(null);

        assertNull(activity.getTopic());
    }

    @Test
    public void setNote_updatesNote() throws Exception {
        Activity activity = newStubActivity();

        activity.setNote("Bring headphones");

        assertEquals("Bring headphones", activity.getNote());
    }
}
