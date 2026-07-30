package seedu.unienable.model;

import seedu.unienable.exception.InvalidActivityException;

/** A user-entered sensory-load rating from 1 (very low) to 5 (very high). */
public class SensoryRating {
    private static final int MIN_VALUE = 1;
    private static final int MAX_VALUE = 5;

    private final int value;

    private SensoryRating(int value) {
        this.value = value;
    }

    /**
     * Creates a SensoryRating after validating that the value is a whole number from 1 to 5.
     */
    public static SensoryRating of(int value) throws InvalidActivityException {
        if (value < MIN_VALUE || value > MAX_VALUE) {
            throw new InvalidActivityException("sensory must be a whole number from 1 to 5.");
        }
        return new SensoryRating(value);
    }

    public int getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value + "/5";
    }
}
