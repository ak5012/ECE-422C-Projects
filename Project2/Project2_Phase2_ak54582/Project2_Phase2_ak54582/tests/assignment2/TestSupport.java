package assignment2;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Tiny dependency-free harness shared by MastermindTests and WordleTests:
 * pass/fail counting, scripted console sessions for any GuessingGame, and
 * launching the real Driver in a child JVM.
 */
final class TestSupport {

    private static int passed = 0;
    private static int failed = 0;

    private TestSupport() { }

    static void check(String name, boolean ok) {
        if (ok) {
            passed++;
            System.out.println("  PASS  " + name);
        } else {
            failed++;
            System.out.println("  FAIL  " + name);
        }
    }

    static void section(String s) {
        System.out.println("\n== " + s + " ==");
    }

    static int count(String haystack, String needle) {
        int n = 0, i = 0;
        while ((i = haystack.indexOf(needle, i)) >= 0) { n++; i += needle.length(); }
        return n;
    }

    /** Every string of the given length over the given alphabet, in lexicographic order. */
    static List<String> allCodes(String alphabet, int length) {
        List<String> out = new ArrayList<>();
        build("", alphabet, length, out);
        return out;
    }

    private static void build(String prefix, String alphabet, int left, List<String> out) {
        if (left == 0) { out.add(prefix); return; }
        for (int i = 0; i < alphabet.length(); i++) build(prefix + alphabet.charAt(i), alphabet, left - 1, out);
    }

    /** Plays a scripted console session of any game and returns everything it printed. */
    static String session(GuessingGame game, boolean testMode, String input) throws Exception {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(buf, true, "UTF-8");
        new ConsoleRunner(game, new BufferedReader(new StringReader(input)), out, testMode).run();
        return buf.toString("UTF-8");
    }

    /**
     * Runs "java assignment2.Driver args..." in a child JVM on the same class path.
     * MASTERMIND_SECRET and WORDLE_SECRET are cleared, then env (name, value, ...) is applied.
     * A child still running after 60 seconds is killed and reported with exit code "timeout",
     * so a hung launcher fails its check instead of hanging the whole suite.
     * @param dir working directory for the child, or null for the current one
     * @return {exit code, stdout, stderr}
     */
    static String[] launch(String[] args, String stdin, String[] jvmProps, File dir, String... env)
            throws Exception {
        List<String> cmd = new ArrayList<>();
        cmd.add(System.getProperty("java.home") + File.separator + "bin" + File.separator + "java");
        if (jvmProps != null) cmd.addAll(Arrays.asList(jvmProps));
        cmd.add("-cp");
        cmd.add(absoluteClassPath());
        cmd.add("assignment2.Driver");
        cmd.addAll(Arrays.asList(args));
        ProcessBuilder pb = new ProcessBuilder(cmd);
        if (dir != null) pb.directory(dir);
        Map<String, String> e = pb.environment();
        e.remove("MASTERMIND_SECRET");
        e.remove("WORDLE_SECRET");
        for (int i = 0; i + 1 < env.length; i += 2) e.put(env[i], env[i + 1]);

        Process p = pb.start();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        Thread outReader = drain(p.getInputStream(), out);
        Thread errReader = drain(p.getErrorStream(), err);
        p.getOutputStream().write(stdin.getBytes("UTF-8"));
        p.getOutputStream().close();
        String rc;
        if (p.waitFor(60, TimeUnit.SECONDS)) {
            rc = String.valueOf(p.exitValue());
        } else {
            p.destroyForcibly();
            rc = "timeout";
        }
        outReader.join(5000);
        errReader.join(5000);
        synchronized (out) {
            synchronized (err) {
                return new String[] { rc, out.toString("UTF-8"), err.toString("UTF-8") };
            }
        }
    }

    /** Copies a child's output stream into a buffer on a background thread. */
    private static Thread drain(final InputStream in, final ByteArrayOutputStream to) {
        Thread t = new Thread(new Runnable() {
            @Override public void run() {
                try { copy(in, to); } catch (IOException ignored) { }
            }
        });
        t.setDaemon(true);
        t.start();
        return t;
    }

    private static void copy(InputStream in, ByteArrayOutputStream to) throws IOException {
        byte[] buf = new byte[8192];
        for (int n; (n = in.read(buf)) > 0; ) {
            synchronized (to) { to.write(buf, 0, n); }
        }
    }

    /** The test JVM's class path with every entry made absolute, so a child can run in another directory. */
    private static String absoluteClassPath() {
        StringBuilder sb = new StringBuilder();
        for (String entry : System.getProperty("java.class.path").split(File.pathSeparator)) {
            if (sb.length() > 0) sb.append(File.pathSeparator);
            sb.append(new File(entry).getAbsolutePath());
        }
        return sb.toString();
    }

    /** Sets (or, for null, clears) a system property and returns its previous value. */
    static String setProperty(String key, String value) {
        String old = System.getProperty(key);
        if (value == null) System.clearProperty(key); else System.setProperty(key, value);
        return old;
    }

    static int passed() { return passed; }
    static int failed() { return failed; }

    /** Prints the totals and exits with status 0 only if every check passed. */
    static void finish() {
        System.out.println("\n----------------------------------------");
        System.out.println("passed: " + passed + "   failed: " + failed);
        System.exit(failed == 0 ? 0 : 1);
    }
}
