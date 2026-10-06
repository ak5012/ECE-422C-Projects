package assignment2;

/**
 * One completed, valid turn: what the player guessed, how the game scored it,
 * and whether that guess solved the game. Immutable.
 *
 * REUSABLE: any turn-based guessing game can describe a turn as
 * "guess text + feedback text + solved?".
 */
public final class Turn {
    private final String guess;
    private final String feedback;
    private final boolean solved;

    public Turn(String guess, String feedback, boolean solved) {
        this.guess = guess;
        this.feedback = feedback;
        this.solved = solved;
    }

    public String getGuess()    { return guess; }
    public String getFeedback() { return feedback; }
    public boolean isSolved()   { return solved; }

    @Override
    public String toString() {
        return guess + " -> " + feedback;
    }
}
