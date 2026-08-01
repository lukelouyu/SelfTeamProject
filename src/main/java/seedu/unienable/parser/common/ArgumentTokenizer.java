package seedu.unienable.parser.common;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import seedu.unienable.exception.InvalidCommandException;
import seedu.unienable.exception.MissingInputException;

/**
 * A lightweight, declarative alternative to a hand-written sequence of {@link FieldParser} calls:
 * given a registry of {@link ArgumentMarker}s for one command, tokenizes a full argument string
 * into a marker-to-value map in a single pass.
 *
 * <p>Marker matching reuses the same field-boundary rule as {@link FieldParser#indexOfMarker}
 * (case-insensitive; a marker only counts at the start of input or immediately after whitespace),
 * so behaviour stays consistent with the existing per-command parsers. On top of that, this class
 * adds three things every existing parser previously had to hand-implement individually:
 *
 * <ul>
 *   <li><b>Required-field checking</b>: a marker declared with {@link ArgumentMarker#required} that
 *       is absent from the input raises {@link MissingInputException} naming it.</li>
 *   <li><b>Unknown-marker rejection (fail-fast)</b>: any other marker-shaped boundary token (e.g.
 *       "x/") that is not declared for this context raises {@link InvalidCommandException}, instead
 *       of silently being swallowed into the previous field's value.</li>
 *   <li><b>Basic quoting</b>: a value that starts with a double quote runs until the next double
 *       quote, taken literally - so it may contain spaces or another marker's prefix text without
 *       being split, e.g. {@code n/"CG3207 lecture"} or {@code note/"see c/o front desk"}. Outside
 *       quotes, a value still runs until the next recognised marker, exactly like
 *       {@link FieldParser#extractField}; an unquoted value containing a marker-shaped token that
 *       isn't declared (e.g. free text with a bare "A/B" after a space) is rejected rather than
 *       silently accepted - callers who need that must quote the value.</li>
 * </ul>
 *
 * <p>This class is not used by any v1.0 command; every existing command parser keeps calling
 * {@link FieldParser} directly, so v1.0 behaviour is completely unchanged. It exists so a v2.0
 * command with a more complex grammar (nested/optional arguments, quoted values) can declare its
 * markers once instead of hand-writing another {@code indexOf}-chain parser.
 */
public final class ArgumentTokenizer {
    private static final Pattern MARKER_SHAPE = Pattern.compile("[A-Za-z][A-Za-z0-9]*/");
    private static final char QUOTE = '"';

    private ArgumentTokenizer() {
    }

    /**
     * Tokenizes args against the given markers.
     *
     * @param args the full argument text (after the command word)
     * @param markers every marker recognised in this context, each declared required or optional
     * @return a map from each present marker's declared-case prefix to its trimmed, unquoted value
     * @throws MissingInputException if a required marker is absent
     * @throws InvalidCommandException if args contains text before the first recognised marker, a
     *     marker-shaped token that is not one of the declared markers, or a value with an
     *     unterminated quote
     */
    public static Map<String, String> tokenize(String args, ArgumentMarker... markers)
            throws MissingInputException, InvalidCommandException {
        String[] prefixes = toPrefixes(markers);
        rejectLeadingText(args, prefixes);

        Map<String, String> values = new LinkedHashMap<>();
        BoundaryMatch current = scanForward(args, 0, prefixes);
        while (current.prefix != null) {
            int valueStart = current.index + current.prefix.length();
            BoundaryMatch next;
            String value;
            if (valueStart < args.length() && args.charAt(valueStart) == QUOTE) {
                int closing = args.indexOf(QUOTE, valueStart + 1);
                if (closing == -1) {
                    throw new InvalidCommandException(current.prefix + " value has an unterminated quote.");
                }
                value = args.substring(valueStart + 1, closing);
                next = scanForward(args, closing + 1, prefixes);
            } else {
                next = scanForward(args, valueStart, prefixes);
                value = args.substring(valueStart, next.index).trim();
            }
            values.put(current.prefix, value);
            current = next;
        }

        for (ArgumentMarker marker : markers) {
            if (marker.isRequired() && !values.containsKey(marker.getPrefix())) {
                throw new MissingInputException(marker.getPrefix() + " is required.");
            }
        }
        return values;
    }

    /**
     * Rejects text before the first recognised marker, delegating to
     * {@link FieldParser#leadingUnrecognisedText} so this class's notion of "leading text" is
     * identical to every existing parser's.
     */
    private static void rejectLeadingText(String args, String[] prefixes) throws InvalidCommandException {
        String leading = FieldParser.leadingUnrecognisedText(args, prefixes);
        if (!leading.isEmpty()) {
            throw new InvalidCommandException("Unknown option \"" + leading + "\".");
        }
    }

    /**
     * Scans forward from {@code from} for the next marker-boundary position: index 0, or a
     * position immediately preceded by whitespace. At each boundary, checks whether a
     * marker-shaped token (letters followed by a slash) starts there. If it matches one of the
     * declared prefixes, that match is returned. If it matches the marker shape but no declared
     * prefix, this is an unknown marker and is rejected immediately (fail-fast) rather than being
     * silently folded into the current field's value. If no marker-shaped boundary token is found
     * before the end of the text, a match with a null prefix (positioned at the end of the text)
     * is returned, meaning "the current value runs to the end of input".
     */
    private static BoundaryMatch scanForward(String text, int from, String[] prefixes)
            throws InvalidCommandException {
        Matcher matcher = MARKER_SHAPE.matcher(text);
        for (int i = from; i < text.length(); i++) {
            boolean isBoundary = i == 0 || Character.isWhitespace(text.charAt(i - 1));
            if (!isBoundary) {
                continue;
            }
            matcher.region(i, text.length());
            if (!matcher.lookingAt()) {
                continue;
            }
            String candidate = matcher.group();
            String declared = matchIgnoreCase(candidate, prefixes);
            if (declared != null) {
                return new BoundaryMatch(i, declared);
            }
            throw new InvalidCommandException("Unrecognised option \"" + candidate + "\".");
        }
        return new BoundaryMatch(text.length(), null);
    }

    private static String matchIgnoreCase(String candidate, String[] prefixes) {
        for (String prefix : prefixes) {
            if (prefix.equalsIgnoreCase(candidate)) {
                return prefix;
            }
        }
        return null;
    }

    private static String[] toPrefixes(ArgumentMarker[] markers) {
        String[] prefixes = new String[markers.length];
        for (int i = 0; i < markers.length; i++) {
            prefixes[i] = markers[i].getPrefix();
        }
        return prefixes;
    }

    /** One marker occurrence found while scanning: its index, and its declared prefix (or null). */
    private static final class BoundaryMatch {
        private final int index;
        private final String prefix;

        private BoundaryMatch(int index, String prefix) {
            this.index = index;
            this.prefix = prefix;
        }
    }
}
