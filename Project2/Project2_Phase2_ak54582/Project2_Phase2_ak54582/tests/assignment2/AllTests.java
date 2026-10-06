package assignment2;

/**
 * Runs the Mastermind regression suite and the Wordle suite together. Run:
 *   java -cp out assignment2.AllTests
 * Exit code 0 = every check in both suites passed.
 */
public class AllTests {
    public static void main(String[] args) throws Exception {
        System.out.println("################ Mastermind (Phase I tests + Phase II regression) ################");
        MastermindTests.runAll();
        int mastermindPassed = TestSupport.passed(), mastermindFailed = TestSupport.failed();
        System.out.println("\n################ Wordle ################");
        WordleTests.runAll();
        System.out.println("\nMastermind: " + mastermindPassed + " passed, " + mastermindFailed + " failed");
        System.out.println("Wordle:     " + (TestSupport.passed() - mastermindPassed) + " passed, "
                + (TestSupport.failed() - mastermindFailed) + " failed");
        TestSupport.finish();
    }
}
