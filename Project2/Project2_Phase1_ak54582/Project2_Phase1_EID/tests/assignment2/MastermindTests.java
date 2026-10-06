package assignment2;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Dependency-free test + stress suite. Run:
 *   java -cp out assignment2.MastermindTests
 * Exit code 0 = all passed.
 */
public class MastermindTests {

    private static int passed = 0;
    private static int failed = 0;

    // ---------- tiny harness ----------

    private static void check(String name, boolean ok) {
        if (ok) {
            passed++;
            System.out.println("  PASS  " + name);
        } else {
            failed++;
            System.out.println("  FAIL  " + name);
        }
    }

    private static void section(String s) {
        System.out.println("\n== " + s + " ==");
    }

    private static String session(GameConfiguration cfg, CodeGenerator gen, boolean testMode, String input)
            throws Exception {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(buf, true, "UTF-8");
        MastermindGame g = new MastermindGame(cfg, gen);
        new ConsoleRunner(g, new BufferedReader(new StringReader(input)), out, testMode).run();
        return buf.toString("UTF-8");
    }

    private static int count(String haystack, String needle) {
        int n = 0, i = 0;
        while ((i = haystack.indexOf(needle, i)) >= 0) { n++; i += needle.length(); }
        return n;
    }

    private static String fb(String secret, String guess) {
        return MastermindScorer.score(secret, guess).toString();
    }

    // ---------- independent reference scorer (classic "used flags" algorithm) ----------

    private static int[] oracle(String s, String g) {
        int n = s.length();
        boolean[] sUsed = new boolean[n];
        boolean[] gUsed = new boolean[n];
        int black = 0, white = 0;
        for (int i = 0; i < n; i++) {
            if (s.charAt(i) == g.charAt(i)) { black++; sUsed[i] = true; gUsed[i] = true; }
        }
        for (int i = 0; i < n; i++) {
            if (gUsed[i]) continue;
            for (int j = 0; j < n; j++) {
                if (!sUsed[j] && s.charAt(j) == g.charAt(i)) { white++; sUsed[j] = true; break; }
            }
        }
        return new int[] {black, white};
    }

    private static List<String> allCodes(String colors, int pegs) {
        List<String> out = new ArrayList<>();
        build("", colors, pegs, out);
        return out;
    }

    private static void build(String prefix, String colors, int left, List<String> out) {
        if (left == 0) { out.add(prefix); return; }
        for (int i = 0; i < colors.length(); i++) build(prefix + colors.charAt(i), colors, left - 1, out);
    }

    // ---------- tests ----------

    public static void main(String[] args) throws Exception {
        scoringKnownCases();
        scoringExhaustiveVsOracle();
        scoringProperties();
        configuration();
        gameRules();
        consoleSessions();
        changedConfiguration();
        fuzz();
        solverEveryDefaultSecret();

        System.out.println("\n----------------------------------------");
        System.out.println("passed: " + passed + "   failed: " + failed);
        System.exit(failed == 0 ? 0 : 1);
    }

    static void scoringKnownCases() {
        section("Scoring: hand-verified cases (incl. duplicate colors)");
        check("exact match RGBY/RGBY = 4B_0W", fb("RGBY", "RGBY").equals("4B_0W"));
        check("all misplaced RGBY/YBGR = 0B_4W", fb("RGBY", "YBGR").equals("0B_4W"));
        check("no overlap BBBB/OOOO = 0B_0W", fb("BBBB", "OOOO").equals("0B_0W"));
        check("guess spam: BBBB secret, guess BOOO = 1B_0W", fb("BBBB", "BOOO").equals("1B_0W"));
        check("secret has 1 R, guess RRRR = 1B_0W (one peg counts once)", fb("RGBY", "RRRR").equals("1B_0W"));
        check("RRGG vs RGRG = 2B_2W", fb("RRGG", "RGRG").equals("2B_2W"));
        check("BOOO vs OOBB = 1B_2W (exact match takes precedence)", fb("BOOO", "OOBB").equals("1B_2W"));
        check("RRRG vs GRRR = 2B_2W", fb("RRRG", "GRRR").equals("2B_2W"));
        check("PPGG vs GGPP = 0B_4W", fb("PPGG", "GGPP").equals("0B_4W"));
        check("ROGB vs OOOO = 1B_0W", fb("ROGB", "OOOO").equals("1B_0W"));
        check("BBOO vs OBBO = 2B_2W", fb("BBOO", "OBBO").equals("2B_2W"));
        check("BBGO vs OGBB = 0B_4W (2 B, 1 G, 1 O all present, none in place)",
                fb("BBGO", "OGBB").equals("0B_4W"));
        check("BBGO vs GBBB = 1B_2W (guess has 3 B but secret only 2)", fb("BBGO", "GBBB").equals("1B_2W"));
    }

