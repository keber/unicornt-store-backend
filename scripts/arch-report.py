#!/usr/bin/env python3
"""Render the ArchUnit rules from the Surefire XML as a standalone HTML page.

The architecture rules are the repo's headline constraint — the dependency rule,
enforced hard with no freeze baseline — but they are 16 rows buried in a
300-plus-test Surefire report, which is a poor thing to link a badge at. This
reads the three architecture test reports and writes a page that shows only
them, one row per rule, with the pass/fail the build actually recorded.

Usage:
    python scripts/arch-report.py [surefire-dir] [output-dir]

Defaults: target/surefire-reports -> target/site/architecture

Exit codes: 0 written, 1 no architecture reports found (a build that never ran
them, which should fail the CI step rather than publish an empty page).
"""

from __future__ import annotations

import glob
import html
import os
import sys
import xml.etree.ElementTree as ET
from datetime import datetime, timezone

PREFIX = "TEST-com.unicornt.store.architecture."

SUITE_BLURBS = {
    "DependencyRulesTest": "Nothing may point inward at Spring, JPA or the web layer, "
                           "and no package may take part in a cycle.",
    "LayeredArchitectureRulesTest": "The layers themselves: who is allowed to call whom.",
    "TargetArchitectureRulesTest": "The shape of the ports-and-adapters wiring — where "
                                   "repository interfaces live and what adapters implement.",
}


def parse(path):
    """Return (suite_name, [(rule, status, seconds, detail), ...]) for one report."""
    root = ET.parse(path).getroot()
    suite = root.get("name", "").rsplit(".", 1)[-1]
    rules = []
    for case in root.findall("testcase"):
        failure = case.find("failure")
        error = case.find("error")
        skipped = case.find("skipped")
        if failure is not None or error is not None:
            node = failure if failure is not None else error
            status, detail = "failed", (node.get("message") or node.text or "").strip()
        elif skipped is not None:
            status, detail = "skipped", ""
        else:
            status, detail = "passed", ""
        rules.append((case.get("name", ""), status, float(case.get("time", 0) or 0), detail))
    return suite, rules


