package assignment2;

/**
 * Strategy for producing the secret code. Swapping the implementation is how
 * testing mode gets a deterministic secret without changing game logic.
 *
 * MASTERMIND-SPECIFIC (a different game would have its own secret type).
 */
public interface CodeGenerator {
    /** @return a code of exactly pegCount characters, each a legal color. */
    String nextCode(GameConfiguration config);
}
