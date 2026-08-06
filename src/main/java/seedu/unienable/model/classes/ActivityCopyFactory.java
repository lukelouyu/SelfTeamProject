package seedu.unienable.model.classes;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/** Creates independent, field-complete copies of activities. */
public final class ActivityCopyFactory {
    private ActivityCopyFactory() {
    }

    /**
     * Copies every activity in encounter order.
     *
     * @param source activities to copy
     * @return independent copies in the same order
     */
    public static List<Activity> copyAll(List<? extends Activity> source) {
        List<Activity> copies = new ArrayList<>(source.size());
        for (Activity activity : source) {
            copies.add(copyOf(activity));
        }
        return copies;
    }

    /**
     * Copies one activity, preserving its concrete type, fields, completion status, and adopted
     * placement.
     *
     * @param activity activity to copy
     * @return an independent field-complete copy
     * @throws IllegalArgumentException if the activity has an unsupported concrete type
     */
    public static Activity copyOf(Activity activity) {
        return copyWithOptionalPlacement(activity, null, false);
    }

    /**
     * Copies one activity and applies a proposed adopted start to flexible activities. If the
     * proposed start is null, an existing adopted placement is preserved.
     *
     * @param activity activity to copy
     * @param proposedStart proposed adopted start, or null to preserve the existing placement
     * @return an independent field-complete copy
     * @throws IllegalArgumentException if the activity has an unsupported concrete type or the
     *     proposed placement does not fit its flexible window
     */
    public static Activity copyWithAdoptedStart(Activity activity, LocalTime proposedStart) {
        return copyWithOptionalPlacement(activity, proposedStart, true);
    }

    private static Activity copyWithOptionalPlacement(Activity activity, LocalTime proposedStart,
            boolean applyProposedStart) {
        Activity copy;
        if (activity instanceof FixedActivity) {
            FixedActivity fixed = (FixedActivity) activity;
            copy = new FixedActivity(fixed.getId(), fixed.getDescription(), fixed.getCategory(),
                    fixed.getDate(), fixed.getStartTime(), fixed.getEndTime(), fixed.getEnergyRating(),
                    fixed.getSensoryRating(), fixed.getTopic(), fixed.getNote());
        } else if (activity instanceof FlexibleActivity) {
            FlexibleActivity flexible = (FlexibleActivity) activity;
            LocalTime adoptedStart = applyProposedStart && proposedStart != null
                    ? proposedStart : flexible.getAdoptedStartTime();
            copy = new FlexibleActivity(flexible.getId(), flexible.getDescription(), flexible.getCategory(),
                    flexible.getDate(), flexible.getEarliestStart(), flexible.getLatestEnd(),
                    flexible.getDurationMinutes(), flexible.getEnergyRating(), flexible.getSensoryRating(),
                    flexible.getTopic(), flexible.getNote(), adoptedStart);
        } else {
            throw new IllegalArgumentException("unsupported activity type: " + activity.getClass().getName());
        }
        if (activity.isComplete()) {
            copy.mark();
        }
        return copy;
    }
}
