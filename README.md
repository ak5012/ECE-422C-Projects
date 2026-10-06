Play (both games from the same program):
  java -cp out assignment2.Driver mastermind
  java -cp out assignment2.Driver wordle
  java -cp out assignment2.Driver wordle test           (prints the secret)

Control the secret in test mode (comma-separated = used in order, then cycled;
every entry is checked against the game's rules at startup):
  java -Dwordle.secret=APPLE,CRANE -cp out assignment2.Driver wordle test
  java -Dmastermind.secret=BGOP,RRGG -cp out assignment2.Driver mastermind test
  (or set env var WORDLE_SECRET / MASTERMIND_SECRET)
  PowerShell: quote every -D option, or PowerShell splits it at the dot:
  java "-Dwordle.secret=APPLE,CRANE" -cp out assignment2.Driver wordle test

Wordle word list (external data, not in the source code):
  data/words.txt, one word per line; '#' lines and blank lines are ignored, and
  entries that are not exactly the configured length in letters A-Z are skipped.
  The file is looked for in the working directory and up to three parent folders.
  Use another file:  java -Dwordle.words=PATH -cp out assignment2.Driver wordle

Change configuration without touching code:
  java -Dwordle.guesses=8 -Dwordle.length=4 -Dwordle.words=four.txt -cp out assignment2.Driver wordle
  java -Dmastermind.guesses=8 -Dmastermind.pegs=6 -Dmastermind.colors=0123456789 -cp out assignment2.Driver mastermind

Tests (no external libraries; exit code 0 = all passed; run from this folder):
  javac -d out src/assignment2/*.java tests/assignment2/*.java
  java -cp out assignment2.AllTests            (both suites, 220 checks, about 10 s)
  java -cp out assignment2.WordleTests         (Wordle only)
  java -cp out assignment2.MastermindTests     (Mastermind, Phase I tests + Phase II regression)

Console conventions (shared by both games):
  Result: C P A P A      Wordle feedback: C = right spot, P = elsewhere in word, A = absent
  Result: nB_mW          Mastermind feedback
  INVALID_GUESS: ...     invalid guess (does not use an attempt)
  HISTORY                lists valid guesses so far (any case, any time)
  You win! / You lose!   end of round; then "Play another game? (Y/N):"

docs/source/ holds the HTML sources and script that rebuild the three PDFs
(cd docs/source && python build_pdfs.py). Requires Java 8 or newer.
