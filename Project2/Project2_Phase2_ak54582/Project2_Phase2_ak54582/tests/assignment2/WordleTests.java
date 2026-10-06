package assignment2;

import static assignment2.TestSupport.*;

import java.io.File;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Dependency-free test + stress suite for Wordle. Run:
 *   java -cp out assignment2.WordleTests
 * Exit code 0 = all passed.
 *
 * Game-level tests use a small in-memory WordList so each case is easy to
 * check by hand; the "Real word file" and "Driver / launcher" sections load
 * the external data/words.txt exactly as the game does.
 */
public class WordleTests {

    /** Test dictionary. The real game loads data/words.txt; tests keep their own fixture. */
    static final WordList WORDS = new WordList(5, Arrays.asList(
            "APPLE", "ALLEY", "CRANE", "EERIE", "STEAM", "LEVEL", "THOSE", "GEESE", "SPEED", "ERASE",
            "ROBOT", "FLOOR", "PLANE", "KAYAK", "KNACK", "ABBEY", "SLATE", "HELLO", "WORLD", "STEAL"));

    private static String fb(String secret, String guess) {
        return WordleFeedback.score(secret, guess).toString();
    }

    private static WordleGame game(String... secrets) {
        return new WordleGame(WordleConfiguration.defaults(), WORDS, new FixedSecretSource(secrets));
    }

    private static String session(WordleGame g, boolean testMode, String input) throws Exception {
        return TestSupport.session(g, testMode, input);
    }

