package assignment2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Wordle's dictionary: the allowed guesses, which are also the words a secret
 * is drawn from. It is loaded from an external text file, so no words are
 * hard-coded in source.
 *
 * File format: one word per line, any case. Blank lines and lines starting
 * with '#' are ignored, and so is any entry that is not exactly wordLength
 * letters A-Z, so a general-purpose dictionary file can be used as-is.
 * Duplicates are ignored.
 *
 * WORDLE-SPECIFIC.
 */
public final class WordList {

    private final int wordLength;
    private final List<String> words;
    private final Set<String> lookup;

    /**
     * @param entries raw entries; each is put in canonical form and kept only if it is a valid word
     * @throws IllegalArgumentException if no entry is a valid word of that length
     */
    public WordList(int wordLength, Collection<String> entries) {
        if (wordLength < 1) {
            throw new IllegalArgumentException("wordLength must be at least 1");
        }
        Set<String> kept = new LinkedHashSet<>();
        for (String entry : entries) {
            if (entry == null) {
                continue;
            }
            String w = canonical(entry);
            if (!w.startsWith("#") && isWord(w, wordLength)) {
                kept.add(w);
            }
        }
        if (kept.isEmpty()) {
            throw new IllegalArgumentException("the word list has no " + wordLength
                    + "-letter words (each entry must be exactly " + wordLength + " letters A-Z)");
        }
        this.wordLength = wordLength;
        this.words = Collections.unmodifiableList(new ArrayList<>(kept));
        this.lookup = kept;
    }

    /** Reads a UTF-8 word file (a leading byte-order mark is ignored). */
    public static WordList load(Path file, int wordLength) throws IOException {
        List<String> lines = new ArrayList<>();
        try (BufferedReader r = new BufferedReader(
                new InputStreamReader(Files.newInputStream(file), StandardCharsets.UTF_8))) {
            for (String line = r.readLine(); line != null; line = r.readLine()) {
                lines.add(line);
            }
        }
        if (!lines.isEmpty() && lines.get(0).startsWith("\ufeff")) {
            lines.set(0, lines.get(0).substring(1));
        }
        return new WordList(wordLength, lines);
    }

    /**
     * Canonical form of a word or guess: trimmed, with a-z mapped to A-Z.
     * Every other character is left alone, so the length never changes
     * (unlike String.toUpperCase, which turns one German sharp s into "SS").
     */
    public static String canonical(String raw) {
        String t = raw.trim();
        StringBuilder sb = new StringBuilder(t.length());
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            sb.append(c >= 'a' && c <= 'z' ? (char) (c - 'a' + 'A') : c);
        }
        return sb.toString();
    }

    /** @return true if s is exactly length characters, each A-Z */
    static boolean isWord(String s, int length) {
        if (s.length() != length) {
            return false;
        }
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < 'A' || c > 'Z') {
                return false;
            }
        }
        return true;
    }

    /** @param word in canonical form */
    public boolean contains(String word) { return lookup.contains(word); }

    /** All words, upper case, in file order (unmodifiable). */
    public List<String> words()          { return words; }
    public int size()                    { return words.size(); }
    public int wordLength()              { return wordLength; }
}
