package seedu.unienable.model.classes;

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
     * Creates a SensoryRating after validating the value.
     *
     * @param value the sensory-load rating to validate
     * @return a SensoryRating wrapping the validated value
     * @throws InvalidActivityException if value is not a whole number from 1 to 5
     */
    public static SensoryRating of(int value) throws InvalidActivityException {
        if (value < MIN_VALUE || value > MAX_VALUE) {
            throw new InvalidActivityException("sensory must be a whole number from 1 to 5.");
        }
        return new SensoryRating(value);
    }

    /** Returns the validated rating, from 1 to 5. */
    public int getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value + "/5";
    }
}
