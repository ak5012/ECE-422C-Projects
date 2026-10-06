package assignment2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Testing mode: hands out a predetermined list of secrets in order, cycling
 * back to the start after the last one. Works for any game, because the game
 * itself checks each secret when a round starts.
 *
 * REUSABLE. Replaces the Phase I FixedCodeGenerator, which checked codes
 * against a Mastermind GameConfiguration.
 */
public class FixedSecretSource implements SecretSource {
    private final List<String> secrets;
    private int next = 0;

    public FixedSecretSource(List<String> secrets) {
        if (secrets == null || secrets.isEmpty()) {
            throw new IllegalArgumentException("need at least one secret");
        }
        for (String s : secrets) {
            if (s == null || s.trim().isEmpty()) {
                throw new IllegalArgumentException("a fixed secret must not be empty");
            }
        }
        this.secrets = new ArrayList<>(secrets);
    }

    public FixedSecretSource(String... secrets) {
        this(Arrays.asList(secrets));
    }

    @Override
    public String nextSecret() {
        String secret = secrets.get(next);
        next = (next + 1) % secrets.size();
        return secret;
    }
}