    static void scoringExhaustiveVsOracle() {
        section("Scoring: EVERY secret x EVERY guess (6 colors, 4 pegs) vs independent reference");
        List<String> codes = allCodes("BGOPRY", 4);
        long pairs = 0, mismatches = 0;
        for (String s : codes) for (String g : codes) {
            Feedback f = MastermindScorer.score(s, g);
            int[] o = oracle(s, g);
            pairs++;
            if (f.getBlack() != o[0] || f.getWhite() != o[1]) mismatches++;
        }
        System.out.println("  (compared " + pairs + " pairs)");
        check("0 mismatches over all pairs", mismatches == 0);

        List<String> small = allCodes("0123", 5); // 4 colors, 5 pegs: heavy repeats
        long mm = 0, pp = 0;
        for (String s : small) for (String g : small) {
            Feedback f = MastermindScorer.score(s, g);
            int[] o = oracle(s, g);
            pp++;
            if (f.getBlack() != o[0] || f.getWhite() != o[1]) mm++;
        }
        System.out.println("  (compared " + pp + " pairs)");
        check("0 mismatches with heavy repeats (4 colors, 5 pegs)", mm == 0);
    }

    static void scoringProperties() {
        section("Scoring: properties on random large configs (10 colors, 1-12 pegs)");
        Random r = new Random(42);
        String colors = "0123456789";
        boolean sym = true, bounds = true, perfect = true, oracleOk = true;
        for (int t = 0; t < 200_000; t++) {
            int n = 1 + r.nextInt(12);
            StringBuilder a = new StringBuilder(), b = new StringBuilder();
            int k = 1 + r.nextInt(10); // vary number of distinct colors used
            for (int i = 0; i < n; i++) { a.append(colors.charAt(r.nextInt(k))); b.append(colors.charAt(r.nextInt(k))); }
            String s = a.toString(), g = b.toString();
            Feedback f1 = MastermindScorer.score(s, g), f2 = MastermindScorer.score(g, s);
            if (!f1.equals(f2)) sym = false;
            if (f1.getBlack() + f1.getWhite() > n) bounds = false;
            if ((f1.getBlack() == n) != s.equals(g)) perfect = false;
            int[] o = oracle(s, g);
            if (o[0] != f1.getBlack() || o[1] != f1.getWhite()) oracleOk = false;
        }
        check("score(s,g) == score(g,s) (symmetry)", sym);
        check("black + white <= pegs", bounds);
        check("black == pegs  <=>  guess equals secret", perfect);
        check("matches reference scorer on 200,000 random cases", oracleOk);
    }

    static void configuration() {
        section("GameConfiguration validation");
        check("defaults are 12 guesses / 4 pegs / BGOPRY",
                GameConfiguration.defaults().getMaxGuesses() == 12
                && GameConfiguration.defaults().getPegCount() == 4
                && GameConfiguration.defaults().getColors().equals("BGOPRY"));
        check("10 colors allowed", ok(1, 1, "0123456789"));
        check("11 colors rejected", !ok(1, 1, "0123456789A"));
        check("duplicate color rejected", !ok(12, 4, "BGOPRB"));
        check("empty colors rejected", !ok(12, 4, ""));
        check("0 pegs rejected", !ok(12, 0, "BGOPRY"));
        check("0 guesses rejected", !ok(0, 4, "BGOPRY"));
        check("negative values rejected", !ok(-1, -1, "BGOPRY"));
        check("space as a color rejected", !ok(12, 4, "B GOP"));
        check("1 color / 1 peg / 1 guess allowed", ok(1, 1, "X"));
    }

    private static boolean ok(int g, int p, String c) {
        try { new GameConfiguration(g, p, c); return true; } catch (IllegalArgumentException e) { return false; }
    }

