package seedu.unienable.model.recommend;

import java.time.LocalDate;
import java.time.LocalTime;

/** One deterministic proposed placement for a flexible activity. */
public record RecommendedPlacement(int activityId, String description, LocalDate date,
        LocalTime startTime, LocalTime endTime, boolean tomatoSuggested) {
}
