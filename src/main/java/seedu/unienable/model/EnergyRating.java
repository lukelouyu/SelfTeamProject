package seedu.unienable.model;

import seedu.unienable.exception.InvalidActivityException;

/** A user-entered energy-demand rating from 1 (very low) to 5 (very high). */
public class EnergyRating {
    private static final int MIN_VALUE = 1;
    private static final int MAX_VALUE = 5;

    private final int value;

    private EnergyRating(int value) {
        this.value = value;
    }

    /**
     * Creates an EnergyRating after validating that the value is a whole number from 1 to 5.
     */
    public static EnergyRating of(int value) throws InvalidActivityException {
        if (value < MIN_VALUE || value > MAX_VALUE) {
            throw new InvalidActivityException("energy must be a whole number from 1 to 5.");
        }
        return new EnergyRating(value);
    }

    public int getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value + "/5";
    }
}
