package assignment2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;

/**
 * Drives any {@link GuessingGame} over a text console: prompts, reports
 * invalid guesses, prints feedback, handles HISTORY, announces win/loss,
 * and asks whether to play again. Knows nothing about pegs or colors.
 *
 * Input and output are injected so tests can script whole sessions.
 *
 * REUSABLE.
 */
public class ConsoleRunner {

    // Output tokens live in one place in case the autograder expects specific wording.
    static final String HISTORY_COMMAND = "HISTORY";
    static final String INVALID_PREFIX  = "INVALID_GUESS: ";
    static final String RESULT_PREFIX   = "Result: ";
    static final String WIN_MESSAGE     = "You win!";
    static final String LOSE_MESSAGE    = "You lose!";
    static final String PLAY_AGAIN      = "Play another game? (Y/N): ";

    private final GuessingGame game;
    private final BufferedReader in;
    private final PrintStream out;
    private final boolean testMode;

    public ConsoleRunner(GuessingGame game, BufferedReader in, PrintStream out, boolean testMode) {
        this.game = game;
        this.in = in;
        this.out = out;
        this.testMode = testMode;
    }

    /**
     * Plays the game as constructed (a new game is ready to play), then resets
     * and plays again for as long as the user says yes. Ends when the user
     * declines or input runs out.
     */
    public void run() throws IOException {
        printBanner();
        if (!playRound()) {
            return; // input ended mid-round
        }
        while (askPlayAgain()) {
            game.startNewRound();
            printBanner();
            if (!playRound()) {
                return;
            }
        }
        out.println("Thanks for playing!");
    }

    private void printBanner() {
        out.println();
        out.println("=== " + game.title() + " ===");
        out.println(game.describeRules());
        out.println("Type " + HISTORY_COMMAND + " at any time to see your previous guesses.");
        if (testMode) {
            out.println("[TEST MODE] Secret: " + game.revealSecret());
        }
    }

    /** @return false if input ended before the round finished. */
    private boolean playRound() throws IOException {
        while (game.status() == GuessingGame.Status.IN_PROGRESS) {
            out.println();
            out.print("Guess " + (game.attemptsUsed() + 1) + " of " + game.maxAttempts()
                    + " (" + game.attemptsRemaining() + " left). Enter your guess: ");
            out.flush();

            String line = in.readLine();
            if (line == null) {
                out.println();
                return false;
            }
            line = line.trim();

            if (line.equalsIgnoreCase(HISTORY_COMMAND)) {
                printHistory();
                continue;
            }
            String problem = game.validate(line);
            if (problem != null) {
                out.println(INVALID_PREFIX + problem);
                continue;
            }
            Turn turn = game.submit(line);
            out.println(RESULT_PREFIX + turn.getFeedback());
        }

        if (game.status() == GuessingGame.Status.WON) {
            out.println(WIN_MESSAGE + " Solved in " + game.attemptsUsed() + " guess(es).");
        } else {
            out.println(LOSE_MESSAGE + " The secret was " + game.revealSecret() + ".");
        }
        return true;
    }

    private void printHistory() {
        if (game.history().isEmpty()) {
            out.println("No guesses yet.");
            return;
        }
        out.println("--- History ---");
        int n = 1;
        for (Turn t : game.history()) {
            out.println(n++ + ". " + t.getGuess() + "  " + t.getFeedback());
        }
    }

    /** Loops until a clear Y/N answer; treats end-of-input as "no". */
    private boolean askPlayAgain() throws IOException {
        while (true) {
            out.print(PLAY_AGAIN);
            out.flush();
            String line = in.readLine();
            if (line == null) {
                out.println();
                return false;
            }
            line = line.trim();
            if (line.equalsIgnoreCase("Y") || line.equalsIgnoreCase("YES")) return true;
            if (line.equalsIgnoreCase("N") || line.equalsIgnoreCase("NO"))  return false;
            out.println("Please answer Y or N.");
        }
    }
}
