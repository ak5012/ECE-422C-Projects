package assignment2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Entry point: java assignment2.Driver mastermind [test]
 *
 * Picks a game by name and hands it to the generic {@link ConsoleRunner}.
 * Adding another game later means adding one case to {@link #createGame}.
 *
 * Testing mode ("test"): the secret is printed at the start of every round.
 * To also control it, set -Dmastermind.secret=BGOP (or several, comma-separated,
 * used in order) or the MASTERMIND_SECRET environment variable.
 */
public class Driver {

    private static final String USAGE = "Usage: java assignment2.Driver mastermind [test]";

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

    /** @return the requested game, or null if the name is not recognised. */
    static GuessingGame createGame(String name, boolean testMode) {
        switch (name.toLowerCase()) {
            case "mastermind":
                GameConfiguration config = GameConfiguration.fromSystemProperties();
                CodeGenerator generator = new RandomCodeGenerator();
                if (testMode) {
                    String forced = System.getProperty("mastermind.secret",
                            System.getenv("MASTERMIND_SECRET"));
                    if (forced != null && !forced.trim().isEmpty()) {
                        List<String> codes = new ArrayList<>();
                        for (String c : forced.split(",")) {
                            codes.add(c.trim());
                        }
                        generator = new FixedCodeGenerator(codes);
                    }
                }
                return new MastermindGame(config, generator);
            default:
                return null;
        }
    }
}
