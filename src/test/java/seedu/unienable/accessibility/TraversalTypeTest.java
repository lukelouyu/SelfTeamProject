package seedu.unienable.accessibility;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.junit.jupiter.api.Test;

class TraversalTypeTest {
    @Test
    public void values_matchDocumentedTypesInOrder() {
        assertArrayEquals(
                new TraversalType[] { TraversalType.RAMP, TraversalType.SHELTERED_RAMP, TraversalType.LIFT,
                    TraversalType.PATH, TraversalType.OTHER },
                TraversalType.values());
    }
}
