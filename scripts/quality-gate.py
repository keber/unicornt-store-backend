#!/usr/bin/env python3
"""Fail the build when the SonarCloud quality gate is not green.

This exists because -Dsonar.qualitygate.wait=true cannot work on this project:
SonarCloud answers any non-main gate read with HTTP 403 "Organization is not
allowed to access data from non main branches", and the scanner reports that as
"Not authorized or project not found" — a message about credentials for a
problem that is not about credentials. The unscoped read, which is main's,
works, so the gate is asserted here and only on main.

The token is read from the environment and used only in a request header: never
a command-line argument (visible in the process list) and never echoed to
stdout, which is the exposure the newer scanner flags in workflow files.

Usage:
    SONAR_TOKEN=... python3 scripts/quality-gate.py <project-key>

Exit codes: 0 gate OK, 1 gate not OK or no analysis to read.
"""

from __future__ import annotations

import base64
import json
import os
import sys
import time
import urllib.error
import urllib.request

BASE = "https://sonarcloud.io/api"
POLL_SECONDS = 5
POLL_ATTEMPTS = 60


def api(path, token):
    """GET one API path, returning parsed JSON. Errors carry the body, not the token."""
    request = urllib.request.Request(f"{BASE}/{path}")
    credentials = base64.b64encode(f"{token}:".encode()).decode()
    request.add_header("Authorization", f"Basic {credentials}")
    try:
        with urllib.request.urlopen(request, timeout=30) as response:
            return json.load(response)
    except urllib.error.HTTPError as err:
        body = err.read().decode("utf-8", "replace")[:400]
        raise SystemExit(f"::error::GET /{path.split('?')[0]} -> HTTP {err.code}: {body}")


def wait_for_analysis(project, token):
    """Block until the analysis this job submitted leaves the compute queue."""
    for _ in range(POLL_ATTEMPTS):
        state = api(f"ce/component?component={project}", token)
        queued = len(state.get("queue") or [])
        if not queued:
            return (state.get("current") or {}).get("analysisId", "")
        print(f"  analysis still queued ({queued}), waiting...", flush=True)
        time.sleep(POLL_SECONDS)
    raise SystemExit("::error::SonarCloud analysis did not finish within 5 minutes")


def main():
    if len(sys.argv) != 2:
        raise SystemExit("usage: quality-gate.py <project-key>")
    project = sys.argv[1]

    token = os.environ.get("SONAR_TOKEN")
    if not token:
        raise SystemExit("::error::SONAR_TOKEN is not set")

    analysis = wait_for_analysis(project, token)
    gate = api(f"qualitygates/project_status?projectKey={project}", token)
    status = gate.get("projectStatus", {}).get("status", "UNKNOWN")
    print(f"quality gate: {status} (analysis {analysis or 'unknown'})")

    if status == "OK":
        return 0

    for condition in gate.get("projectStatus", {}).get("conditions", []):
        if condition.get("status") != "OK":
            print(
                f"  {condition.get('metricKey')}: {condition.get('actualValue')} "
                f"(threshold {condition.get('errorThreshold')})"
            )
    print(f"::error::SonarCloud quality gate is {status}")
    return 1


if __name__ == "__main__":
    sys.exit(main())
