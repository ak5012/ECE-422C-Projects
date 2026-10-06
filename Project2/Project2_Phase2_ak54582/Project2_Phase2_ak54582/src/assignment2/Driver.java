package assignment2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Entry point: java assignment2.Driver mastermind|wordle [test]
 *
 * Picks a game by name and hands it to the generic {@link ConsoleRunner}.
 * Adding another game means adding one case to {@link #createGame} and a
 * small factory method; argument handling and testing mode are shared.
 *
 * Testing mode ("test"): the secret is printed at the start of every round.
 * To also control it, set -D&lt;game&gt;.secret (several comma-separated
 * secrets are used in order) or the &lt;GAME&gt;_SECRET environment variable:
 *   -Dmastermind.secret=BGOP,RRGG    or  MASTERMIND_SECRET=BGOP
 *   -Dwordle.secret=APPLE,CRANE      or  WORDLE_SECRET=APPLE
 * Every forced secret is checked against the game's rules at startup.
 *
 * Wordle reads its dictionary from an external file: data/words.txt by
 * default, or -Dwordle.words=PATH.
 */
public class Driver {

    private static final String USAGE = "Usage: java assignment2.Driver mastermind|wordle [test]";

    /** Wordle dictionary used when -Dwordle.words is not given. */
    static final String DEFAULT_WORD_FILE = "data/words.txt";

    public static void main(String[] args) throws IOException {
        if (args.length < 1 || args.length > 2
                || (args.length == 2 && !args[1].equalsIgnoreCase("test"))) {
            System.err.println(USAGE);
            System.exit(1);
            return;
        }
        boolean testMode = args.length == 2;

        GuessingGame game;
        try {
            game = createGame(args[0], testMode);
        } catch (IllegalArgumentException | IllegalStateException e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
            return;
        }
        if (game == null) {
            System.err.println("Unknown game '" + args[0] + "'. " + USAGE);
            System.exit(1);
            return;
        }

        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        new ConsoleRunner(game, in, System.out, testMode).run();
    }

    /**
     * @return the requested game, or null if the name is not recognised
     * @throws IllegalArgumentException if a setting or a forced test secret is invalid
     * @throws IllegalStateException    if the Wordle word list cannot be read
     */
    static GuessingGame createGame(String name, boolean testMode) {
        String key = name.toLowerCase(Locale.ROOT);
        List<String> forced = testMode ? forcedSecrets(key) : null;
        GuessingGame game;
        switch (key) {
            case "mastermind":
                game = createMastermind(forced);
                break;
            case "wordle":
                game = createWordle(forced);
                break;
            default:
                return null;
        }
        // The first secret was checked when the game started its first round; check the
        // rest now so a bad later entry is reported before play, not in the middle of a replay.
        if (forced != null) {
            for (String s : forced) {
                String problem = game.validate(s);
                if (problem != null) {
                    throw new IllegalArgumentException("test secret '" + s + "' cannot be used: " + problem);
                }
            }
        }
        return game;
    }

    private static GuessingGame createMastermind(List<String> forced) {
        MastermindConfiguration config = MastermindConfiguration.fromSystemProperties();
        SecretSource secrets = forced != null ? new FixedSecretSource(forced) : new RandomCodeSource(config);
        return new MastermindGame(config, secrets);
    }

    private static GuessingGame createWordle(List<String> forced) {
        WordleConfiguration config = WordleConfiguration.fromSystemProperties();
        WordList words = loadWordList(System.getProperty("wordle.words", DEFAULT_WORD_FILE),
                config.getWordLength());
        SecretSource secrets = forced != null ? new FixedSecretSource(forced) : new RandomWordSource(words);
        return new WordleGame(config, words, secrets);
    }

    /**
     * Testing-mode secrets for a game: -D&lt;game&gt;.secret, else the
     * &lt;GAME&gt;_SECRET environment variable, comma-separated.
     * @return the secrets in order, or null if neither is set
     */
    static List<String> forcedSecrets(String game) {
        String raw = System.getProperty(game + ".secret",
                System.getenv(game.toUpperCase(Locale.ROOT) + "_SECRET"));
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        List<String> secrets = new ArrayList<>();
        for (String s : raw.split(",", -1)) {
            secrets.add(s.trim());
        }
        return secrets;
    }

    static WordList loadWordList(String fileName, int wordLength) {
        Path file = findFile(fileName);
        try {
            return WordList.load(file, wordLength);
        } catch (NoSuchFileException e) {
            throw new IllegalStateException("word list not found: " + fileName
                    + " (run from the project folder, or set -Dwordle.words=PATH)");
        } catch (IOException e) {
            throw new IllegalStateException("cannot read word list " + fileName + ": " + e.getMessage());
        }
    }

    /**
     * Resolves a relative file name against the working directory, then up to
     * three parent directories, so the game also starts when launched from a
     * subfolder such as out/ or src/.
     */
    static Path findFile(String name) {
        Path path = Paths.get(name);
        if (path.isAbsolute() || Files.isRegularFile(path)) {
            return path;
        }
        Path dir = Paths.get("").toAbsolutePath().getParent();
        for (int i = 0; i < 3 && dir != null; i++, dir = dir.getParent()) {
            Path candidate = dir.resolve(name);
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
        }
        return path;
    }
}
