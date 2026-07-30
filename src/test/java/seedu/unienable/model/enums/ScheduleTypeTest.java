package seedu.unienable.model.enums;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.junit.jupiter.api.Test;

class ScheduleTypeTest {
    @Test
    public void values_matchDocumentedTypesInOrder() {
        assertArrayEquals(
                new ScheduleType[] { ScheduleType.FIXED, ScheduleType.FLEXIBLE },
                ScheduleType.values());
    }
}
