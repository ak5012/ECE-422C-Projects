package assignment2;

/**
 * Mastermind rules: what a legal guess looks like and how it is scored.
 * Attempt counting, history, win/loss bookkeeping and the secret's life
 * cycle are inherited from {@link AbstractGuessingGame}.
 *
 * MASTERMIND-SPECIFIC.
 */
public final class MastermindGame extends AbstractGuessingGame {

    private final MastermindConfiguration config;

    public MastermindGame(MastermindConfiguration config, SecretSource secrets) {
        super(config.getMaxGuesses(), secrets);
        this.config = config;
        startNewRound();
    }

    @Override
    public String title() {
        return "Mastermind";
    }

    @Override
    public String describeRules() {
        return "I have picked a secret code of " + config.getPegCount() + " pegs.\n"
             + "Legal colors: " + spaced(config.getColors()) + " (colors may repeat).\n"
             + "You have " + config.getMaxGuesses() + " guesses.\n"
             + "Feedback is nB_mW: n black pegs = right color in the right spot, "
             + "m white pegs = right color in the wrong spot.";
    }

    @Override
    protected String checkGuess(String g) {
        if (g.length() != config.getPegCount()) {
            return "a guess must be exactly " + config.getPegCount() + " pegs (you typed "
                    + g.length() + " character(s))";
        }
        for (int i = 0; i < g.length(); i++) {
            if (!config.isLegalColor(g.charAt(i))) {
                return "'" + g.charAt(i) + "' is not a legal color. Legal colors: "
                        + spaced(config.getColors());
            }
        }
        return null;
    }

    @Override
    protected Turn evaluate(String secret, String guess) {
        MastermindFeedback fb = MastermindScorer.score(secret, guess);
        return new Turn(guess, fb.toString(), fb.getBlack() == config.getPegCount());
    }

    /**
     * Trims, then lets the player type colors in either case: a character is
     * used as typed if it is a legal color, otherwise its upper- or lower-case
     * form is used if that is legal. Anything else is left alone (and will be
     * rejected by checkGuess).
     */
    @Override
    protected String normalize(String input) {
        String t = input.trim();
        StringBuilder sb = new StringBuilder(t.length());
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            if (config.isLegalColor(c)) {
                sb.append(c);
            } else if (config.isLegalColor(Character.toUpperCase(c))) {
                sb.append(Character.toUpperCase(c));
            } else if (config.isLegalColor(Character.toLowerCase(c))) {
                sb.append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String spaced(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            if (i > 0) sb.append(' ');
            sb.append(s.charAt(i));
        }
        return sb.toString();
    }
}