    private static String repeat(String line, int times) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < times; i++) sb.append(line);
        return sb.toString();
    }

    // ---------- independent reference scorer (nested loops with used-flags) ----------

    /** Written separately from PositionMatcher (no maps, no shared code) to cross-check it. */
    static String oracle(String s, String g) {
        int n = s.length();
        char[] out = new char[n];
        boolean[] used = new boolean[n];
        for (int i = 0; i < n; i++) {
            if (s.charAt(i) == g.charAt(i)) { out[i] = 'C'; used[i] = true; }
        }
        for (int i = 0; i < n; i++) {
            if (out[i] != 0) continue;
            out[i] = 'A';
            for (int j = 0; j < n; j++) {
                if (!used[j] && s.charAt(j) == g.charAt(i)) { out[i] = 'P'; used[j] = true; break; }
            }
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) { if (i > 0) sb.append(' '); sb.append(out[i]); }
        return sb.toString();
    }

    // ---------- tests ----------

    public static void main(String[] args) throws Exception {
        runAll();
        finish();
    }

    static void runAll() throws Exception {
        scoringRepeatedLetters();
        scoringOtherCases();
        scoringExhaustiveVsOracle();
        scoringProperties();
        wordList();
        realWordFile();
        configuration();
        gameRules();
        consoleSessions();
        changedConfiguration();
        driverAndLauncher();
        fuzz();
        solverEveryWord();
    }

    static void scoringRepeatedLetters() {
        section("Scoring: repeated letters (hand-verified)");
        // Guess has MORE copies of a letter than the secret.
        check("spec example: APPLE / ALLEY = C P A P A (two L's guessed, one in secret: only the first is P)",
                fb("APPLE", "ALLEY").equals("C P A P A"));
        check("STEAM / EERIE = P A A A A (three E's guessed, one in secret, none in place: only the first E is P)",
                fb("STEAM", "EERIE").equals("P A A A A"));
        check("LEVEL / EERIE = P C A A A (secret has two E's, guess three: one C, one P, the third A)",
                fb("LEVEL", "EERIE").equals("P C A A A"));
        // One occurrence is CORRECT, so another occurrence of the same letter must NOT get PRESENT.
        check("WORLD / HELLO = A A A C P (the only L is C at position 4, so the L at position 3 is A, not P)",
                fb("WORLD", "HELLO").equals("A A A C P"));
        check("THOSE / GEESE = A A A C C (the secret's one E is C at the end; the two earlier E's are A)",
                fb("THOSE", "GEESE").equals("A A A C C"));
        check("CRANE / EERIE = A A P A C (the C match comes LAST but is assigned first; earlier E's are A)",
                fb("CRANE", "EERIE").equals("A A P A C"));
        check("ROBOT / FLOOR = A A P C P (one O is C, the other O is P because the secret has a second O)",
                fb("ROBOT", "FLOOR").equals("A A P C P"));
        // Secret has repeats.
        check("SPEED / ERASE = P A A P P (both have two E's, none in place: both E's are P)",
                fb("SPEED", "ERASE").equals("P A A P P"));
        check("APPLE / PLANE = P P P A C (secret has two P's, guess one: that P is P once)",
                fb("APPLE", "PLANE").equals("P P P A C"));
        check("HELLO / LLAMA = P P A A A (secret has two L's, so both guessed L's are P)",
                fb("HELLO", "LLAMA").equals("P P A A A"));
        check("ABBEY / BABES = P P C C A (one B is C, the other B is P via the secret's second B)",
                fb("ABBEY", "BABES").equals("P P C C A"));
        check("KAYAK / KNACK = C A P A C (both K's C; one A guessed against two in secret)",
                fb("KAYAK", "KNACK").equals("C A P A C"));
        check("EERIE / EERIE = C C C C C (all repeats in place)", fb("EERIE", "EERIE").equals("C C C C C"));
    }

    static void scoringOtherCases() {
        section("Scoring: other cases and the feedback value");
        check("exact match = C C C C C and solved", fb("CRANE", "CRANE").equals("C C C C C")
                && WordleFeedback.score("CRANE", "CRANE").isSolved());
        check("no letters in common = A A A A A", fb("CRANE", "BOOST").equals("A A A A A"));
        check("anagram STEAL / SLATE = C P P P P", fb("STEAL", "SLATE").equals("C P P P P"));
        check("one letter off is not solved", !WordleFeedback.score("CRANE", "CRANK").isSolved());
        check("format: one code per letter, single spaces, C/P/A only",
                fb("APPLE", "ALLEY").matches("[CPA]( [CPA]){4}"));
        check("one-letter words work: A / A = C, A / B = A", fb("A", "A").equals("C") && fb("A", "B").equals("A"));
        boolean threw = false;
        try { WordleFeedback.score("CRANE", "CRAN"); } catch (IllegalArgumentException e) { threw = true; }
        check("words of different lengths are rejected", threw);
        threw = false;
        try { WordleFeedback.score("CRANE", "CRANE").marks().set(0, PositionMatch.ABSENT); }
        catch (UnsupportedOperationException e) { threw = true; }
        check("marks() is unmodifiable", threw);
        check("equal feedback values are equal (equals/hashCode)",
                WordleFeedback.score("APPLE", "ALLEY").equals(WordleFeedback.score("APPLE", "ALLEY"))
                && WordleFeedback.score("APPLE", "ALLEY").hashCode() == WordleFeedback.score("APPLE", "ALLEY").hashCode()
                && !WordleFeedback.score("APPLE", "ALLEY").equals(WordleFeedback.score("APPLE", "APPLE")));
        check("codes: CORRECT=C, PRESENT=P, ABSENT=A",
                WordleFeedback.code(PositionMatch.CORRECT) == 'C' && WordleFeedback.code(PositionMatch.PRESENT) == 'P'
                && WordleFeedback.code(PositionMatch.ABSENT) == 'A');
    }

    static void scoringExhaustiveVsOracle() {
        section("Scoring: EVERY secret x EVERY guess over small alphabets vs independent reference");
        String[][] spaces = { {"ABC", "5"}, {"ABCDE", "4"}, {"AB", "7"} };
        for (String[] sp : spaces) {
            List<String> all = allCodes(sp[0], Integer.parseInt(sp[1]));
            long pairs = 0, mismatches = 0;
            for (String s : all) for (String g : all) {
                pairs++;
                if (!fb(s, g).equals(oracle(s, g))) mismatches++;
            }
            System.out.println("  (" + sp[0].length() + " letters, length " + sp[1] + ": compared " + pairs + " pairs)");
            check("0 mismatches: alphabet " + sp[0] + ", length " + sp[1] + " (every repeat pattern)", mismatches == 0);
        }
    }

    static void scoringProperties() {
        section("Scoring: the repeated-letter rule as properties (200,000 random pairs, length 1-10)");
        Random r = new Random(2026);
        String letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        boolean perLetter = true, correctWhereEqual = true, presentOnlyIfInSecret = true, solvedIffEqual = true,
                oracleOk = true, mastermindAgrees = true;
        for (int t = 0; t < 200_000; t++) {
            int n = 1 + r.nextInt(10);
            int k = 1 + r.nextInt(6); // few distinct letters => many repeats
            StringBuilder a = new StringBuilder(), b = new StringBuilder();
            for (int i = 0; i < n; i++) { a.append(letters.charAt(r.nextInt(k))); b.append(letters.charAt(r.nextInt(k))); }
            String s = a.toString(), g = b.toString();
            List<PositionMatch> m = WordleFeedback.score(s, g).marks();
            int c = 0, p = 0;
            for (int i = 0; i < n; i++) {
                if ((m.get(i) == PositionMatch.CORRECT) != (s.charAt(i) == g.charAt(i))) correctWhereEqual = false;
                if (m.get(i) == PositionMatch.PRESENT && s.indexOf(g.charAt(i)) < 0) presentOnlyIfInSecret = false;
                if (m.get(i) == PositionMatch.CORRECT) c++;
                if (m.get(i) == PositionMatch.PRESENT) p++;
            }
            for (int x = 0; x < k; x++) {
                char ch = letters.charAt(x);
                int inS = 0, inG = 0, scored = 0;
                for (int i = 0; i < n; i++) {
                    if (s.charAt(i) == ch) inS++;
                    if (g.charAt(i) == ch) { inG++; if (m.get(i) != PositionMatch.ABSENT) scored++; }
                }
                if (scored != Math.min(inS, inG)) perLetter = false;
            }
            if (WordleFeedback.score(s, g).isSolved() != s.equals(g)) solvedIffEqual = false;
            if (!WordleFeedback.score(s, g).toString().equals(oracle(s, g))) oracleOk = false;
            MastermindFeedback mm = MastermindScorer.score(s, g);
            if (mm.getBlack() != c || mm.getWhite() != p) mastermindAgrees = false;
        }
        check("per letter: #C + #P == min(copies in secret, copies in guess), so no secret letter is used twice",
                perLetter);
        check("C exactly where the letters are equal", correctWhereEqual);
        check("P is never given for a letter the secret does not contain", presentOnlyIfInSecret);
        check("solved <=> guess equals secret", solvedIffEqual);
        check("matches the independent reference on every random pair", oracleOk);
        check("shared matcher: Wordle's #C/#P equal Mastermind's black/white for the same strings", mastermindAgrees);
    }

    static void wordList() throws Exception {
        section("WordList: parsing the external dictionary format");
        WordList w = new WordList(5, Arrays.asList("  apple ", "Crane", "", "   ", "# comment", "#APPLE",
                "APPLE", "apples", "ap-le", "12345", "\u00c4PFEL", "ALLEY\t", null));
        check("case folded and trimmed; blanks, comments, duplicates and invalid entries skipped",
                w.words().equals(Arrays.asList("APPLE", "CRANE", "ALLEY")));
        check("contains() is exact on canonical words", w.contains("APPLE") && !w.contains("apple") && !w.contains("APPLES"));
        check("size() and wordLength()", w.size() == 3 && w.wordLength() == 5);
        boolean threw = false;
        try { w.words().add("HELLO"); } catch (UnsupportedOperationException e) { threw = true; }
        check("words() is unmodifiable", threw);
        threw = false;
        try { new WordList(5, Arrays.asList("cat", "# only short words")); } catch (IllegalArgumentException e) { threw = true; }
        check("a list with no words of the right length is rejected", threw);
        check("canonical(): trims and maps a-z to A-Z only, never changing the length",
                WordList.canonical(" apple\t").equals("APPLE") && WordList.canonical("stra\u00dfe").equals("STRA\u00dfE"));

        Path tmp = Files.createTempFile("words", ".txt");
        try {
            try (Writer out = new OutputStreamWriter(Files.newOutputStream(tmp), "UTF-8")) {
                out.write("\ufeffapple\r\n# comment\r\n\r\ncrane\r\nCRANE\r\ntoolong\r\n");
            }
            WordList f = WordList.load(tmp, 5);
            check("load(): UTF-8 with byte-order mark and CRLF line endings",
                    f.words().equals(Arrays.asList("APPLE", "CRANE")));
        } finally {
            Files.deleteIfExists(tmp);
        }
        threw = false;
        try { WordList.load(Paths.get("no-such-word-file.txt"), 5); } catch (java.io.IOException e) { threw = true; }
        check("load() of a missing file throws IOException", threw);
    }

    static void realWordFile() throws Exception {
        section("Real word file: data/words.txt (external data, not in source)");
        Path file = Driver.findFile(Driver.DEFAULT_WORD_FILE);
        check("data/words.txt is found", Files.isRegularFile(file));
        WordList real = WordList.load(file, 5);
        int entries = 0;
        for (String line : Files.readAllLines(file)) {
            if (!line.trim().isEmpty() && !line.trim().startsWith("#")) entries++;
        }
        System.out.println("  (" + real.size() + " words)");
        check("at least 2,000 words", real.size() >= 2000);
        check("every entry is a valid 5-letter word and none is a duplicate (nothing skipped)", entries == real.size());
        boolean allValid = true;
        for (String word : real.words()) if (!word.matches("[A-Z]{5}")) allValid = false;
        check("every word is exactly 5 letters A-Z", allValid);
        check("contains the words used in the examples and tests",
                real.contains("APPLE") && real.contains("ALLEY") && real.contains("CRANE") && real.contains("EERIE"));
    }

    static void configuration() {
        section("WordleConfiguration");
        WordleConfiguration d = WordleConfiguration.defaults();
        check("defaults are 6 guesses / 5 letters", d.getMaxGuesses() == 6 && d.getWordLength() == 5);
        check("0 guesses rejected", !ok(0, 5));
        check("0 letters rejected", !ok(6, 0));
        check("negative values rejected", !ok(-1, -1));
        check("1 guess / 1 letter allowed", ok(1, 1));

        String g = setProperty("wordle.guesses", null), l = setProperty("wordle.length", null);
        try {
            WordleConfiguration p = WordleConfiguration.fromSystemProperties();
            check("no properties -> defaults", p.getMaxGuesses() == 6 && p.getWordLength() == 5);
            setProperty("wordle.guesses", "8");
            setProperty("wordle.length", "4");
            p = WordleConfiguration.fromSystemProperties();
            check("-Dwordle.guesses and -Dwordle.length are read", p.getMaxGuesses() == 8 && p.getWordLength() == 4);
        } finally {
            setProperty("wordle.guesses", g);
            setProperty("wordle.length", l);
        }
        boolean threw = false;
        try { new WordleGame(new WordleConfiguration(6, 4), WORDS, new FixedSecretSource("APPLE")); }
        catch (IllegalArgumentException e) { threw = true; }
        check("a 5-letter word list with a 4-letter game is rejected", threw);
    }

    private static boolean ok(int guesses, int length) {
        try { new WordleConfiguration(guesses, length); return true; } catch (IllegalArgumentException e) { return false; }
    }

    static void gameRules() {
        section("Game rules via the API");
        WordleGame g = game("APPLE");
        check("starts IN_PROGRESS with 6 attempts left",
                g.status() == GuessingGame.Status.IN_PROGRESS && g.attemptsRemaining() == 6 && g.maxAttempts() == 6);

        String[] bad = {"", "APP", "APPLES", "APPL3", "AP LE", "ZZZZZ", "     ", "\t", "\u00c4PPLE", "apple!", "A.P.P"};
        int thrown = 0;
        for (String b : bad) {
            try { g.submit(b); } catch (IllegalArgumentException e) { thrown++; }
        }
        check("all " + bad.length + " invalid inputs rejected (length, non-letters, not in word list)", thrown == bad.length);
        check("invalid guesses consume NO attempts", g.attemptsUsed() == 0 && g.history().isEmpty());
        // String.valueOf: a broken validate() returning null must FAIL these checks, not crash the suite.
        check("wrong length -> message names the required length",
                String.valueOf(g.validate("APP")).contains("5 letters"));
        check("non-letter -> message says letters A-Z only", String.valueOf(g.validate("APPL3")).contains("A-Z"));
        check("unknown word -> message says it is not in the word list",
                String.valueOf(g.validate("zzzzz")).equals("ZZZZZ is not in the word list"));
        check("null input rejected", g.validate(null) != null);

        Turn t = g.submit("  alley ");
        check("lowercase / padded input accepted and normalized to ALLEY", t.getGuess().equals("ALLEY"));
        check("feedback recorded on the turn: C P A P A", t.getFeedback().equals("C P A P A") && !t.isSolved());
        check("valid guess consumes exactly 1 attempt", g.attemptsUsed() == 1 && g.attemptsRemaining() == 5);
        try { g.submit("XXXXX"); } catch (IllegalArgumentException e) { /* expected */ }
        check("an invalid guess between valid ones changes nothing", g.attemptsUsed() == 1 && g.history().size() == 1);

        WordleGame w = game("APPLE");
        t = w.submit("APPLE");
        check("correct guess -> WON, solved, C C C C C",
                w.status() == GuessingGame.Status.WON && t.isSolved() && t.getFeedback().equals("C C C C C"));
        boolean threw = false;
        try { w.submit("APPLE"); } catch (IllegalStateException e) { threw = true; }
        check("submit after game over throws IllegalStateException", threw);

        WordleGame l = game("APPLE");
        for (int i = 0; i < 5; i++) l.submit("CRANE");
        check("still IN_PROGRESS after 5 wrong guesses", l.status() == GuessingGame.Status.IN_PROGRESS);
        l.submit("CRANE");
        check("LOST after the 6th wrong guess", l.status() == GuessingGame.Status.LOST && l.attemptsRemaining() == 0);
        check("secret revealed after a loss", l.revealSecret().equals("APPLE"));

        WordleGame last = game("APPLE");
        for (int i = 0; i < 5; i++) last.submit("CRANE");
        last.submit("APPLE");
        check("winning on the LAST (6th) guess is a win, not a loss", last.status() == GuessingGame.Status.WON);

        threw = false;
        try { l.history().add(new Turn("x", "y", false)); } catch (UnsupportedOperationException e) { threw = true; }
        check("history list is unmodifiable", threw);

        WordleGame two = game("APPLE", "CRANE");
        two.submit("ALLEY");
        two.startNewRound();
        check("startNewRound: next fixed secret, empty history, IN_PROGRESS, 6 attempts",
                two.revealSecret().equals("CRANE") && two.history().isEmpty()
                && two.status() == GuessingGame.Status.IN_PROGRESS && two.attemptsRemaining() == 6);

        threw = false;
        try { game("ZZZZZ"); } catch (IllegalStateException e) { threw = true; }
        check("a fixed secret that is not in the word list is rejected (IllegalStateException)", threw);
        threw = false;
        try { game("APPLES"); } catch (IllegalStateException e) { threw = true; }
        check("a fixed secret of the wrong length is rejected", threw);
        check("a fixed secret is normalized like a guess ('apple' -> APPLE)", game(" apple ").revealSecret().equals("APPLE"));

        RandomWordSource src = new RandomWordSource(WORDS, new Random(5));
        Set<String> seen = new HashSet<>();
        boolean allFromList = true;
        for (int i = 0; i < 5000; i++) {
            String s = src.nextSecret();
            if (!WORDS.contains(s)) allFromList = false;
            seen.add(s);
        }
        check("random word source: 5000 secrets, all from the word list", allFromList);
        check("random word source reaches every word in a 20-word list", seen.size() == WORDS.size());
        RandomWordSource a = new RandomWordSource(WORDS, new Random(9)), b = new RandomWordSource(WORDS, new Random(9));
        boolean same = true;
        for (int i = 0; i < 100; i++) if (!a.nextSecret().equals(b.nextSecret())) same = false;
        check("same seed -> same sequence of secrets (reproducible)", same);
    }

    static void consoleSessions() throws Exception {
        section("Console sessions (scripted input, same ConsoleRunner as Mastermind)");
        String o = session(game("APPLE"), true, "ALLEY\nAPPLE\nN\n");
        check("banner shows the Wordle title and rules", o.contains("=== Wordle ===")
                && o.contains("You have 6 guesses") && o.contains("C = right letter in the right spot"));
        check("test mode reveals the secret", o.contains("[TEST MODE] Secret: APPLE"));
        check("feedback printed as C/P/A letters", o.contains("Result: C P A P A"));
        check("win reported with the number of guesses", o.contains("You win! Solved in 2 guess(es)."));
        check("declining replay ends cleanly", o.contains("Thanks for playing!"));

        o = session(game("APPLE"), false, "APPLE\nN\n");
        check("normal mode does NOT reveal the secret", !o.contains("[TEST MODE]") && !o.contains("Secret:"));

        o = session(game("APPLE"), false, "APP\nZZZZZ\nAPP1E\n\nAPPLE\nN\n");
        check("invalid guesses are reported", count(o, "INVALID_GUESS") == 4);
        check("not-in-list guess reported by name", o.contains("INVALID_GUESS: ZZZZZ is not in the word list"));
        check("prompt counter did not advance during invalid guesses", count(o, "Guess 1 of 6") == 5);

        o = session(game("APPLE"), false, "HISTORY\nALLEY\nCRANE\nHISTORY\nhistory\nAPPLE\nN\n");
        check("HISTORY before any guess says none yet", o.contains("No guesses yet."));
        check("HISTORY lists prior valid guesses with feedback, oldest first",
                o.contains("1. ALLEY  C P A P A") && o.contains("2. CRANE  A A P A C")
                && o.indexOf("1. ALLEY") < o.indexOf("2. CRANE"));
        check("HISTORY is case-insensitive and repeatable", count(o, "--- History ---") == 2);
        check("HISTORY does not consume an attempt", o.contains("Guess 3 of 6"));

        o = session(game("APPLE"), false, "ALLEY\nZZZZZ\nHISTORY\nAPPLE\nN\n");
        check("an invalid guess never appears in HISTORY", o.contains("1. ALLEY") && !o.contains("2. "));

        o = session(game("APPLE"), false, repeat("CRANE\n", 6) + "N\n");
        check("6 wrong guesses -> loss message with the secret", o.contains("You lose! The secret was APPLE."));
        check("no 7th prompt after the loss", !o.contains("Guess 7"));

        o = session(game("APPLE", "CRANE"), true, "APPLE\nY\nALLEY\nHISTORY\nCRANE\nN\n");
        check("play again: two rounds, two wins", count(o, "You win!") == 2);
        check("fixed secrets used in order; the first is not skipped",
                o.indexOf("Secret: APPLE") >= 0 && o.indexOf("Secret: APPLE") < o.indexOf("Secret: CRANE"));
        check("the new round starts with a clean history",
                o.contains("1. ALLEY  P A A P A") && !o.contains("1. APPLE"));

        o = session(game("APPLE"), false, "ALLEY\n");
        check("input ending mid-game exits without crashing", o.contains("Result: C P A P A"));
        o = session(game("APPLE"), false, "APPLE\n");
        check("input ending at the replay prompt exits without crashing", o.contains("You win!"));
        o = session(game("APPLE"), false, "");
        check("completely empty input exits without crashing", o.contains("Enter your guess"));
        o = session(game("APPLE"), false, "  apple  \r\nN\r\n");
        check("surrounding whitespace / CRLF line endings tolerated", o.contains("You win!"));
    }

    static void changedConfiguration() throws Exception {
        section("Changed configuration (game logic untouched)");
        WordList four = new WordList(4, Arrays.asList("COLD", "CORD", "CARD", "WARD", "WORD"));
        WordleGame g = new WordleGame(new WordleConfiguration(3, 4), four, new FixedSecretSource("WORD"));
        String o = session(g, false, "APPLE\nCARD\nCORD\nWORD\nN\n");
        check("4 letters / 3 guesses: rules mention both", o.contains("secret 4-letter word") && o.contains("You have 3 guesses"));
        check("5-letter guess rejected when the game wants 4", o.contains("INVALID_GUESS: a guess must be exactly 4 letters"));
        check("4-letter feedback: CARD = A A C C, CORD = A C C C", o.contains("Result: A A C C") && o.contains("Result: A C C C"));
        check("solved on the 3rd (last) guess", o.contains("You win! Solved in 3 guess(es)."));

        g = new WordleGame(new WordleConfiguration(2, 5), WORDS, new FixedSecretSource("APPLE"));
        o = session(g, false, "CRANE\nALLEY\nN\n");
        check("2 guesses max -> loss after 2 valid guesses", o.contains("You lose!") && o.contains("Guess 2 of 2"));
    }

    static void driverAndLauncher() throws Exception {
        section("Driver / launcher: both games from one program");
        String[] keys = {"wordle.secret", "wordle.words", "wordle.length", "wordle.guesses", "mastermind.secret"};
        String[] saved = new String[keys.length];
        for (int i = 0; i < keys.length; i++) saved[i] = setProperty(keys[i], null);
        Path tmp = Files.createTempFile("four", ".txt");
        try {
            GuessingGame g = Driver.createGame("wordle", false);
            check("createGame('wordle') builds Wordle from data/words.txt (6 guesses, real word as secret)",
                    g instanceof WordleGame && g.maxAttempts() == 6 && g.validate(g.revealSecret()) == null);
            check("game name is case-insensitive ('WORDLE')", Driver.createGame("WORDLE", false) instanceof WordleGame);
            check("unknown game -> null", Driver.createGame("tetris", false) == null);

            setProperty("wordle.secret", "apple, CRANE");
            g = Driver.createGame("wordle", true);
            String first = g.revealSecret();
            g.startNewRound();
            check("test mode: -Dwordle.secret entries used in order, normalized", first.equals("APPLE")
                    && g.revealSecret().equals("CRANE"));

            setProperty("wordle.secret", "APPLE,QQQQQ");
            boolean threw = false;
            try { Driver.createGame("wordle", true); } catch (IllegalArgumentException e) { threw = true; }
            check("a test secret that is not in the word list is rejected at startup", threw);

            setProperty("wordle.secret", null);
            Files.write(tmp, Arrays.asList("cold", "cord", "word"));
            setProperty("wordle.words", tmp.toString());
            setProperty("wordle.length", "4");
            g = Driver.createGame("wordle", false);
            check("-Dwordle.words and -Dwordle.length switch to another dictionary",
                    g.validate("cord") == null && g.validate("APPLE") != null);

            setProperty("wordle.length", null);
            threw = false;
            try { Driver.createGame("wordle", false); } catch (IllegalArgumentException e) { threw = true; }
            check("a dictionary with no words of the configured length is rejected", threw);

            setProperty("wordle.words", "no-such-dir/words.txt");
            String msg = "";
            try { Driver.createGame("wordle", false); } catch (IllegalStateException e) { msg = e.getMessage(); }
            check("a missing word file -> IllegalStateException that names the file", msg.contains("no-such-dir/words.txt"));
        } finally {
            for (int i = 0; i < keys.length; i++) setProperty(keys[i], saved[i]);
            Files.deleteIfExists(tmp);
        }

        // Real "java assignment2.Driver ..." processes.
        String[] r = launch(new String[] {"wordle", "test"}, "ALLEY\nHISTORY\nAPPLE\nN\n",
                new String[] {"-Dwordle.secret=APPLE"}, null);
        check("launcher: wordle test + -Dwordle.secret: revealed, scored, HISTORY, win, exit 0",
                r[0].equals("0") && r[1].contains("[TEST MODE] Secret: APPLE") && r[1].contains("Result: C P A P A")
                && r[1].contains("1. ALLEY  C P A P A") && r[1].contains("You win! Solved in 2 guess(es)."));
        r = launch(new String[] {"wordle", "test"}, "CRANE\nN\n", null, null, "WORDLE_SECRET", "CRANE");
        check("launcher: WORDLE_SECRET environment variable controls the secret",
                r[1].contains("Secret: CRANE") && r[1].contains("Result: C C C C C"));
        r = launch(new String[] {"wordle"}, "N\n", new String[] {"-Dwordle.secret=APPLE"}, null);
        check("launcher: normal mode never reveals the secret", r[0].equals("0") && !r[1].contains("Secret:"));
        r = launch(new String[] {"mastermind", "test"}, "BGOP\nN\n", new String[] {"-Dmastermind.secret=BGOP"}, null);
        check("launcher: mastermind still runs from the same program",
                r[0].equals("0") && r[1].contains("=== Mastermind ===") && r[1].contains("Result: 4B_0W"));
        File outDir = new File("out");
        if (outDir.isDirectory()) {
            r = launch(new String[] {"wordle", "test"}, "APPLE\nN\n", new String[] {"-Dwordle.secret=APPLE"}, outDir);
            check("launcher: started from out/, data/words.txt is still found", r[0].equals("0") && r[1].contains("You win!"));
        }
        r = launch(new String[] {"wordle"}, "", new String[] {"-Dwordle.words=missing.txt"}, null);
        check("launcher: missing word file -> one-line error, exit 1, no stack trace",
                r[0].equals("1") && r[2].contains("Error: word list not found: missing.txt") && !r[2].contains("\tat "));
        r = launch(new String[] {"wordle", "test"}, "", new String[] {"-Dwordle.secret=QQQQQ"}, null);
        check("launcher: test secret not in the word list -> error, exit 1",
                r[0].equals("1") && r[2].contains("not in the word list") && !r[2].contains("\tat "));
        r = launch(new String[] {}, "", null, null);
        check("launcher: no arguments -> usage naming both games, exit 1",
                r[0].equals("1") && r[2].contains("mastermind|wordle"));
        r = launch(new String[] {"tetris"}, "", null, null);
        check("launcher: unknown game -> message, exit 1", r[0].equals("1") && r[2].contains("Unknown game 'tetris'"));
    }

    static void fuzz() throws Exception {
        section("Fuzz: garbage input must never crash or burn an attempt");
        Random r = new Random(4321);
        String alphabet = "APLECRNYSTMaplecrny01 \t!#\u00e9\u00c4\u4e2d\ud83d\ude00HISTORYhistory-_,.";
        boolean invariant = true;
        int valid = 0, invalid = 0;
        WordleGame g = game("APPLE");
        for (int t = 0; t < 100_000; t++) {
            if (g.status() != GuessingGame.Status.IN_PROGRESS) g.startNewRound();
            String in;
            if (r.nextInt(5) == 0) {
                in = WORDS.words().get(r.nextInt(WORDS.size()));
                if (r.nextBoolean()) in = in.toLowerCase();
            } else {
                StringBuilder sb = new StringBuilder();
                for (int i = r.nextInt(8); i > 0; i--) sb.append(alphabet.charAt(r.nextInt(alphabet.length())));
                in = sb.toString();
            }
            int before = g.attemptsUsed();
            String problem = g.validate(in);
            try {
                g.submit(in);
                if (problem != null || g.attemptsUsed() != before + 1) invariant = false;
                valid++;
            } catch (IllegalArgumentException e) {
                if (problem == null || g.attemptsUsed() != before) invariant = false;
                invalid++;
            }
        }
        System.out.println("  (100,000 random inputs: " + valid + " valid, " + invalid + " invalid)");
        check("validate/submit agree; only valid guesses consume attempts", invariant);
        check("fuzz exercised both valid and invalid paths", valid > 100 && invalid > 100);

        boolean noCrash = true;
        StringBuilder in = new StringBuilder();
        String[] tokens = {"HISTORY", "history", "Y", "N", "", " ", "APPLE", "alley", "CRANE", "ZZZZZ", "\u00e9\u00e9\u00e9\u00e9\u00e9"};
        for (int i = 0; i < 20_000; i++) {
            if (r.nextInt(3) == 0) in.append(tokens[r.nextInt(tokens.length)]);
            else for (int k = r.nextInt(8); k > 0; k--) in.append(alphabet.charAt(r.nextInt(alphabet.length())));
            in.append('\n');
        }
        try {
            session(game("APPLE", "CRANE", "EERIE"), true, in.toString());
        } catch (Throwable t) {
            noCrash = false;
            t.printStackTrace(System.out);
        }
        check("20,000 random console lines: no exceptions", noCrash);

        String o = session(game("APPLE"), false, repeat("A", 1_000_000) + "\nAPPLE\nN\n");
        check("1,000,000-character line is rejected and the game continues",
                o.contains("INVALID_GUESS") && o.contains("You win!"));
    }

    static void solverEveryWord() throws Exception {
        section("Solver: play every word in data/words.txt as the secret through the real game object");
        WordList real = WordList.load(Driver.findFile(Driver.DEFAULT_WORD_FILE), 5);
        List<String> all = real.words();
        int wins = 0, consistent = 0, ended = 0;
        long total = 0;
        int[] byGuesses = new int[7];
        for (String secret : all) {
            WordleGame g = new WordleGame(WordleConfiguration.defaults(), real, new FixedSecretSource(secret));
            List<String> candidates = new ArrayList<>(all);
            boolean secretKept = true;
            while (g.status() == GuessingGame.Status.IN_PROGRESS) {
                String guess = candidates.get(0);
                Turn t = g.submit(guess);
                List<String> next = new ArrayList<>();
                for (String c : candidates) if (oracle(c, guess).equals(t.getFeedback())) next.add(c);
                candidates = next;
                if (!candidates.contains(secret)) { secretKept = false; break; }
            }
            if (secretKept) consistent++;
            if (g.status() != GuessingGame.Status.IN_PROGRESS && g.attemptsUsed() <= 6) ended++;
            if (g.status() == GuessingGame.Status.WON) {
                wins++;
                total += g.attemptsUsed();
                byGuesses[g.attemptsUsed()]++;
            }
        }
        System.out.println("  (measured: wins=" + wins + "/" + all.size() + " = "
                + String.format("%.1f", 100.0 * wins / all.size()) + "%, average "
                + String.format("%.3f", total / (double) wins) + " guesses when won; by guesses 1-6: "
                + Arrays.toString(Arrays.copyOfRange(byGuesses, 1, 7)) + ")");
        check("the game's feedback always agrees with the independent reference (secret never ruled out)",
                consistent == all.size());
        check("every game ends in WON or LOST within 6 valid guesses", ended == all.size());
    }
}
