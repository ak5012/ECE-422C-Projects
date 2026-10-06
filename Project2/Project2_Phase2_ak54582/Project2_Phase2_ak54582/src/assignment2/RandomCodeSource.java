package assignment2;

import java.util.Random;

/**
 * Normal play: each peg is an independent uniform random color (repeats allowed).
 *
 * MASTERMIND-SPECIFIC.
 */
public class RandomCodeSource implements SecretSource {
    private final MastermindConfiguration config;
    private final Random random;

    public RandomCodeSource(MastermindConfiguration config) {
        this(config, new Random());
    }

    /** Pass a seeded Random for reproducible "random" games. */
    public RandomCodeSource(MastermindConfiguration config, Random random) {
        this.config = config;
        this.random = random;
    }

    @Override
    public String nextSecret() {
        StringBuilder sb = new StringBuilder();
        String colors = config.getColors();
        for (int i = 0; i < config.getPegCount(); i++) {
            sb.append(colors.charAt(random.nextInt(colors.length())));
        }
        return sb.toString();
    }
}
