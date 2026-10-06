package assignment2;

/**
 * The Mastermind scoring rule: black = exact matches, white = right color in
 * the wrong spot, where each secret peg can pair with at most one guess peg.
 *
 * That repeated-color rule is the same one Wordle uses for repeated letters,
 * so the matching itself lives in the shared {@link PositionMatcher}; this
 * class only turns its per-position results into the two Mastermind counts.
 *
 * MASTERMIND-SPECIFIC.
 */
public final class MastermindScorer {

    private MastermindScorer() { }

    /** Both strings must have the same length. */
    public static MastermindFeedback score(String secret, String guess) {
        int black = 0;
        int white = 0;
        for (PositionMatch m : PositionMatcher.match(secret, guess)) {
            if (m == PositionMatch.CORRECT) {
                black++;
            } else if (m == PositionMatch.PRESENT) {
                white++;
            }
        }
        return new MastermindFeedback(black, white);
    }
}
