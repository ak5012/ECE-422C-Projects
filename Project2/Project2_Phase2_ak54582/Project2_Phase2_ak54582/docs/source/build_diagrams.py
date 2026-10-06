"""Generates diagram-before.svg (Phase I) and diagram-after.svg (Phase II).

Run from docs/source:  python build_diagrams.py
Writes the SVGs here and copies them to docs/ (referenced by ARCHITECTURE_CHANGE.md).
"""
import os
import shutil
from html import escape

HERE = os.path.dirname(os.path.abspath(__file__))
LH = 16.5  # member line height

BADGE_COLORS = {
    "unchanged": "#2e7d32", "modified": "#e65100", "new": "#6a1b9a",
    "renamed": "#1565c0", "replaced": "#b71c1c",
}


class Diagram:
    def __init__(self, w, h):
        self.w, self.h = w, h
        self.parts = []
        self.overlay = []

    def zone(self, x, y, w, h, cls, title, tcls):
        self.parts.append(f'<rect class="zone {cls}" x="{x}" y="{y}" width="{w}" height="{h}" rx="8"/>')
        self.parts.append(f'<text class="zt {tcls}" x="{x + 14}" y="{y + 19}">{escape(title)}</text>')

    def box(self, x, y, w, name, lines, kind, stereo=None, badge=None, badge_kind=None):
        """kind: r shared, m Mastermind, w Wordle, e entry point."""
        h = 38 + LH * len(lines) + 12
        p = [f'<g class="cls {kind}">', f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="4"/>']
        if stereo:
            p.append(f'<text class="st" x="{x + w / 2}" y="{y + 14}" text-anchor="middle">{escape(stereo)}</text>')
            p.append(f'<text class="nm" x="{x + w / 2}" y="{y + 30}" text-anchor="middle">{escape(name)}</text>')
        else:
            p.append(f'<text class="nm" x="{x + w / 2}" y="{y + 24}" text-anchor="middle">{escape(name)}</text>')
        p.append(f'<line class="sep" x1="{x}" y1="{y + 38}" x2="{x + w}" y2="{y + 38}"/>')
        for i, t in enumerate(lines):
            p.append(f'<text class="mb" x="{x + 8}" y="{y + 38 + LH * (i + 1) - 2}">{escape(t)}</text>')
        p.append('</g>')
        self.parts.append("\n".join(p))
        if badge:
            bw = len(badge) * 5.15 + 12
            bx = x + w - bw - 6
            col = BADGE_COLORS[badge_kind]
            self.overlay.append(
                f'<rect x="{bx:.1f}" y="{y - 13}" width="{bw:.1f}" height="15" rx="7.5" fill="{col}"/>'
                f'<text class="bd" x="{bx + bw / 2:.1f}" y="{y - 2}" text-anchor="middle">{escape(badge)}</text>')
        return h

    def line(self, x1, y1, x2, y2, end=None, start=None, dash=False):
        d = ' stroke-dasharray="5 4"' if dash else ""
        me = f' marker-end="url(#{end})"' if end else ""
        ms = f' marker-start="url(#{start})"' if start else ""
        self.parts.append(f'<line class="ln" x1="{x1}" y1="{y1}" x2="{x2}" y2="{y2}"{d}{me}{ms}/>')

    def poly(self, pts, end=None, dash=False):
        d = ' stroke-dasharray="5 4"' if dash else ""
        me = f' marker-end="url(#{end})"' if end else ""
        pp = " ".join(f"{a},{b}" for a, b in pts)
        self.parts.append(f'<polyline class="ln" points="{pp}"{d}{me}/>')

    def label(self, x, y, t, anchor="start", cls="lb"):
        self.parts.append(f'<text class="{cls}" x="{x}" y="{y}" text-anchor="{anchor}">{escape(t)}</text>')

    def callout(self, x, y, n):
        self.overlay.append(f'<circle cx="{x}" cy="{y}" r="9" fill="#b71c1c"/>'
                            f'<text class="co" x="{x}" y="{y + 4}" text-anchor="middle">{n}</text>')

    def legend(self, x, y, items_left, items_right_x):
        g = []
        for i, (stroke, fill, text) in enumerate(items_left):
            yy = y + 22 * i
            g.append(f'<rect x="{x}" y="{yy}" width="14" height="14" fill="{fill}" stroke="{stroke}" stroke-width="2"/>'
                     f'<text class="leg" x="{x + 20}" y="{yy + 12}">{escape(text)}</text>')
        rx = items_right_x
        g.append(f'<line x1="{rx}" y1="{y + 7}" x2="{rx + 40}" y2="{y + 7}" class="ln" marker-end="url(#tri)"/>'
                 f'<text class="leg" x="{rx + 48}" y="{y + 11}">extends</text>')
        g.append(f'<line x1="{rx}" y1="{y + 29}" x2="{rx + 40}" y2="{y + 29}" class="ln" stroke-dasharray="5 4" marker-end="url(#tri)"/>'
                 f'<text class="leg" x="{rx + 48}" y="{y + 33}">implements</text>')
        g.append(f'<line x1="{rx}" y1="{y + 51}" x2="{rx + 40}" y2="{y + 51}" class="ln" marker-end="url(#open)"/>'
                 f'<text class="leg" x="{rx + 48}" y="{y + 55}">uses / has-a</text>')
        g.append(f'<line x1="{rx + 140}" y1="{y + 7}" x2="{rx + 180}" y2="{y + 7}" class="ln" marker-start="url(#dia)"/>'
                 f'<text class="leg" x="{rx + 188}" y="{y + 11}">owns</text>')
        g.append(f'<line x1="{rx + 140}" y1="{y + 29}" x2="{rx + 180}" y2="{y + 29}" class="ln" stroke-dasharray="5 4" marker-end="url(#open)"/>'
                 f'<text class="leg" x="{rx + 188}" y="{y + 33}">creates</text>')
        self.overlay.append("".join(g))

    def svg(self):
        return f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {self.w} {self.h}" font-family="Segoe UI, Arial, Helvetica, sans-serif">
<defs>
<marker id="open" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="9" markerHeight="9" orient="auto-start-reverse"><path d="M1,1 L9,5 L1,9" fill="none" stroke="#37474f" stroke-width="1.4"/></marker>
<marker id="tri" viewBox="0 0 12 12" refX="11" refY="6" markerWidth="12" markerHeight="12" orient="auto-start-reverse"><path d="M1,1 L11,6 L1,11 Z" fill="#fff" stroke="#37474f" stroke-width="1.4"/></marker>
<marker id="dia" viewBox="0 0 14 10" refX="1" refY="5" markerWidth="14" markerHeight="10" orient="auto"><path d="M1,5 L7,1 L13,5 L7,9 Z" fill="#37474f" stroke="#37474f"/></marker>
</defs>
<style>
.zone{{fill-opacity:.55;stroke-width:1.4}}
.zr{{fill:#eef7ee;stroke:#2e7d32}} .zm{{fill:#eaf2fb;stroke:#1565c0}} .zw{{fill:#f5eefa;stroke:#6a1b9a}}
.zt{{font-size:12.5px;font-weight:700;letter-spacing:.2px}} .tr{{fill:#1b5e20}} .tm{{fill:#0d47a1}} .tw{{fill:#4a148c}}
.cls rect{{stroke-width:1.5}}
.cls.r rect{{fill:#fff;stroke:#2e7d32}} .cls.m rect{{fill:#fff;stroke:#1565c0}} .cls.w rect{{fill:#fff;stroke:#6a1b9a}}
.cls.e rect{{fill:#fff4e5;stroke:#ef6c00}}
.nm{{font-size:14px;font-weight:700;fill:#111}} .st{{font-size:10.5px;fill:#555;font-style:italic}}
.sep{{stroke:#9aa;stroke-width:1}}
.mb{{font-family:Consolas, "Courier New", monospace;font-size:11.2px;fill:#222;white-space:pre}}
.ln{{stroke:#37474f;stroke-width:1.4;fill:none}}
.lb{{font-size:10.5px;fill:#37474f;font-style:italic}}
.nt{{font-size:11.2px;fill:#1b5e20}} .ntr{{font-size:11.2px;fill:#7f1d1d}} .nth{{font-size:11.5px;fill:#7f1d1d;font-weight:700}}
.leg{{font-size:11px;fill:#222}}
.bd{{font-size:8.4px;font-weight:700;fill:#fff;letter-spacing:.3px}}
.co{{font-size:10.5px;font-weight:700;fill:#fff}}
</style>
<rect width="{self.w}" height="{self.h}" fill="#fff"/>
{chr(10).join(self.parts)}
{chr(10).join(self.overlay)}
</svg>'''


def before():
    d = Diagram(1000, 915)
    d.legend(20, 18, [("#2e7d32", "#fff", "reusable (Phase I claim)"), ("#1565c0", "#fff", "Mastermind-specific"),
                      ("#ef6c00", "#fff4e5", "entry point")], 690)
    d.zone(15, 130, 430, 770, "zr", "REUSABLE  —  game-independent", "tr")
    d.zone(480, 130, 505, 770, "zm", "", "tm")
    d.label(970, 149, "MASTERMIND-SPECIFIC  —  pegs, colors, secret code", "end", cls="zt tm")

    d.box(360, 15, 280, "Driver", ["main(args)  // mastermind [test]", "createGame(name, testMode)",
                                   "reads -Dmastermind.secret inline"], "e",
          badge="CHANGED", badge_kind="modified")
    d.box(35, 165, 395, "ConsoleRunner", [
        "- game: GuessingGame     - testMode: boolean",
        "- in: BufferedReader     - out: PrintStream",
        "+ run()   // play a round, ask 'play again?'",
        "prompt, INVALID_GUESS, HISTORY, win/lose text"], "r",
        badge="KEPT AS IS", badge_kind="unchanged")
    d.box(35, 330, 255, "GuessingGame", [
        "title(): String", "describeRules(): String", "startNewRound()", "status(): Status",
        "maxAttempts(): int", "attemptsUsed(): int", "attemptsRemaining(): int",
        "validate(String): String", "submit(String): Turn", "history(): List<Turn>", "revealSecret(): String"],
        "r", "«interface»", badge="KEPT AS IS", badge_kind="unchanged")
    d.box(310, 340, 120, "Turn", ["guess: String", "feedback: String", "solved: boolean"], "r", "«immutable»",
          badge="KEPT", badge_kind="unchanged")
    d.box(35, 605, 395, "AbstractGuessingGame", [
        "- maxAttempts: int      - status: Status",
        "- history: List<Turn>",
        "+ startNewRound()        «final»",
        "+ submit(String): Turn   «final»",
        "# onNewRound()           «hook»",
        "# evaluate(String): Turn «hook»"], "r", "«abstract»", badge="CHANGED", badge_kind="modified")

    d.box(500, 165, 215, "GameConfiguration", [
        "- maxGuesses: int", "- pegCount: int", "- colors: String",
        "+ defaults()", "+ fromSystemProperties()", "+ isLegalColor(char)"], "m", "«immutable»",
        badge="RENAMED → MastermindConfiguration", badge_kind="renamed")
    d.box(740, 165, 225, "CodeGenerator", ["nextCode(config): String"], "m", "«interface»",
          badge="REPLACED → SecretSource", badge_kind="replaced")
    d.box(740, 250, 225, "RandomCodeGenerator", ["uniform random color per peg", "+ nextCode(config)"], "m",
          badge="REPLACED → RandomCodeSource", badge_kind="replaced")
    d.box(740, 350, 225, "FixedCodeGenerator", ["fixed secrets (test mode)", "checks pegs/colors vs config"], "m",
          badge="REPLACED → FixedSecretSource", badge_kind="replaced")
    d.box(500, 570, 465, "MastermindGame", [
        "- config: GameConfiguration   - generator: CodeGenerator",
        "- secret: String",
        "# onNewRound()     secret = generator.nextCode(config)",
        "+ validate(input)  length + legal colors, case-tolerant",
        "# evaluate(input)  MastermindScorer -> Feedback -> Turn",
        "+ title() / describeRules() / revealSecret()"], "m", badge="CHANGED", badge_kind="modified")
    d.box(500, 770, 215, "MastermindScorer", ["static score(secret, guess)", "exact matches first, then",
                                              "min(count) per color"], "m", "«utility»",
          badge="CHANGED", badge_kind="modified")
    d.box(740, 770, 225, "Feedback", ["- black: int   - white: int", "toString() -> \"nB_mW\"", "equals / hashCode"],
          "m", "«immutable»", badge="RENAMED → MastermindFeedback", badge_kind="renamed")

    # relationships
    d.line(150, 281, 150, 330, end="open")
    d.label(158, 306, "uses (only this interface)")
    d.line(290, 385, 310, 385, end="open", dash=True)
    d.line(150, 605, 150, 561.5, end="tri", dash=True)
    d.line(372, 605, 372, 439.5, start="dia")
    d.label(380, 565, "history")
    d.label(380, 579, "0..*")
    d.line(500, 647, 430, 647, end="tri")
    d.label(437, 639, "extends")
    d.line(600, 570, 600, 314, end="open")
    d.label(608, 495, "config")
    d.poly([(965, 615), (976, 615), (976, 197), (965, 197)], end="open")
    d.label(970, 505, "generator", "end")
    d.poly([(740, 290), (728, 290), (728, 197), (740, 197)], end="tri", dash=True)
    d.poly([(740, 390), (728, 390), (728, 290)], dash=True)
    d.line(607, 719, 607, 770, end="open")
    d.label(615, 760, "uses")
    d.line(715, 817, 740, 817, end="open")
    d.line(380, 114.5, 300, 165, end="open", dash=True)
    d.line(505, 114.5, 505, 165, end="open", dash=True)
    d.label(330, 124, "creates + runs", "end")
    d.label(512, 125, "creates + wires")

    # what blocked Wordle
    d.callout(733, 175, 1)
    d.callout(493, 780, 2)
    d.callout(493, 580, 3)
    d.callout(493, 175, 4)
    d.callout(733, 780, 4)
    d.callout(353, 25, 5)
    notes = [
        ("nth", "What turned out to be too Mastermind-specific:"),
        ("ntr", "1  CodeGenerator.nextCode() takes pegs/colors (GameConfiguration)."),
        ("ntr", "2  The repeated-symbol rule is locked inside black/white counting."),
        ("ntr", "3  Each game must store, draw and reveal its own secret."),
        ("ntr", "4  Generic names on Mastermind-only types."),
        ("ntr", "5  Test-mode secrets exist only for Mastermind, read inline."),
    ]
    for i, (cls, t) in enumerate(notes):
        d.label(35, 790 + 17 * i, t, cls=cls)
    return d.svg()


def after():
    d = Diagram(1400, 1095)
    d.legend(20, 14, [("#2e7d32", "#fff", "shared by both games"), ("#1565c0", "#fff", "Mastermind-specific"),
                      ("#6a1b9a", "#fff", "Wordle-specific"), ("#ef6c00", "#fff4e5", "entry point")], 1070)

    d.box(520, 16, 360, "Driver", [
        "main(args)   // mastermind|wordle [test]",
        "createGame(name, testMode)",
        "createMastermind() / createWordle()",
        "forcedSecrets(game)  -D<game>.secret",
        "findFile() / loadWordList()"], "e", badge="MODIFIED", badge_kind="modified")

    # ---- shared band ----
    d.zone(15, 170, 1370, 505, "zr", "SHARED  —  used unchanged by Mastermind and Wordle", "tr")
    d.box(35, 205, 350, "ConsoleRunner", [
        "- game: GuessingGame     - testMode: boolean",
        "- in: BufferedReader     - out: PrintStream",
        "+ run()   // round, 'play again?', repeat",
        "prompt, INVALID_GUESS, HISTORY, win/lose"], "r", badge="UNCHANGED", badge_kind="unchanged")
    d.box(430, 205, 255, "GuessingGame", [
        "title(): String", "describeRules(): String", "startNewRound()", "status(): Status",
        "maxAttempts(): int", "attemptsUsed(): int", "attemptsRemaining(): int",
        "validate(String): String", "submit(String): Turn", "history(): List<Turn>", "revealSecret(): String"],
        "r", "«interface»", badge="UNCHANGED", badge_kind="unchanged")
    d.box(730, 215, 130, "Turn", ["guess: String", "feedback: String", "solved: boolean"], "r", "«immutable»",
          badge="UNCHANGED", badge_kind="unchanged")
    d.box(430, 475, 430, "AbstractGuessingGame", [
        "- maxAttempts  - history: List<Turn>  - status",
        "- secrets: SecretSource   - secret: String",
        "+ startNewRound() «final»  draws + checks secret",
        "+ validate(in)    «final»  + submit(in) «final»",
        "+ revealSecret()  «final»",
        "# normalize(raw)          «hook»",
        "# checkGuess(guess)       «hook»",
        "# evaluate(secret, guess) «hook»"], "r", "«abstract»", badge="MODIFIED", badge_kind="modified")
    d.box(905, 475, 200, "SecretSource", ["nextSecret(): String"], "r", "«interface»",
          badge="REPLACES CodeGenerator", badge_kind="new")
    d.box(1150, 475, 215, "FixedSecretSource", ["fixed list, used in order", "and cycled (test mode)"], "r",
          badge="REPLACES FixedCodeGenerator", badge_kind="new")
    d.label(35, 365, "Kept byte-for-byte from Phase I:", cls="nt")
    d.label(35, 382, "ConsoleRunner, GuessingGame (+ Status), Turn.", cls="nt")
    d.label(35, 408, "AbstractGuessingGame now also owns the secret:", cls="nt")
    d.label(35, 425, "it draws it from a SecretSource and rejects one", cls="nt")
    d.label(35, 442, "that is not itself a legal guess.", cls="nt")
    d.label(35, 468, "A game supplies only normalize, checkGuess,", cls="nt")
    d.label(35, 485, "evaluate, title and describeRules.", cls="nt")

    d.line(385, 262, 430, 262, end="open")
    d.label(390, 254, "uses")
    d.line(685, 262, 730, 262, end="open", dash=True)
    d.line(520, 475, 520, 436.5, end="tri", dash=True)
    d.line(795, 475, 795, 314.5, start="dia")
    d.label(803, 455, "history 0..*")
    d.line(860, 505, 905, 505, end="open")
    d.label(864, 497, "secrets")
    d.line(1150, 505, 1105, 505, end="tri", dash=True)
    d.line(540, 148.5, 360, 205, end="open", dash=True)
    d.label(470, 166, "creates + runs", "end")
    d.line(860, 148.5, 1255, 475, end="open", dash=True)
    d.label(1090, 300, "creates (test mode)", "middle")

    # generalization tree: both games extend AbstractGuessingGame
    d.poly([(645, 690), (645, 657)], end="tri")
    d.poly([(200, 740), (200, 690), (1220, 690), (1220, 740)])
    d.label(655, 684, "both games extend AbstractGuessingGame")

    # ---- Mastermind band ----
    d.zone(15, 705, 550, 375, "zm", "MASTERMIND", "tm")
    d.box(35, 740, 325, "MastermindGame", [
        "- config: MastermindConfiguration",
        "# normalize   case-tolerant colors",
        "# checkGuess  length + legal colors",
        "# evaluate    MastermindScorer -> Turn",
        "+ title() / describeRules()"], "m", badge="MODIFIED", badge_kind="modified")
    d.box(380, 740, 170, "MastermindScorer", ["+ score(secret, guess)", "black = # CORRECT", "white = # PRESENT"],
          "m", "«utility»", badge="MODIFIED", badge_kind="modified")
    d.box(30, 915, 196, "MastermindConfiguration", [
        "- maxGuesses: int", "- pegCount: int", "- colors: String",
        "+ defaults()", "+ fromSystemProperties()", "+ isLegalColor(char)"], "m", "«immutable»",
        badge="RENAMED", badge_kind="renamed")
    d.box(236, 915, 150, "RandomCodeSource", ["- config, - random", "+ nextSecret()"], "m", "«SecretSource»",
          badge="REPLACES RandomCodeGenerator", badge_kind="new")
    d.box(396, 915, 158, "MastermindFeedback", ["- black, - white", "toString(): nB_mW", "equals / hashCode"],
          "m", "«immutable»", badge="RENAMED", badge_kind="renamed")
    d.line(360, 790, 380, 790, end="open")
    d.line(470, 839.5, 470, 915, end="open")
    d.label(476, 885, "returns")
    d.line(130, 872.5, 130, 915, end="open")
    d.label(136, 900, "config")
    d.line(236, 960, 226, 960, end="open")

    # ---- shared rule column ----
    d.zone(580, 705, 245, 375, "zr", "SHARED RULE", "tr")
    d.box(597, 740, 212, "PositionMatcher", [
        "+ match(secret, guess)", "    : List<PositionMatch>", "1. CORRECT where equal",
        "2. PRESENT left to right"], "r", "«utility»", badge="NEW (extracted)", badge_kind="new")
    d.box(597, 915, 212, "PositionMatch", ["CORRECT | PRESENT | ABSENT"], "r", "«enum»", badge="NEW", badge_kind="new")
    d.line(703, 856, 703, 915, end="open")
    d.label(709, 890, "returns")
    d.line(550, 790, 597, 790, end="open")
    d.label(556, 782, "uses")
    d.label(597, 1010, "One implementation of the repeated-", cls="nt")
    d.label(597, 1027, "symbol rule, used by both games.", cls="nt")

    # ---- Wordle band ----
    d.zone(840, 705, 545, 375, "zw", "WORDLE", "tw")
    d.box(855, 740, 200, "WordleFeedback", [
        "- marks: List<PositionMatch>", "+ score(secret, guess)", "+ isSolved()", "toString(): \"C P A P A\""],
        "w", "«immutable»", badge="NEW", badge_kind="new")
    d.box(1075, 740, 295, "WordleGame", [
        "- config: WordleConfiguration",
        "- words: WordList",
        "# normalize   trim, a-z -> A-Z",
        "# checkGuess  length, A-Z, in word list",
        "# evaluate    WordleFeedback -> Turn",
        "+ title() / describeRules()"], "w", badge="NEW", badge_kind="new")
    d.box(852, 925, 168, "WordleConfiguration", [
        "- maxGuesses = 6", "- wordLength = 5", "+ defaults()", "+ fromSystemProperties()"], "w", "«immutable»",
        badge="NEW", badge_kind="new")
    d.box(1032, 925, 200, "WordList", [
        "reads data/words.txt", "+ load(Path, length)", "+ canonical(raw)", "+ contains(word)",
        "+ words(): List<String>"], "w", badge="NEW", badge_kind="new")
    d.box(1242, 925, 138, "RandomWordSource", ["- words", "+ nextSecret()"], "w", "«SecretSource»",
          badge="NEW", badge_kind="new")
    d.line(855, 790, 809, 790, end="open")
    d.label(815, 782, "uses")
    d.line(1075, 790, 1055, 790, end="open")
    d.poly([(1110, 889), (1110, 905), (940, 905), (940, 925)], end="open")
    d.label(946, 918, "config")
    d.line(1160, 889, 1160, 925, end="open")
    d.label(1166, 912, "words")
    d.line(1242, 965, 1232, 965, end="open")
    return d.svg()


if __name__ == "__main__":
    docs = os.path.normpath(os.path.join(HERE, ".."))
    for name, fn in [("diagram-before.svg", before), ("diagram-after.svg", after)]:
        path = os.path.join(HERE, name)
        with open(path, "w", encoding="utf-8") as f:
            f.write(fn())
        shutil.copy(path, os.path.join(docs, name))
        print("wrote", path)