    static void gameRules() {
        section("Game rules via the API");
        GameConfiguration cfg = GameConfiguration.defaults();

        MastermindGame g = new MastermindGame(cfg, new FixedCodeGenerator("BGOP"));
        check("starts IN_PROGRESS with 12 attempts left",
                g.status() == GuessingGame.Status.IN_PROGRESS && g.attemptsRemaining() == 12);

        int thrown = 0;
        String[] bad = {"", "BGO", "BGOPR", "BGOX", "1234", "B G O", "BGO!", "\t", "BGOP BGOP"};
        for (String b : bad) {
            try { g.submit(b); } catch (IllegalArgumentException e) { thrown++; }
        }
        check("all " + bad.length + " invalid inputs rejected", thrown == bad.length);
        check("invalid guesses consume NO attempts", g.attemptsUsed() == 0 && g.history().isEmpty());

        g.submit("bgop".equals("bgop") ? "ybgo" : "");
        check("lowercase input accepted and normalised to upper", g.history().get(0).getGuess().equals("YBGO"));
        check("valid guess consumes exactly 1 attempt", g.attemptsUsed() == 1);

        MastermindGame w = new MastermindGame(cfg, new FixedCodeGenerator("BGOP"));
        Turn t = w.submit("BGOP");
        check("correct guess -> WON and solved flag", w.status() == GuessingGame.Status.WON && t.isSolved());
        boolean threw = false;
        try { w.submit("BGOP"); } catch (IllegalStateException e) { threw = true; }
        check("submit after game over throws IllegalStateException", threw);

        MastermindGame l = new MastermindGame(cfg, new FixedCodeGenerator("BGOP"));
        for (int i = 0; i < 11; i++) l.submit("RRRR");
        check("still IN_PROGRESS after 11 wrong guesses", l.status() == GuessingGame.Status.IN_PROGRESS);
        l.submit("RRRR");
        check("LOST after 12th wrong guess", l.status() == GuessingGame.Status.LOST && l.attemptsRemaining() == 0);

        MastermindGame last = new MastermindGame(cfg, new FixedCodeGenerator("BGOP"));
        for (int i = 0; i < 11; i++) last.submit("RRRR");
        last.submit("BGOP");
        check("winning on the LAST allowed guess is a win, not a loss", last.status() == GuessingGame.Status.WON);

        check("history list is unmodifiable", unmodifiable(l));
        check("startNewRound resets history/status", resets(l));

        RandomCodeGenerator rg = new RandomCodeGenerator(new Random(7));
        boolean allLegal = true;
        for (int i = 0; i < 5000; i++) {
            String c = rg.nextCode(cfg);
            if (c.length() != 4) allLegal = false;
            for (char ch : c.toCharArray()) if (!cfg.isLegalColor(ch)) allLegal = false;
        }
        check("random generator: 5000 secrets all length 4 / legal colors", allLegal);

        boolean sawRepeat = false;
        for (int i = 0; i < 2000 && !sawRepeat; i++) {
            String c = rg.nextCode(cfg);
            if (c.charAt(0) == c.charAt(1) || c.charAt(1) == c.charAt(2) || c.charAt(2) == c.charAt(3)) sawRepeat = true;
        }
        check("random generator can produce repeated colors", sawRepeat);

        boolean rejected = false;
        try { new MastermindGame(cfg, new FixedCodeGenerator("BGOX")); } catch (IllegalStateException e) { rejected = true; }
        check("fixed generator rejects a secret with an illegal color", rejected);
    }

    private static boolean unmodifiable(MastermindGame g) {
        try { g.history().add(new Turn("x", "y", false)); return false; }
        catch (UnsupportedOperationException e) { return true; }
    }

    private static boolean resets(MastermindGame g) {
        g.startNewRound();
        return g.history().isEmpty() && g.status() == GuessingGame.Status.IN_PROGRESS && g.attemptsRemaining() == 12;
    }

