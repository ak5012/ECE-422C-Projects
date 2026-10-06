package assignment2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Wordle feedback: one mark per letter, printed as C (right letter, right
 * spot), P (letter is elsewhere in the secret) or A (absent), separated by
 * spaces, e.g. "C P A P A". Repeated letters are resolved by the shared
 * {@link PositionMatcher}.
 *
 * WORDLE-SPECIFIC.
 */
public final class WordleFeedback {
    private final List<PositionMatch> marks;

    public WordleFeedback(List<PositionMatch> marks) {
        if (marks == null || marks.isEmpty()) {
            throw new IllegalArgumentException("feedback needs at least one mark");
        }
        this.marks = Collections.unmodifiableList(new ArrayList<>(marks));
    }

    /** Scores guess against secret. Both words must have the same length. */
    public static WordleFeedback score(String secret, String guess) {
        return new WordleFeedback(PositionMatcher.match(secret, guess));
    }

    /** One mark per letter of the guess (unmodifiable). */
    public List<PositionMatch> marks() { return marks; }

    /** @return true if every letter is CORRECT */
    public boolean isSolved() {
        for (PositionMatch m : marks) {
            if (m != PositionMatch.CORRECT) {
                return false;
            }
        }
        return true;
    }

    /** The portable one-letter code for a mark: C, P or A. */
    public static char code(PositionMatch m) {
        switch (m) {
            case CORRECT: return 'C';
            case PRESENT: return 'P';
            default:      return 'A';
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (PositionMatch m : marks) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(code(m));
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof WordleFeedback && marks.equals(((WordleFeedback) o).marks);
    }

    @Override
    public int hashCode() {
        return marks.hashCode();
    }
}
