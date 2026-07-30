package seedu.unienable.model.enums;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.junit.jupiter.api.Test;

class ActivityOrderTest {
    @Test
    public void values_matchDocumentedOrdersInOrder() {
        assertArrayEquals(
                new ActivityOrder[] { ActivityOrder.INPUT, ActivityOrder.TIME, ActivityOrder.CHRONOLOGICAL },
                ActivityOrder.values());
    }
}
