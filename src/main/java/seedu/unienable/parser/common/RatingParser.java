package seedu.unienable.parser.common;

import seedu.unienable.exception.InvalidActivityException;
import seedu.unienable.model.classes.EnergyRating;
import seedu.unienable.model.classes.SensoryRating;

/** Parses raw text into validated energy/sensory ratings. */
public class RatingParser {
    /**
     * Parses raw text into an EnergyRating.
     *
     * @param rating the raw rating text
     * @return the validated EnergyRating
     * @throws InvalidActivityException if rating is not a whole number from 1 to 5
     */
    public static EnergyRating parseEnergyRating(String rating) throws InvalidActivityException {
        return EnergyRating.of(parseWholeNumber(rating, "energy"));
    }

    /**
     * Parses raw text into a SensoryRating.
     *
     * @param rating the raw rating text
     * @return the validated SensoryRating
     * @throws InvalidActivityException if rating is not a whole number from 1 to 5
     */
    public static SensoryRating parseSensoryRating(String rating) throws InvalidActivityException {
        return SensoryRating.of(parseWholeNumber(rating, "sensory"));
    }

    private static int parseWholeNumber(String rating, String fieldName) throws InvalidActivityException {
        try {
            return Integer.parseInt(rating.trim());
        } catch (NumberFormatException e) {
            throw new InvalidActivityException(fieldName + " must be a whole number from 1 to 5.");
        }
    }
}
