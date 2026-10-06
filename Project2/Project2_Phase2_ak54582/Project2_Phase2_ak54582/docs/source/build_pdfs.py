"""Builds ARCHITECTURE_CHANGE.pdf, TESTS.pdf and AI.pdf into the project root.

Usage (from docs/source):
    python build_diagrams.py      # only if the diagrams changed
    python build_pdfs.py

ARCHITECTURE_CHANGE.pdf is generated from ../../ARCHITECTURE_CHANGE.md (the .md is the
single source); TESTS.pdf and AI.pdf come from tests.html and ai.html in this folder.
Needs Python 3 and Microsoft Edge or Google Chrome (used headless to print to PDF).
"""
import argparse
import html
import os
import re
import shutil
import subprocess
import sys
import tempfile

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.normpath(os.path.join(HERE, "..", ".."))
BROWSERS = [
    r"C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe",
    r"C:\Program Files\Microsoft\Edge\Application\msedge.exe",
    r"C:\Program Files\Google\Chrome\Application\chrome.exe",
    r"C:\Program Files (x86)\Google\Chrome\Application\chrome.exe",
    "/usr/bin/google-chrome", "/usr/bin/chromium", "/usr/bin/chromium-browser",
    "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome",
    "/Applications/Microsoft Edge.app/Contents/MacOS/Microsoft Edge",
]


def read(path):
    with open(path, encoding="utf-8") as f:
        return f.read()


# ---------------------------------------------------------------- tiny Markdown subset
def inline(text):
    """Escapes HTML, then renders `code`, **bold** and *italic* (bold may contain code)."""
    codes = []

    def stash(m):
        codes.append("<code>" + html.escape(m.group(1)) + "</code>")
        return "\x00%d\x00" % (len(codes) - 1)

    e = html.escape(re.sub(r"`([^`]+)`", stash, text))
    e = re.sub(r"\*\*(.+?)\*\*", r"<b>\1</b>", e)
    e = re.sub(r"(?<![*\w])\*(?![\s*])(.+?)(?<![\s*])\*(?![*\w])", r"<i>\1</i>", e)
    return re.sub("\x00(\\d+)\x00", lambda m: codes[int(m.group(1))], e)


def md_to_html(md, base_dir):
    md = re.sub(r"<!-- pdf:skip-start -->.*?<!-- pdf:skip-end -->", "", md, flags=re.S)
    lines = md.split("\n")
    out, i, first_para_after_h1 = [], 0, False
    while i < len(lines):
        line = lines[i]
        if not line.strip():
            i += 1
            continue
        if line.startswith("```"):
            j = i + 1
            while j < len(lines) and not lines[j].startswith("```"):
                j += 1
            out.append("<pre><code>" + html.escape("\n".join(lines[i + 1:j])) + "</code></pre>")
            i = j + 1
            continue
        if line.strip() == "---":
            i += 1
            continue
        m = re.match(r"(#{1,3}) (.*)", line)
        if m:
            level = len(m.group(1))
            # "### heading" + image + caption paragraph -> one landscape page
            k = i + 1
            while k < len(lines) and not lines[k].strip():
                k += 1
            img = re.match(r"!\[(.*?)\]\((.*?)\)", lines[k]) if k < len(lines) else None
            if level == 3 and img:
                svg = read(os.path.join(base_dir, img.group(2)))
                c = k + 1
                while c < len(lines) and not lines[c].strip():
                    c += 1
                cap = []
                while c < len(lines) and lines[c].strip() and not lines[c].startswith(("#", "```", "!", "<!--")):
                    cap.append(lines[c])
                    c += 1
                out.append('<section class="wide"><h3>' + inline(m.group(2)) + "</h3><figure>" + svg
                           + "<figcaption>" + inline(" ".join(cap)) + "</figcaption></figure></section>")
                i = c
                continue
            out.append(f"<h{level}>{inline(m.group(2))}</h{level}>")
            first_para_after_h1 = level == 1
            i += 1
            continue
        if line.startswith("|"):
            rows = []
            while i < len(lines) and lines[i].startswith("|"):
                rows.append([c.strip() for c in lines[i].strip().strip("|").split("|")])
                i += 1
            head, body = rows[0], [r for r in rows[1:] if not re.match(r"^:?-+:?$", r[0])]
            t = ["<table><tr>" + "".join(f"<th>{inline(c)}</th>" for c in head) + "</tr>"]
            for r in body:
                t.append("<tr>" + "".join(f"<td>{inline(c)}</td>" for c in r) + "</tr>")
            out.append("".join(t) + "</table>")
            continue
        if re.match(r"(- |\d+\. )", line):
            ordered = bool(re.match(r"\d+\. ", line))
            tag = "ol" if ordered else "ul"
            items = []
            while i < len(lines) and re.match(r"(- |\d+\. )", lines[i]):
                items.append(re.sub(r"^(- |\d+\. )", "", lines[i]))
                i += 1
            out.append(f"<{tag}>" + "".join(f"<li>{inline(x)}</li>" for x in items) + f"</{tag}>")
            continue
        para = []
        while i < len(lines) and lines[i].strip() and not re.match(r"(#{1,3} |```|\||- |\d+\. |!\[)", lines[i]):
            para.append(lines[i])
            i += 1
        if para:
            cls = ' class="sub"' if first_para_after_h1 else ""
            first_para_after_h1 = False
            out.append(f"<p{cls}>" + inline(" ".join(para)) + "</p>")
        else:
            i += 1
    return "\n".join(out)