def render(suites, commit, run_url):
    total = sum(len(r) for _, r in suites)
    failed = sum(1 for _, rules in suites for r in rules if r[1] == "failed")
    generated = datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M UTC")
    verdict = "all rules green" if not failed else f"{failed} of {total} rules violated"
    tone = "ok" if not failed else "bad"

    rows = []
    for suite, rules in suites:
        blurb = SUITE_BLURBS.get(suite, "")
        rows.append(f"<h2>{html.escape(suite)}</h2>")
        if blurb:
            rows.append(f"<p class='blurb'>{html.escape(blurb)}</p>")
        rows.append("<table><thead><tr><th>Rule</th><th>Status</th><th>Time</th></tr></thead><tbody>")
        for name, status, seconds, detail in rules:
            pretty = html.escape(name.replace("_", " "))
            cls = {"passed": "ok", "failed": "bad", "skipped": "meh"}[status]
            mark = {"passed": "&#10003; passed", "failed": "&#10007; failed", "skipped": "skipped"}[status]
            rows.append(
                f"<tr><td><code>{html.escape(name)}</code><div class='pretty'>{pretty}</div></td>"
                f"<td class='{cls}'>{mark}</td><td class='num'>{seconds:.3f}s</td></tr>"
            )
            if detail:
                rows.append(f"<tr class='detail'><td colspan='3'><pre>{html.escape(detail)}</pre></td></tr>")
        rows.append("</tbody></table>")

    source = ""
    if commit:
        short = html.escape(commit[:7])
        source = f" · commit <code>{short}</code>"
    if run_url:
        source += f" · <a href='{html.escape(run_url)}'>CI run</a>"

    return f"""<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Architecture rules — unicornt-store-backend</title>
<style>
  :root {{ color-scheme: light dark; --fg:#1a1a1a; --bg:#fff; --muted:#666; --line:#e2e2e2;
           --ok:#1a7f37; --bad:#c1121f; --meh:#8a6d00; --code:#f6f6f6; }}
  @media (prefers-color-scheme: dark) {{
    :root {{ --fg:#e8e8e8; --bg:#101214; --muted:#9aa0a6; --line:#2c2f33;
             --ok:#4ac26b; --bad:#ff6b6b; --meh:#d9b100; --code:#1a1d20; }}
  }}
  body {{ margin:0 auto; padding:2rem 1.25rem 4rem; max-width:60rem; background:var(--bg); color:var(--fg);
         font:15px/1.55 -apple-system,BlinkMacSystemFont,"Segoe UI",Roboto,sans-serif; }}
  h1 {{ font-size:1.6rem; margin:0 0 .35rem; }}
  h2 {{ font-size:1.05rem; margin:2.25rem 0 .25rem; }}
  .lede, .blurb, .meta {{ color:var(--muted); }}
  .lede {{ margin:0 0 1.5rem; }}
  .blurb {{ margin:0 0 .75rem; font-size:.92rem; }}
  .meta {{ font-size:.85rem; margin-top:3rem; border-top:1px solid var(--line); padding-top:1rem; }}
  .verdict {{ display:inline-block; padding:.3rem .7rem; border-radius:999px; font-weight:600;
              background:var(--code); }}
  .verdict.ok {{ color:var(--ok); }} .verdict.bad {{ color:var(--bad); }}
  table {{ border-collapse:collapse; width:100%; }}
  th, td {{ text-align:left; padding:.5rem .6rem; border-bottom:1px solid var(--line); vertical-align:top; }}
  th {{ font-size:.78rem; text-transform:uppercase; letter-spacing:.04em; color:var(--muted); }}
  code {{ background:var(--code); padding:.1rem .3rem; border-radius:4px; font-size:.86rem; }}
  .pretty {{ color:var(--muted); font-size:.85rem; margin-top:.2rem; }}
  .ok {{ color:var(--ok); }} .bad {{ color:var(--bad); font-weight:600; }} .meh {{ color:var(--meh); }}
  .num {{ color:var(--muted); white-space:nowrap; }}
  .detail pre {{ background:var(--code); padding:.75rem; border-radius:6px; overflow-x:auto; font-size:.8rem; }}
  a {{ color:inherit; }}
</style>
</head>
<body>
<h1>Architecture rules</h1>
<p class="lede">
  ArchUnit enforces the dependency rule in <code>unicornt-store-backend</code>: the domain depends on
  nobody, the application layer depends only on domain ports, and infrastructure may depend on both.
  There is <strong>no freeze baseline</strong> — a new violation fails the build rather than being
  recorded as accepted. These {total} rules run inside <code>mvn verify</code> with the rest of the suite.
</p>
<p><span class="verdict {tone}">{verdict}</span></p>
{os.linesep.join(rows)}
<p class="meta">
  Generated {generated}{source} ·
  <a href="../coverage/">coverage report</a> ·
  <a href="../tests/">full test report</a>
</p>
</body>
</html>
"""


def main():
    surefire = sys.argv[1] if len(sys.argv) > 1 else "target/surefire-reports"
    outdir = sys.argv[2] if len(sys.argv) > 2 else "target/site/architecture"

    reports = sorted(glob.glob(os.path.join(surefire, PREFIX + "*.xml")))
    if not reports:
        print(f"::error::no architecture reports matching {PREFIX}*.xml in {surefire}", file=sys.stderr)
        return 1

    suites = [parse(p) for p in reports]
    page = render(
        suites,
        os.environ.get("GITHUB_SHA", ""),
        f"{os.environ['GITHUB_SERVER_URL']}/{os.environ['GITHUB_REPOSITORY']}/actions/runs/{os.environ['GITHUB_RUN_ID']}"
        if os.environ.get("GITHUB_RUN_ID") else "",
    )

    os.makedirs(outdir, exist_ok=True)
    with open(os.path.join(outdir, "index.html"), "w", encoding="utf-8") as fh:
        fh.write(page)

    total = sum(len(r) for _, r in suites)
    print(f"wrote {outdir}/index.html — {total} rules from {len(suites)} suites")
    return 0


if __name__ == "__main__":
    sys.exit(main())
