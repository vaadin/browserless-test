#!/usr/bin/env python3
"""Extract Javadoc problems from a Maven log and emit a JUnit XML report.

Usage: javadocErrorsToJUnit.py <maven-log> <output-xml>

maven-javadoc-plugin prints every problem of a failed run as a block of
header + source line + caret. Each line is prefixed by Maven's [ERROR]
marker, or by [WARNING] when the run had only warnings. The header says
"error:" or "warning:"; with failOnWarnings both fail the build.

The JUnit XML file has one testcase per problem, so the
publish-unit-test-result-action in the test-results job surfaces them in
the PR comment and job summary alongside surefire failures. Each problem
is also emitted as a GitHub Actions workflow command (`::error ...`) so it
shows up inline in the failing job's log and as an annotation on the PR
diff. Writes nothing if the log has no Javadoc problems.
"""

import os
import re
import sys
from xml.sax.saxutils import escape, quoteattr

JAVADOC_PATTERN = re.compile(
    r'^\[(ERROR|WARNING)\] (\S+\.java):(\d+): (?:error|warning): (.*)$\n'
    r'^\[\1\] (.*)$\n'
    r'^\[\1\] (.*\^.*)$',
    re.M,
)

JAVADOC_START = re.compile(
    r'^\[INFO\] --- javadoc:\S+:jar .*@ (\S+) ---$', re.M)
JAVADOC_FAILED = 'maven-javadoc-plugin:\\S+ .*on project {}: '


def relativize(path, workspace):
    if workspace and path.startswith(workspace + '/'):
        return path[len(workspace) + 1:]
    return path


def annotation_escape(value):
    """Escape a value for use in a GitHub Actions workflow command message."""
    return value.replace('%', '%25').replace('\r', '%0D').replace('\n', '%0A')


def annotation_property_escape(value):
    """Escape a value for use as a GitHub Actions workflow command property."""
    return annotation_escape(value).replace(':', '%3A').replace(',', '%2C')


def emit_annotation(file_rel, line_no, detail):
    location = []
    if file_rel:
        location.append(f'file={annotation_property_escape(file_rel)}')
    if line_no:
        location.append(f'line={line_no}')
    location.append('title=Javadoc')
    print(f'::error {",".join(location)}::{annotation_escape(detail)}')


def javadoc_problems(log, workspace):
    for start in JAVADOC_START.finditer(log):
        # Only a failed run is a problem; a passing run can still print
        # warnings when failOnWarnings is off. Maven stops at the failed
        # goal and prints its output after the build result, so the rest of
        # the log belongs to that run.
        rest = log[start.end():]
        failed = JAVADOC_FAILED.format(re.escape(start.group(1)))
        if not re.search(failed, rest):
            continue
        for _, path, line_no, msg, src, caret in JAVADOC_PATTERN.findall(rest):
            file_rel = relativize(path, workspace)
            yield (f'{file_rel}:{line_no}', file_rel, line_no, msg,
                   f'{file_rel}:{line_no}: {msg}\n{src}\n{caret}')


def main(log_path, out_path):
    with open(log_path, encoding='utf-8', errors='replace') as f:
        log = f.read()
    workspace = os.environ.get('GITHUB_WORKSPACE', '')

    problems = list(javadoc_problems(log, workspace))
    # javadoc can print the same problem more than once.
    problems = list(dict.fromkeys(problems))
    if not problems:
        print(f'No Javadoc problems found in {log_path}', file=sys.stderr)
        return 0

    parts = ['<?xml version="1.0" encoding="UTF-8"?>']
    n = len(problems)
    parts.append(
        f'<testsuite name="Javadoc" tests="{n}" failures="{n}" '
        f'errors="0" skipped="0">'
    )
    for name, file_rel, line_no, msg, detail in problems:
        emit_annotation(file_rel, line_no, detail)
        parts.append(
            f'  <testcase classname={quoteattr("Javadoc")} '
            f'name={quoteattr(name)}>'
        )
        parts.append(
            f'    <failure message={quoteattr(msg)}>'
            f'{escape(detail)}</failure>'
        )
        parts.append('  </testcase>')
    parts.append('</testsuite>')

    os.makedirs(os.path.dirname(out_path) or '.', exist_ok=True)
    with open(out_path, 'w', encoding='utf-8') as f:
        f.write('\n'.join(parts))
    print(f'Wrote {out_path} with {n} Javadoc failure(s)')
    return 0


if __name__ == '__main__':
    if len(sys.argv) != 3:
        sys.stderr.write(__doc__ or '')
        sys.exit(2)
    sys.exit(main(sys.argv[1], sys.argv[2]))
