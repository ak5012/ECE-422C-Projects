package assignment2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Bookkeeping that every "N attempts to guess a secret" game shares:
 * attempt limit, history, win/loss status, the rule that an invalid guess
 * never consumes an attempt, and the secret itself (drawing it from a
 * {@link SecretSource}, checking it, and revealing it). Subclasses supply
 * only the game-specific parts: the canonical form of a guess, what a legal
 * guess is, and how a guess is scored.
 *
 * REUSABLE (template-method pattern).
 */
public abstract class AbstractGuessingGame implements GuessingGame {

    private final int maxAttempts;
    private final SecretSource secrets;
    private final List<Turn> history = new ArrayList<>();
    private Status status = Status.IN_PROGRESS;
    private String secret;

    protected AbstractGuessingGame(int maxAttempts, SecretSource secrets) {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be at least 1");
        }
        if (secrets == null) {
            throw new IllegalArgumentException("a secret source is required");
        }
        this.maxAttempts = maxAttempts;
        this.secrets = secrets;
    }

    // ---- hooks for the specific game ----

    /** Canonical form of raw text (trimmed, case-folded, ...). Applied to guesses and secrets alike. */
    protected abstract String normalize(String raw);

    /** @return null if the normalized text is a legal guess, otherwise a human-readable reason. */
    protected abstract String checkGuess(String normalized);

    /** Scores a legal, normalized guess against the secret. */
    protected abstract Turn evaluate(String secret, String guess);

    // ---- shared behaviour ----

    /**
     * Draws the next secret, then resets history and status. The secret must
     * itself be a legal guess (a secret the player cannot type could never be
     * won), so a bad fixed secret fails here instead of producing a broken round.
     * Subclass constructors call this once, after their own fields are set.
     *
     * @throws IllegalStateException if the source supplies an unusable secret
     */
    @Override
    public final void startNewRound() {
        String raw = secrets.nextSecret();
        if (raw == null) {
            throw new IllegalStateException("the secret source returned no secret");
        }
        String next = normalize(raw);
        String problem = checkGuess(next);
        if (problem != null) {
            throw new IllegalStateException("secret '" + raw + "' cannot be used: " + problem);
        }
        secret = next;
        history.clear();
        status = Status.IN_PROGRESS;
    }

    @Override
    public final String validate(String input) {
        if (input == null) {
            return "no input";
        }
        return checkGuess(normalize(input));
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
        Turn turn = evaluate(secret, normalize(input));
        history.add(turn);
        if (turn.isSolved()) {
            status = Status.WON;
        } else if (history.size() >= maxAttempts) {
            status = Status.LOST;
        }
        return turn;
    }

    @Override public final String revealSecret() { return secret; }
    @Override public final Status status()       { return status; }
    @Override public final int maxAttempts()     { return maxAttempts; }
    @Override public final int attemptsUsed()    { return history.size(); }
    @Override public final List<Turn> history()  { return Collections.unmodifiableList(history); }
}
