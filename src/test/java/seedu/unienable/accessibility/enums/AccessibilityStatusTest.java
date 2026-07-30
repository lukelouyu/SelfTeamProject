package seedu.unienable.accessibility.enums;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.junit.jupiter.api.Test;

class AccessibilityStatusTest {
    @Test
    public void values_matchThreeStatesInOrder() {
        assertArrayEquals(
                new AccessibilityStatus[] { AccessibilityStatus.YES, AccessibilityStatus.NO,
                    AccessibilityStatus.UNKNOWN },
                AccessibilityStatus.values());
    }
}
