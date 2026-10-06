package assignment2;

import java.util.List;
import java.util.Random;

/**
 * Normal play: each secret is a uniformly random word from the word list.
 *
 * WORDLE-SPECIFIC.
 */
public class RandomWordSource implements SecretSource {
    private final List<String> words;
    private final Random random;

    public RandomWordSource(WordList words) {
        this(words, new Random());
    }

    /** Pass a seeded Random for reproducible "random" games. */
    public RandomWordSource(WordList words, Random random) {
        this.words = words.words();
        this.random = random;
    }

    @Override
    public String nextSecret() {
        return words.get(random.nextInt(words.size()));
    }
}
