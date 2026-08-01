package seedu.unienable.logic.recur;

import java.util.regex.Pattern;

import seedu.unienable.model.classes.Activity;
import seedu.unienable.model.classes.FixedActivity;
import seedu.unienable.model.enums.ActivityCategory;

/** Central rule for identifying fixed academic class-schedule activities. */
public class ClassSchedulePolicy {
    private static final Pattern SESSION_TERM = Pattern.compile(
            "(?i)(?<![A-Za-z0-9])(?:lecture|lec|tutorial|tut|lab|laboratory|"
                    + "section(?:al)?\\s+teaching|sec)(?![A-Za-z0-9])");

    /**
     * Returns whether an activity is an eligible lecture/tutorial/lab/section session.
     * The same rule is shared by recurrence and reset's "keep class schedule" option.
     */
    public boolean isClassSchedule(Activity activity) {
        return activity instanceof FixedActivity
                && activity.getCategory() == ActivityCategory.ACADEMIC
                && SESSION_TERM.matcher(activity.getDescription()).find();
    }
}