    static void consoleSessions() throws Exception {
        section("Console sessions (scripted input)");
        GameConfiguration cfg = GameConfiguration.defaults();

        String o = session(cfg, new FixedCodeGenerator("BGOP"), false, "BGOP\nN\n");
        check("immediate win prints win message", o.contains("You win!"));
        check("win does NOT print the secret banner outside test mode", !o.contains("[TEST MODE]"));
        check("declining replay ends cleanly", o.contains("Thanks for playing!"));

        o = session(cfg, new FixedCodeGenerator("BGOP"), true, "BGOP\nN\n");
        check("test mode reveals the secret", o.contains("[TEST MODE] Secret: BGOP"));

        o = session(cfg, new FixedCodeGenerator("RRGG"), false, "RGRG\nN\n");
        check("feedback printed as nB_mW", o.contains("Result: 2B_2W"));

        o = session(cfg, new FixedCodeGenerator("BGOP"), false, "XXXX\nBG\nBGOPY\n\nBGOP\nN\n");
        check("invalid guesses are reported", count(o, "INVALID_GUESS") == 4);
        check("prompt counter did not advance during invalid guesses", count(o, "Guess 1 of 12") == 5);

        o = session(cfg, new FixedCodeGenerator("BGOP"), false, "HISTORY\nRRRR\nYYYY\nHISTORY\nhistory\nBGOP\nN\n");
        check("HISTORY before any guess says none yet", o.contains("No guesses yet."));
        check("HISTORY lists guesses in order with feedback",
                o.contains("1. RRRR  0B_0W") && o.contains("2. YYYY  0B_0W")
                && o.indexOf("1. RRRR") < o.indexOf("2. YYYY"));
        check("HISTORY is case-insensitive and repeatable", count(o, "--- History ---") == 2);
        check("HISTORY does not consume an attempt", o.contains("Guess 3 of 12"));

        StringBuilder loss = new StringBuilder();
        for (int i = 0; i < 12; i++) loss.append("RRRR\n");
        loss.append("N\n");
        o = session(cfg, new FixedCodeGenerator("BGOP"), false, loss.toString());
        check("12 wrong guesses -> loss message", o.contains("You lose!"));
        check("loss reveals the secret", o.contains("The secret was BGOP."));

        o = session(cfg, new FixedCodeGenerator("BGOP", "YYYY"), false, "BGOP\nY\nRRRR\nHISTORY\nYYYY\nN\n");
        check("play again: two rounds played", count(o, "You win!") == 2);
        check("new round starts with a clean history", o.contains("1. RRRR") && count(o, "1. RRRR") == 1
                && !o.substring(o.indexOf("Guess 1 of 12", o.indexOf("Play another"))).contains("1. BGOP"));

        o = session(cfg, new FixedCodeGenerator("BGOP", "YYYY"), true, "BGOP\nY\nYYYY\nN\n");
        check("REGRESSION: first secret from a fixed list is used first (not skipped)",
                o.indexOf("Secret: BGOP") >= 0 && o.indexOf("Secret: BGOP") < o.indexOf("Secret: YYYY"));

        o = session(cfg, new FixedCodeGenerator("BGOP"), false, "BGOP\nmaybe\n\nyes\nBGOP\nno\n");
        check("replay prompt re-asks on garbage, accepts 'yes'/'no'",
                count(o, "Please answer Y or N.") == 2 && count(o, "You win!") == 2);

        o = session(cfg, new FixedCodeGenerator("BGOP"), false, "RRRR\n");
        check("input ending mid-game exits without crashing", o.contains("Result: 0B_0W"));
        o = session(cfg, new FixedCodeGenerator("BGOP"), false, "BGOP\n");
        check("input ending at replay prompt exits without crashing", o.contains("You win!"));
        o = session(cfg, new FixedCodeGenerator("BGOP"), false, "");
        check("completely empty input exits without crashing", o.contains("Enter your guess"));

        o = session(cfg, new FixedCodeGenerator("BGOP"), false, "  bgop  \r\nN\r\n");
        check("surrounding whitespace / CRLF line endings tolerated", o.contains("You win!"));
    }

    static void changedConfiguration() throws Exception {
        section("Changed configuration (game algorithm untouched)");
        GameConfiguration cfg = new GameConfiguration(3, 6, "0123456789");
        String o = session(cfg, new FixedCodeGenerator("012345"), true, "543210\n000000\n012345\nN\n");
        check("6 pegs / 10 digit colors: banner mentions 6 pegs", o.contains("secret code of 6 pegs"));
        check("6 pegs: reversed guess = 0B_6W", o.contains("Result: 0B_6W"));
        check("6 pegs: '000000' vs 012345 = 1B_0W", o.contains("Result: 1B_0W"));
        check("6 pegs: solved on 3rd (last) guess", o.contains("You win! Solved in 3 guess(es)."));

        o = session(cfg, new FixedCodeGenerator("012345"), false, "0123\n01234567\nABCDEF\n111111\n222222\n333333\nN\n");
        check("4-peg guess rejected when config wants 6", count(o, "INVALID_GUESS") == 3);
        check("3 guesses max -> loss after 3 valid", o.contains("You lose!") && o.contains("Guess 3 of 3"));

        GameConfiguration tiny = new GameConfiguration(2, 1, "XY");
        o = session(tiny, new FixedCodeGenerator("Y"), false, "X\nY\nN\n");
        check("1 peg / 2 colors / 2 guesses: win on 2nd", o.contains("You win! Solved in 2 guess(es)."));

        GameConfiguration lower = new GameConfiguration(12, 4, "abcd");
        o = session(lower, new FixedCodeGenerator("abcd"), false, "ABCD\nN\n");
        check("lowercase color set: typing uppercase still works", o.contains("You win!"));

        GameConfiguration digits = new GameConfiguration(5, 4, "0123456789");
        boolean sameAsOracle = true;
        MastermindGame g = new MastermindGame(digits, new FixedCodeGenerator("0011"));
        g.submit("1100");
        sameAsOracle = g.history().get(0).getFeedback().equals("0B_4W");
        check("digit colors with repeats: 0011 vs 1100 = 0B_4W", sameAsOracle);
    }

