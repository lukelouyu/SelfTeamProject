package seedu.unienable.logic.recur;

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

class ClassSchedulePolicyTest {
    private final ClassSchedulePolicy policy = new ClassSchedulePolicy();

    @Test
    public void isClassSchedule_screenshotAbbreviations_returnsTrue() throws Exception {
        assertTrue(policy.isClassSchedule(fixed("CS2113 LEC [1]", ActivityCategory.ACADEMIC)));
        assertTrue(policy.isClassSchedule(fixed("GEC1044 TUT [E2]", ActivityCategory.ACADEMIC)));
        assertTrue(policy.isClassSchedule(fixed("CG3201 LAB [01]", ActivityCategory.ACADEMIC)));
        assertTrue(policy.isClassSchedule(fixed("ES2631 SEC [G06]", ActivityCategory.ACADEMIC)));
    }

    @Test
    public void isClassSchedule_fullSessionWordsCaseInsensitive_returnsTrue() throws Exception {
        assertTrue(policy.isClassSchedule(fixed("Module Lecture", ActivityCategory.ACADEMIC)));
        assertTrue(policy.isClassSchedule(fixed("MODULE TUTORIAL", ActivityCategory.ACADEMIC)));
        assertTrue(policy.isClassSchedule(fixed("Module Laboratory", ActivityCategory.ACADEMIC)));
        assertTrue(policy.isClassSchedule(fixed("Module Sectional Teaching", ActivityCategory.ACADEMIC)));
    }

    @Test
    public void isClassSchedule_flexibleAcademicLecture_returnsFalse() throws Exception {
        FlexibleActivity flexible = new FlexibleActivity(1, "CS2113 lecture",
                ActivityCategory.ACADEMIC, LocalDate.of(2026, 8, 14), LocalTime.of(9, 0),
                LocalTime.of(12, 0), 60, EnergyRating.of(2), SensoryRating.of(2), null, null);

        assertFalse(policy.isClassSchedule(flexible));
    }

    @Test
    public void isClassSchedule_nonAcademicLecture_returnsFalse() throws Exception {
        assertFalse(policy.isClassSchedule(fixed("Public lecture", ActivityCategory.CCA)));
    }

    @Test
    public void isClassSchedule_unrelatedSubstring_returnsFalse() throws Exception {
        assertFalse(policy.isClassSchedule(fixed("Collaboration meeting", ActivityCategory.ACADEMIC)));
    }

    private FixedActivity fixed(String description, ActivityCategory category) throws Exception {
        return new FixedActivity(1, description, category, LocalDate.of(2026, 8, 14),
                LocalTime.of(9, 0), LocalTime.of(10, 0), EnergyRating.of(2),
                SensoryRating.of(2), null, null);
    }
}
