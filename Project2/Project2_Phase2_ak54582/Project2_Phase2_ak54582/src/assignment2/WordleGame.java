package assignment2;

/**
 * Wordle rules: what a legal guess looks like (a word of the configured
 * length that is in the word list) and how it is scored. Attempt counting,
 * history, win/loss bookkeeping and the secret's life cycle are inherited
 * from {@link AbstractGuessingGame}; repeated letters are resolved by the
 * shared {@link PositionMatcher}.
 *
 * WORDLE-SPECIFIC.
 */
public final class WordleGame extends AbstractGuessingGame {

    private final WordleConfiguration config;
    private final WordList words;

    public WordleGame(WordleConfiguration config, WordList words, SecretSource secrets) {
        super(config.getMaxGuesses(), secrets);
        if (words.wordLength() != config.getWordLength()) {
            throw new IllegalArgumentException("the word list has " + words.wordLength()
                    + "-letter words but the game is set to " + config.getWordLength() + " letters");
        }
        this.config = config;
        this.words = words;
        startNewRound();
    }

    @Override
    public String title() {
        return "Wordle";
    }

    @Override
    public String describeRules() {
        int n = config.getWordLength();
        return "I have picked a secret " + n + "-letter word.\n"
             + "You have " + config.getMaxGuesses() + " guesses. Each guess must be a " + n
             + "-letter word from the word list (" + words.size() + " words).\n"
             + "Feedback is one letter per position: C = right letter in the right spot, "
             + "P = letter is in the word but in another spot, A = letter is not in the word.\n"
             + "Each letter of the secret is matched at most once, so a repeated letter in "
             + "your guess scores only as often as it appears in the secret.";
    }

    @Override
    protected String normalize(String raw) {
        return WordList.canonical(raw);
    }

    @Override
    protected String checkGuess(String g) {
        int n = config.getWordLength();
        if (g.length() != n) {
            return "a guess must be exactly " + n + " letters (you typed "
                    + g.length() + " character(s))";
        }
        for (int i = 0; i < n; i++) {
            char c = g.charAt(i);
            if (c < 'A' || c > 'Z') {
                return "'" + c + "' is not a letter; a guess may use only the letters A-Z";
            }
        }
        if (!words.contains(g)) {
            return g + " is not in the word list";
        }
        return null;
    }

    @Override
    protected Turn evaluate(String secret, String guess) {
        WordleFeedback fb = WordleFeedback.score(secret, guess);
        return new Turn(guess, fb.toString(), fb.isSolved());
    }
}
