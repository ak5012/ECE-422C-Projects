package assignment2;

/**
 * How one guessed symbol relates to the secret, after repeated symbols have
 * been resolved by {@link PositionMatcher}.
 *
 * REUSABLE: Mastermind counts these (black = CORRECT, white = PRESENT);
 * Wordle shows one per letter (C / P / A).
 */
public enum PositionMatch {
    /** Same symbol in the same position. */
    CORRECT,
    /** The symbol has an unclaimed occurrence at another position of the secret. */
    PRESENT,
    /** No unclaimed occurrence of the symbol is left in the secret. */
    ABSENT
}
