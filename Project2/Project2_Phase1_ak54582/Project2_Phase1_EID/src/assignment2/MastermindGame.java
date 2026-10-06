package assignment2;

/**
 * Mastermind rules: what a legal guess looks like, how it is scored, and
 * where the secret comes from. Attempt counting, history and win/loss
 * bookkeeping are inherited from {@link AbstractGuessingGame}.
 *
 * MASTERMIND-SPECIFIC.
 */
public final class MastermindGame extends AbstractGuessingGame {

    private final GameConfiguration config;
    private final CodeGenerator generator;
    private String secret;

    public MastermindGame(GameConfiguration config, CodeGenerator generator) {
        super(config.getMaxGuesses());
        this.config = config;
        this.generator = generator;
        startNewRound();
    }

    @Override
    protected void onNewRound() {
        secret = generator.nextCode(config);
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
    public String validate(String input) {
        if (input == null) {
            return "no input";
        }
        String g = normalize(input);
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
    protected Turn evaluate(String validInput) {
        String g = normalize(validInput);
        Feedback fb = MastermindScorer.score(secret, g);
        return new Turn(g, fb.toString(), fb.getBlack() == config.getPegCount());
    }

    @Override
    public String revealSecret() {
        return secret;
    }

    /**
     * Trims, then lets the player type colors in either case: a character is
     * used as typed if it is a legal color, otherwise its upper- or lower-case
     * form is used if that is legal. Anything else is left alone (and will be
     * rejected by validate).
     */
    private String normalize(String input) {
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
