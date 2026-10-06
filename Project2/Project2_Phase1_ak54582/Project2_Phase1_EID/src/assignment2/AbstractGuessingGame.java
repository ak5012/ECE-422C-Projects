package assignment2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Bookkeeping that every "N attempts to guess a secret" game shares:
 * attempt limit, history, win/loss status, and the rule that an invalid
 * guess never consumes an attempt. Subclasses supply only the game-specific
 * parts: what a legal guess is, how it is scored, and how a new secret is made.
 *
 * REUSABLE (template-method pattern).
 */
public abstract class AbstractGuessingGame implements GuessingGame {

    private final int maxAttempts;
    private final List<Turn> history = new ArrayList<>();
    private Status status = Status.IN_PROGRESS;

    protected AbstractGuessingGame(int maxAttempts) {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be at least 1");
        }
        this.maxAttempts = maxAttempts;
    }

    // ---- hooks for the specific game ----

    /** Create a fresh secret / reset game-specific state. */
    protected abstract void onNewRound();

    /** Score an input that has already passed {@link #validate}. */
    protected abstract Turn evaluate(String validInput);

    // ---- shared behaviour ----

    @Override
    public final void startNewRound() {
        history.clear();
        status = Status.IN_PROGRESS;
        onNewRound();
    }

    @Override
    public final Turn submit(String input) {
        if (status != Status.IN_PROGRESS) {
            throw new IllegalStateException("Round is already over");
        }
        String problem = validate(input);
        if (problem != null) {
            throw new IllegalArgumentException(problem);
        }
        Turn turn = evaluate(input);
        history.add(turn);
        if (turn.isSolved()) {
            status = Status.WON;
        } else if (history.size() >= maxAttempts) {
            status = Status.LOST;
        }
        return turn;
    }

    @Override public final Status status()       { return status; }
    @Override public final int maxAttempts()     { return maxAttempts; }
    @Override public final int attemptsUsed()    { return history.size(); }
    @Override public final List<Turn> history()  { return Collections.unmodifiableList(history); }
}