def page(title, css, body):
    return (f'<!doctype html><html><head><meta charset="utf-8"><title>{html.escape(title)}</title>'
            f"<style>{css}</style></head><body>{body}</body></html>")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default=ROOT)
    args = ap.parse_args()
    browser = next((b for b in BROWSERS if os.path.exists(b)), None) or shutil.which("msedge") or shutil.which("chrome")
    if not browser:
        sys.exit("No Edge/Chrome found; open the HTML in a browser and print it to PDF by hand.")

    css = read(os.path.join(HERE, "docs.css"))
    subs = {"{{DIAGRAM_BEFORE}}": read(os.path.join(HERE, "diagram-before.svg")),
            "{{DIAGRAM_AFTER}}": read(os.path.join(HERE, "diagram-after.svg"))}
    docs = [("ARCHITECTURE_CHANGE.pdf", "Architecture Change",
             md_to_html(read(os.path.join(ROOT, "ARCHITECTURE_CHANGE.md")), ROOT))]
    for src, pdf, title in [("tests.html", "TESTS.pdf", "Tests"), ("ai.html", "AI.pdf", "AI Usage")]:
        body = read(os.path.join(HERE, src))
        for k, v in subs.items():
            body = body.replace(k, v)
        docs.append((pdf, title, body))

    tmp = tempfile.mkdtemp(prefix="pdfbuild_")
    try:
        for pdf, title, body in docs:
            hp = os.path.join(tmp, pdf.replace(".pdf", ".html"))
            with open(hp, "w", encoding="utf-8") as f:
                f.write(page(title, css, body))
            out_pdf = os.path.abspath(os.path.join(args.out, pdf))
            if os.path.exists(out_pdf):
                os.remove(out_pdf)
            cmd = [browser, "--headless=new", "--disable-gpu", "--no-pdf-header-footer",
                   f"--print-to-pdf={out_pdf}", "file:///" + hp.replace("\\", "/")]
            subprocess.run(cmd, capture_output=True, timeout=180)
            print(("wrote " if os.path.exists(out_pdf) else "FAILED ") + out_pdf)
            if os.environ.get("KEEP_HTML"):
                shutil.copy(hp, os.path.join(os.environ["KEEP_HTML"], os.path.basename(hp)))
    finally:
        shutil.rmtree(tmp, ignore_errors=True)


if __name__ == "__main__":
    main()
