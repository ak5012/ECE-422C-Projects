package assignment2;

/**
 * Immutable Mastermind settings: number of guesses, pegs per code, legal colors.
 * The game algorithm reads everything from here, so changing a setting never
 * requires touching game logic.
 *
 * MASTERMIND-SPECIFIC.
 */
public final class GameConfiguration {

    public static final int DEFAULT_MAX_GUESSES = 12;
    public static final int DEFAULT_PEG_COUNT   = 4;
    public static final String DEFAULT_COLORS   = "BGOPRY";
    public static final int MAX_COLORS          = 10;

    private final int maxGuesses;
    private final int pegCount;
    private final String colors;

    /**
     * @param colors one character per color, all distinct, 1 to 10 of them
     * @throws IllegalArgumentException on any invalid setting
     */
    public GameConfiguration(int maxGuesses, int pegCount, String colors) {
        if (maxGuesses < 1) {
            throw new IllegalArgumentException("maxGuesses must be at least 1");
        }
        if (pegCount < 1) {
            throw new IllegalArgumentException("pegCount must be at least 1");
        }
        if (colors == null || colors.isEmpty()) {
            throw new IllegalArgumentException("colors must not be empty");
        }
        if (colors.length() > MAX_COLORS) {
            throw new IllegalArgumentException("at most " + MAX_COLORS + " colors are allowed");
        }
        for (int i = 0; i < colors.length(); i++) {
            char c = colors.charAt(i);
            if (Character.isWhitespace(c) || Character.isISOControl(c)) {
                throw new IllegalArgumentException("colors must be visible characters");
            }
            if (colors.indexOf(c) != i) {
                throw new IllegalArgumentException("duplicate color '" + c + "'");
            }
        }
        this.maxGuesses = maxGuesses;
        this.pegCount = pegCount;
        this.colors = colors;
    }

    public static GameConfiguration defaults() {
        return new GameConfiguration(DEFAULT_MAX_GUESSES, DEFAULT_PEG_COUNT, DEFAULT_COLORS);
    }

    /**
     * Optional overrides via JVM properties, e.g.
     * java -Dmastermind.pegs=6 -Dmastermind.colors=0123456789 -Dmastermind.guesses=8 assignment2.Driver mastermind
     */
    public static GameConfiguration fromSystemProperties() {
        int guesses = Integer.getInteger("mastermind.guesses", DEFAULT_MAX_GUESSES);
        int pegs    = Integer.getInteger("mastermind.pegs", DEFAULT_PEG_COUNT);
        String cols = System.getProperty("mastermind.colors", DEFAULT_COLORS);
        return new GameConfiguration(guesses, pegs, cols);
    }

    public int getMaxGuesses() { return maxGuesses; }
    public int getPegCount()   { return pegCount; }
    public String getColors()  { return colors; }

    public boolean isLegalColor(char c) {
        return colors.indexOf(c) >= 0;
    }

    @Override
    public String toString() {
        return "GameConfiguration[guesses=" + maxGuesses + ", pegs=" + pegCount + ", colors=" + colors + "]";
    }
}
