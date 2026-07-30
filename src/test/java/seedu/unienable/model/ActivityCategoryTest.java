package seedu.unienable.model;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.junit.jupiter.api.Test;

class ActivityCategoryTest {
    @Test
    public void values_matchDocumentedCategoriesInOrder() {
        assertArrayEquals(
                new ActivityCategory[] {
                    ActivityCategory.ACADEMIC,
                    ActivityCategory.CCA,
                    ActivityCategory.WORK_INTERNSHIP,
                    ActivityCategory.OTHERS
                },
                ActivityCategory.values());
    }
}
