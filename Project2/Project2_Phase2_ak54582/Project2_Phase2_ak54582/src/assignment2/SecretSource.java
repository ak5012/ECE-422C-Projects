package assignment2;

/**
 * Strategy for producing each round's secret. Swapping the implementation is
 * how testing mode gets a deterministic secret without changing game logic.
 *
 * A source does not need to know the game's rules: the game normalizes and
 * checks every secret it receives with the same rule it applies to guesses
 * (see {@link AbstractGuessingGame#startNewRound}).
 *
 * REUSABLE. Replaces the Phase I CodeGenerator, whose nextCode() took a
 * Mastermind GameConfiguration and so could not serve a second game.
 */
public interface SecretSource {
    /** @return the secret for the next round, as text. */
    String nextSecret();
}
