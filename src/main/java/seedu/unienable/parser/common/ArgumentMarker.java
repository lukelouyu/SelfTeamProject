package seedu.unienable.parser.common;

/**
 * A declarative field marker for {@link ArgumentTokenizer}: a prefix (e.g. "n/") and whether the
 * field it introduces is required. A command's full grammar is expressed as a list of these,
 * built once and reused, instead of a sequence of imperative {@link FieldParser} calls.
 */
public final class ArgumentMarker {
    private final String prefix;
    private final boolean required;

    private ArgumentMarker(String prefix, boolean required) {
        this.prefix = prefix;
        this.required = required;
    }

    /**
     * Declares a marker whose field must be present.
     *
     * @param prefix the marker text, e.g. "n/"
     * @return the required marker
     */
    public static ArgumentMarker required(String prefix) {
        return new ArgumentMarker(prefix, true);
    }

    /**
     * Declares a marker whose field may be omitted.
     *
     * @param prefix the marker text, e.g. "note/"
     * @return the optional marker
     */
    public static ArgumentMarker optional(String prefix) {
        return new ArgumentMarker(prefix, false);
    }

    /** Returns the marker text, e.g. "n/". */
    public String getPrefix() {
        return prefix;
    }

    /** Returns whether this marker's field must be present. */
    public boolean isRequired() {
        return required;
    }
}
