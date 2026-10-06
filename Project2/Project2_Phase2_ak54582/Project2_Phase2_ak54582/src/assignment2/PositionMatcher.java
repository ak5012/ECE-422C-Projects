package assignment2;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The repeated-symbol matching rule that Mastermind and Wordle share, isolated
 * as a pure function so it can be tested exhaustively without any I/O.
 *
 * Rule: exact matches (CORRECT) are assigned first and removed from
 * consideration. Then, left to right, each remaining guessed symbol is PRESENT
 * if an unclaimed occurrence of it is left in the secret (and claims it), or
 * ABSENT if none is left. So one occurrence in the secret never satisfies more
 * than one guessed symbol.
 *
 * REUSABLE. Extracted in Phase II from MastermindScorer, which applied the
 * same rule but returned only the two counts.
 */
public final class PositionMatcher {

    private PositionMatcher() { }

    /**
     * Both strings must have the same length.
     * @return one result per position of the guess (unmodifiable)
     */
    public static List<PositionMatch> match(String secret, String guess) {
        if (secret.length() != guess.length()) {
            throw new IllegalArgumentException("secret and guess must be the same length");
        }
        int n = secret.length();
        PositionMatch[] result = new PositionMatch[n];
        Map<Character, Integer> unclaimed = new HashMap<>();

        for (int i = 0; i < n; i++) {
            char s = secret.charAt(i);
            if (s == guess.charAt(i)) {
                result[i] = PositionMatch.CORRECT;
            } else {
                unclaimed.merge(s, 1, Integer::sum);
            }
        }
        for (int i = 0; i < n; i++) {
            if (result[i] != null) {
                continue;
            }
            char g = guess.charAt(i);
            Integer left = unclaimed.get(g);
            if (left != null && left > 0) {
                result[i] = PositionMatch.PRESENT;
                unclaimed.put(g, left - 1);
            } else {
                result[i] = PositionMatch.ABSENT;
            }
        }
        return Collections.unmodifiableList(Arrays.asList(result));
    }
}
