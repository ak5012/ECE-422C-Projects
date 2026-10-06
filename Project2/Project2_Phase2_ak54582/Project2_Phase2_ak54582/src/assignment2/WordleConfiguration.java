package assignment2;

/**
 * Immutable Wordle settings: number of guesses and word length. Like
 * MastermindConfiguration, the game reads everything from here, so changing
 * a setting never requires touching game logic. The two configuration classes
 * share no code because the only setting they have in common, the attempt
 * limit, is all that AbstractGuessingGame needs, and it takes that as an int.
 *
 * WORDLE-SPECIFIC.
 */
public final class WordleConfiguration {

    public static final int DEFAULT_MAX_GUESSES = 6;
    public static final int DEFAULT_WORD_LENGTH = 5;

    private final int maxGuesses;
    private final int wordLength;

    /** @throws IllegalArgumentException on any invalid setting */
    public WordleConfiguration(int maxGuesses, int wordLength) {
        if (maxGuesses < 1) {
            throw new IllegalArgumentException("maxGuesses must be at least 1");
        }
        if (wordLength < 1) {
            throw new IllegalArgumentException("wordLength must be at least 1");
        }
        this.maxGuesses = maxGuesses;
        this.wordLength = wordLength;
    }

    public static WordleConfiguration defaults() {
        return new WordleConfiguration(DEFAULT_MAX_GUESSES, DEFAULT_WORD_LENGTH);
    }

    /**
     * Optional overrides via JVM properties, e.g.
     * java -Dwordle.guesses=8 -Dwordle.length=4 -Dwordle.words=four.txt assignment2.Driver wordle
     * (the word file must then contain words of that length).
     */
    public static WordleConfiguration fromSystemProperties() {
        int guesses = Integer.getInteger("wordle.guesses", DEFAULT_MAX_GUESSES);
        int length  = Integer.getInteger("wordle.length", DEFAULT_WORD_LENGTH);
        return new WordleConfiguration(guesses, length);
    }

    public int getMaxGuesses() { return maxGuesses; }
    public int getWordLength() { return wordLength; }

    @Override
    public String toString() {
        return "WordleConfiguration[guesses=" + maxGuesses + ", length=" + wordLength + "]";
    }
}
