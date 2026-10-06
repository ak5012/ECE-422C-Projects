package assignment2;

import java.util.List;

/**
 * The contract between the console loop (game-independent) and a specific
 * game (game-specific). The console loop only talks to this interface, so a
 * different guessing game can be plugged in without touching the loop.
 *
 * REUSABLE.
 */
public interface GuessingGame {

    enum Status { IN_PROGRESS, WON, LOST }

    /** Display name, e.g. "Mastermind". */
    String title();

    /** Multi-line rules/intro text shown at the start of each round. */
    String describeRules();

    /**
     * Resets all round state (history, status, secret). A newly constructed game
     * is already ready to play; call this only to start a further round.
     */
    void startNewRound();

    Status status();

    int maxAttempts();

    /** Valid guesses made so far this round. */
    int attemptsUsed();

    default int attemptsRemaining() {
        return maxAttempts() - attemptsUsed();
    }

    /** @return null if the input is a legal guess, otherwise a human-readable reason. */
    String validate(String input);

    /**
     * Scores a valid guess and records it. Only valid guesses consume an attempt.
     * @throws IllegalArgumentException if {@link #validate} would reject the input
     * @throws IllegalStateException    if the round is already over
     */
    Turn submit(String input);

    /** Valid turns so far this round, oldest first (unmodifiable). */
    List<Turn> history();

    /** The secret as text. Used by testing mode and to show the answer after a loss. */
    String revealSecret();
}
