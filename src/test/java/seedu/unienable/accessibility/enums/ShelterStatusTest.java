package seedu.unienable.accessibility.enums;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.junit.jupiter.api.Test;

class ShelterStatusTest {
    @Test
    public void values_matchThreeStatesInOrder() {
        assertArrayEquals(
                new ShelterStatus[] { ShelterStatus.YES, ShelterStatus.NO, ShelterStatus.UNKNOWN },
                ShelterStatus.values());
    }
}
