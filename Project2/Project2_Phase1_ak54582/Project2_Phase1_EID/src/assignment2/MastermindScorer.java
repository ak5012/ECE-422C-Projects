package assignment2;

import java.util.HashMap;
import java.util.Map;

/**
 * The Mastermind scoring rule, isolated as a pure function so it can be
 * tested exhaustively without any I/O.
 *
 * Rule: exact matches (black) are counted first and removed from
 * consideration. Each remaining secret peg can pair with at most one
 * remaining guess peg of the same color (white).
 *
 * MASTERMIND-SPECIFIC.
 */
public final class MastermindScorer {

    private MastermindScorer() { }

    /** Both strings must have the same length. */
    public static Feedback score(String secret, String guess) {
        if (secret.length() != guess.length()) {
            throw new IllegalArgumentException("secret and guess must be the same length");
        }
        int black = 0;
        Map<Character, Integer> secretLeft = new HashMap<>();
        Map<Character, Integer> guessLeft = new HashMap<>();

        for (int i = 0; i < secret.length(); i++) {
            char s = secret.charAt(i);
            char g = guess.charAt(i);
            if (s == g) {
                black++;
            } else {
                secretLeft.merge(s, 1, Integer::sum);
                guessLeft.merge(g, 1, Integer::sum);
            }
        }
        int white = 0;
        for (Map.Entry<Character, Integer> e : guessLeft.entrySet()) {
            Integer inSecret = secretLeft.get(e.getKey());
            if (inSecret != null) {
                white += Math.min(inSecret, e.getValue());
            }
        }
        return new Feedback(black, white);
    }
}
