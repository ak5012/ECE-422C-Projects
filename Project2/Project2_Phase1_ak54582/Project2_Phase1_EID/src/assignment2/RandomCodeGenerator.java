package assignment2;

import java.util.Random;

/** Normal play: each peg is an independent uniform random color (repeats allowed). */
public class RandomCodeGenerator implements CodeGenerator {
    private final Random random;

    public RandomCodeGenerator() {
        this(new Random());
    }

    /** Pass a seeded Random for reproducible "random" games. */
    public RandomCodeGenerator(Random random) {
        this.random = random;
    }

    @Override
    public String nextCode(GameConfiguration config) {
        StringBuilder sb = new StringBuilder();
        String colors = config.getColors();
        for (int i = 0; i < config.getPegCount(); i++) {
            sb.append(colors.charAt(random.nextInt(colors.length())));
        }
        return sb.toString();
    }
}
