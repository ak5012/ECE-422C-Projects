package assignment2;

/**
 * Mastermind feedback: black pegs (right color, right place) and white pegs
 * (right color, wrong place). Prints as "nB_mW".
 *
 * MASTERMIND-SPECIFIC. Called Feedback in Phase I; renamed so it is not
 * mistaken for a shared type now that WordleFeedback exists.
 */
public final class MastermindFeedback {
    private final int black;
    private final int white;

    public MastermindFeedback(int black, int white) {
        this.black = black;
        this.white = white;
    }

    public int getBlack() { return black; }
    public int getWhite() { return white; }

    @Override
    public String toString() {
        return black + "B_" + white + "W";
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof MastermindFeedback)) return false;
        MastermindFeedback f = (MastermindFeedback) o;
        return black == f.black && white == f.white;
    }

    @Override
    public int hashCode() {
        return 31 * black + white;
    }
}
