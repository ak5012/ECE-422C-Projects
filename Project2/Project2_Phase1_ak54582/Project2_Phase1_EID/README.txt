Build (from this folder):
  mkdir -p out && javac -d out src/assignment2/*.java

Play:
  java -cp out assignment2.Driver mastermind
  java -cp out assignment2.Driver mastermind test        (prints the secret)

Control the secret in test mode (comma-separated = used in order):
  java -Dmastermind.secret=BGOP,RRGG -cp out assignment2.Driver mastermind test
  (or set env var MASTERMIND_SECRET)

Change configuration without touching code:
  java -Dmastermind.guesses=8 -Dmastermind.pegs=6 -Dmastermind.colors=0123456789 -cp out assignment2.Driver mastermind

Tests + stress tests (no external libraries):
  javac -d out src/assignment2/*.java tests/assignment2/*.java
  java -cp out assignment2.MastermindTests