    static void fuzz() throws Exception {
        section("Fuzz: garbage input must never crash or burn an attempt");
        Random r = new Random(1234);
        String alphabet = "BGOPRYbgopry0123 \t!@#\u00e9\u4e2d\ud83d\ude00HISTORYhistory-_,.";
        GameConfiguration cfg = GameConfiguration.defaults();

        // 1) API-level invariant: validate()==null  <=>  submit succeeds; attempts move only on success
        boolean invariant = true;
        int valid = 0, invalid = 0;
        MastermindGame g = new MastermindGame(cfg, new FixedCodeGenerator("BGOP"));
        for (int t = 0; t < 100_000; t++) {
            if (g.status() != GuessingGame.Status.IN_PROGRESS) g.startNewRound();
            int len = r.nextInt(8);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < len; i++) sb.append(alphabet.charAt(r.nextInt(alphabet.length())));
            String in = sb.toString();
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

        // 2) console-level: 20,000 random lines incl. HISTORY/Y/N, 3 configs, must not throw
        boolean noCrash = true;
        GameConfiguration[] cfgs = { cfg, new GameConfiguration(3, 6, "0123456789"), new GameConfiguration(1, 1, "X") };
        for (GameConfiguration c : cfgs) {
            StringBuilder in = new StringBuilder();
            String[] tokens = {"HISTORY", "history", "Y", "N", "", " ", "BGOP", "RRRR", "0123", "X", "x", "\u00e9\u00e9",
                    "01234567890123456789", "!!!!"};
            for (int i = 0; i < 20_000; i++) {
                if (r.nextInt(4) == 0) in.append(tokens[r.nextInt(tokens.length)]);
                else for (int k = r.nextInt(8); k > 0; k--) in.append(alphabet.charAt(r.nextInt(alphabet.length())));
                in.append('\n');
            }
            try {
                String secret = c.getMaxGuesses() == 1 ? "X" : (c.getPegCount() == 6 ? "012345" : "BGOP");
                session(c, new RandomOrFixed(secret), false, in.toString());
            } catch (Throwable t) {
                noCrash = false;
                t.printStackTrace(System.out);
            }
        }
        check("60,000 random console lines across 3 configs: no exceptions", noCrash);

        // 3) very long line
        StringBuilder huge = new StringBuilder();
        for (int i = 0; i < 1_000_000; i++) huge.append('B');
        String o = session(cfg, new FixedCodeGenerator("BGOP"), false, huge + "\nBGOP\nN\n");
        check("1,000,000-character line is rejected quickly and game continues",
                o.contains("INVALID_GUESS") && o.contains("You win!"));
    }

    /** Fixed secret, but only checked lazily so 1-color / 1-peg configs work in fuzz. */
    private static class RandomOrFixed implements CodeGenerator {
        private final String s;
        RandomOrFixed(String s) { this.s = s; }
        @Override public String nextCode(GameConfiguration c) { return s; }
    }

    static void solverEveryDefaultSecret() {
        section("Solver: play all 1,296 possible default secrets through the real game object");
        GameConfiguration cfg = GameConfiguration.defaults();
        List<String> all = allCodes(cfg.getColors(), cfg.getPegCount());
        int worst = 0, wins = 0;
        long total = 0;
        String worstSecret = "";
        for (String secret : all) {
            MastermindGame game = new MastermindGame(cfg, new FixedCodeGenerator(secret));
            List<String> candidates = new ArrayList<>(all);
            while (game.status() == GuessingGame.Status.IN_PROGRESS) {
                String guess = candidates.get(0);
                Turn t = game.submit(guess);
                List<String> next = new ArrayList<>();
                for (String c : candidates) if (fb(c, guess).equals(t.getFeedback())) next.add(c);
                candidates = next;
            }
            if (game.status() == GuessingGame.Status.WON) {
                wins++;
                total += game.attemptsUsed();
                if (game.attemptsUsed() > worst) { worst = game.attemptsUsed(); worstSecret = secret; }
            }
        }
        System.out.println("  (measured: wins=" + wins + "/1296, worst=" + worst + " guesses on " + worstSecret
                + ", average=" + String.format("%.3f", total / (double) wins) + ")");
        check("consistent-guess solver wins all 1,296 games", wins == all.size());
        check("...and always within the 12-guess limit", worst <= 12);
    }
}
