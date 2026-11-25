#!/usr/bin/env python3
"""Extract Javadoc problems from a Maven log and emit a JUnit XML report.

Usage: javadocErrorsToJUnit.py <maven-log> <output-xml>

Two tools generate the Javadoc here, and each reports a failed run
differently:

- maven-javadoc-plugin (pure-Java modules) prints every problem of a
  failed run as a block of header + source line + caret. Each line is
  prefixed by Maven's [ERROR] marker, or by [WARNING] when the run had only
  warnings. The header says "error:" or "warning:"; with failOnWarnings
  both fail the build.
- Dokka (mixed Java/Kotlin modules) logs each problem as a single
  [WARNING] or [ERROR] line while it runs and only fails at the end, with
  "Failed with warningCount=... and errorCount=...". Its messages name the
  source file but not the line.

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

DOKKA_START = re.compile(
    r'^\[INFO\] --- dokka:\S+:javadocJar .*@ (\S+) ---$', re.M)
DOKKA_FAILED = 'on project {}: .*Failed with warningCount='
DOKKA_PROBLEM = re.compile(r'^\[(?:WARNING|ERROR)\] (.*)$')
# The end of the Dokka run: the next plugin execution or the build result.
DOKKA_END = re.compile(r'^\[INFO\] (?:--- |-{20,}$|BUILD )')
DOKKA_SOURCE = re.compile(r'\bin ([\w$-]+\.(?:kt|java))/')


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


def find_source(name, root):
    """Return the only file called name under root, or None."""
    matches = []
    for directory, subdirs, files in os.walk(root):
        subdirs[:] = [d for d in subdirs
                      if d not in ('target', 'node_modules')
                      and not d.startswith('.')]
        if name in files:
            matches.append(os.path.join(directory, name))
    return matches[0] if len(matches) == 1 else None


def dokka_problems(log, workspace):
    lines = log.splitlines()
    for start in DOKKA_START.finditer(log):
        # Maven reports the failed goal after the Dokka run has finished,
        # so look for this project's failure in the rest of the log.
        failed = DOKKA_FAILED.format(re.escape(start.group(1)))
        if not re.search(failed, log[start.end():]):
            continue
        index = log.count('\n', 0, start.start()) + 1
        messages = []
        for line in lines[index:]:
            if DOKKA_END.match(line):
                break
            problem = DOKKA_PROBLEM.match(line)
            if problem:
                messages.append(problem.group(1))
        for msg in messages:
            source = DOKKA_SOURCE.search(msg)
            path = find_source(source.group(1), workspace or '.') \
                if source else None
            file_rel = relativize(os.path.abspath(path), workspace) \
                if path else None
            name = f'{file_rel}: {msg}' if file_rel else msg
            yield (name, file_rel, None, msg, msg)


def main(log_path, out_path):
    with open(log_path, encoding='utf-8', errors='replace') as f:
        log = f.read()
    workspace = os.environ.get('GITHUB_WORKSPACE', '')

    problems = list(javadoc_problems(log, workspace))
    problems += dokka_problems(log, workspace)
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
