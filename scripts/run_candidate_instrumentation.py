#!/usr/bin/env python3
"""Run offline tests against the exact distributed APK on disposable CI emulators."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
from xml.etree import ElementTree


RUNNER = "com.pritam.liquidchat.test/androidx.test.runner.AndroidJUnitRunner"


def evaluate_output(output, returncode=0):
    """ADB can exit successfully when tests fail; require a complete runner result."""
    cases = []
    status = {}
    field = None
    expected = None
    for line in output.replace("\r", "").splitlines():
        match = re.fullmatch(r"INSTRUMENTATION_STATUS: ([^=]+)=(.*)", line)
        if match:
            field = match[1]
            status[field] = match[2]
            if field == "numtests":
                expected = int(match[2])
        elif line.startswith("INSTRUMENTATION_STATUS_CODE: "):
            code = int(line.partition(": ")[2])
            if code != 1 and status.get("class") and status.get("test"):
                cases.append({"class": status["class"], "name": status["test"],
                              "code": code, "stack": status.get("stack", "")})
            status = {}
            field = None
        elif field:
            status[field] += "\n" + line
    failures = []
    if returncode:
        failures.append(f"ADB instrumentation process failed ({returncode}).")
    if not re.search(r"^INSTRUMENTATION_CODE: -1\s*$", output, re.MULTILINE):
        failures.append("Runner did not finish normally.")
    if expected is None or expected <= 0 or len(cases) != expected:
        failures.append(f"Incomplete test run: expected {expected}, completed {len(cases)}.")
    if any(case["code"] not in (0, -3, -4) for case in cases):
        failures.append("One or more instrumentation tests failed.")
    return cases, failures


def write_junit(output, returncode, path):
    cases, failures = evaluate_output(output, returncode)
    suite = ElementTree.Element("testsuite", name="ExactCandidateInstrumentation",
                                tests=str(len(cases) + bool(failures)))
    for case in cases:
        node = ElementTree.SubElement(suite, "testcase", classname=case["class"], name=case["name"])
        if case["code"] in (-3, -4):
            ElementTree.SubElement(node, "skipped").text = case["stack"]
        elif case["code"] != 0:
            ElementTree.SubElement(node, "failure").text = case["stack"] or f"Status {case['code']}"
    if failures:
        node = ElementTree.SubElement(suite, "testcase", classname="runner", name="completeRun")
        ElementTree.SubElement(node, "failure").text = "\n".join(failures)
    suite.set("failures", str(len(suite.findall("./testcase/failure"))))
    suite.set("skipped", str(len(suite.findall("./testcase/skipped"))))
    ElementTree.ElementTree(suite).write(path, encoding="utf-8", xml_declaration=True)
    return failures


SHARD_COUNT = 4
# Explicit suite floor: never turn an incomplete discovery/test fixture into green CI.
MINIMUM_INSTRUMENTATION_TESTS = 80
IMAGE_TEST_CLASS = "com.pritam.liquidchat.ui.ProtectedImageCacheTest"


def verify_sharded_cases(cases, errors, minimum=MINIMUM_INSTRUMENTATION_TESTS):
    problems = list(errors)
    seen = set()
    for case in cases:
        key = (case["class"], case["name"])
        if key in seen:
            problems.append(f"Duplicate instrumented test across shards: {key}.")
        seen.add(key)
        if case["code"] in (-3, -4):
            problems.append(f"Skipped mandatory instrumentation test: {key}.")
    if len(seen) < minimum:
        problems.append(f"Incomplete instrumentation suite: {len(seen)} distinct, expected at least {minimum}.")
    return problems


def combine_junit(shard_paths, target, validation_errors):
    suite = ElementTree.Element("testsuite", name="ExactCandidateInstrumentationSharded")
    for source in shard_paths:
        root = ElementTree.parse(source).getroot()
        for case in root.findall("testcase"):
            suite.append(case)
    if validation_errors:
        case = ElementTree.SubElement(suite, "testcase", classname="runner", name="shardCoverage")
        ElementTree.SubElement(case, "failure").text = "\n".join(validation_errors)
    suite.set("tests", str(len(suite.findall("testcase"))))
    suite.set("failures", str(len(suite.findall("./testcase/failure"))))
    suite.set("skipped", str(len(suite.findall("./testcase/skipped"))))
    ElementTree.ElementTree(suite).write(target, encoding="utf-8", xml_declaration=True)


def verify_image_repeat(cases, errors, expected):
    """Require the full class discovered in the mandatory suite on every repeat."""
    problems = list(errors)
    actual = {(case["class"], case["name"]) for case in cases}
    if not expected:
        problems.append("Mandatory suite did not discover protected-image tests.")
    if actual != expected:
        problems.append(f"Protected-image repeat coverage differs: missing {sorted(expected - actual)}, "
                        f"unexpected {sorted(actual - expected)}.")
    return verify_sharded_cases(cases, problems, minimum=len(expected))


def run_instrumentation(command):
    try:
        return subprocess.run(command, text=True, stdout=subprocess.PIPE,
                              stderr=subprocess.STDOUT, timeout=6 * 60)
    except subprocess.TimeoutExpired as error:
        output = error.stdout or ""
        if isinstance(output, bytes):
            output = output.decode("utf-8", errors="replace")
        return subprocess.CompletedProcess(command, 124,
                                           output + "\nInstrumentation process timed out.\n")


def run_image_repeats(adb, report_dir, expected, rounds):
    """Fresh runner invocations expose Activity teardown races; no retry-to-green."""
    failures = []
    summary = []
    for index in range(rounds):
        command = adb + ["shell", "am", "instrument", "-w", "-r",
                         "-e", "expectedExperimentalCalls", "true",
                         "-e", "class", IMAGE_TEST_CLASS, RUNNER]
        result = run_instrumentation(command)
        prefix = report_dir / f"image-repeat-{index + 1}"
        prefix.with_suffix(".txt").write_text(result.stdout, encoding="utf-8")
        problems = write_junit(result.stdout, result.returncode, prefix.with_suffix(".xml"))
        cases, _ = evaluate_output(result.stdout, result.returncode)
        problems = verify_image_repeat(cases, problems, expected)
        # Keep class coverage/skip failures in the machine-readable report too.
        combine_junit([prefix.with_suffix(".xml")], prefix.with_suffix(".xml"), problems)
        failures.extend(f"Image repeat {index + 1}: {item}" for item in problems)
        summary.append({"round": index + 1, "completed": len(cases), "errors": problems})
        print(f"Protected-image repeat {index + 1}/{rounds}: {len(cases)} tests, "
              f"{'FAIL' if problems else 'PASS'}", flush=True)
        if problems:
            # Preserve the first failure rather than passing after a later attempt.
            break
    (report_dir / "image-repeat-summary.json").write_text(json.dumps({
        "requested_rounds": rounds, "completed_rounds": len(summary),
        "expected_tests": sorted(expected), "rounds": summary,
        "physical_device_acceptance": "NOT VERIFIED",
        "root_cause": "NOT ESTABLISHED by successful repeats",
    }, indent=2) + "\n", encoding="utf-8")
    return failures


def disposable_emulator():
    if os.environ.get("LIQUID_DISPOSABLE_EMULATOR") != "1":
        raise RuntimeError("A disposable emulator must be explicitly selected.")
    serial = subprocess.check_output(["adb", "-e", "get-serialno"], text=True).strip()
    if not re.fullmatch(r"emulator-[0-9]+", serial):
        raise RuntimeError("Expected a local emulator serial.")
    adb = ["adb", "-s", serial]
    if subprocess.check_output(adb + ["shell", "getprop", "ro.kernel.qemu"], text=True).strip() != "1":
        raise RuntimeError("Target is not a verified emulator.")
    return adb


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("candidate", type=Path)
    parser.add_argument("tests", type=Path)
    parser.add_argument("report_dir", type=Path)
    parser.add_argument("--image-stress-rounds", type=int, choices=range(0, 11), default=0,
                        metavar="0..10", help="Extra fresh-process protected-image runs after the full suite")
    args = parser.parse_args()
    if not args.candidate.is_file() or not args.tests.is_file():
        parser.error("Both the exact candidate APK and its test fixture are required.")
    adb = disposable_emulator()
    args.report_dir.mkdir(parents=True, exist_ok=True)
    (args.report_dir / "candidate.json").write_text(json.dumps({
        "candidate_sha256": hashlib.sha256(args.candidate.read_bytes()).hexdigest(),
        "instrumentation_sha256": hashlib.sha256(args.tests.read_bytes()).hexdigest(),
        "expected_experimental_calls": True,
        "device_scope": "disposable Android emulator; physical two-phone QA not performed",
    }, indent=2) + "\n", encoding="utf-8")
    # This is a fresh CI AVD. Keep the test target identical to the uploaded APK;
    # never rebuild it on a different runner with different FCM/signing values.
    for apk in (args.candidate, args.tests):
        subprocess.run(adb + ["install", "-r", str(apk)], check=True)
    all_cases = []
    all_failures = []
    paths = []
    logs = []
    # Separate Android test processes reduce Compose/Activity teardown races,
    # but every test MUST still be executed exactly once and none may be skipped.
    for index in range(SHARD_COUNT):
        command = adb + ["shell", "am", "instrument", "-w", "-r",
                         "-e", "expectedExperimentalCalls", "true",
                         "-e", "numShards", str(SHARD_COUNT),
                         "-e", "shardIndex", str(index), RUNNER]
        result = run_instrumentation(command)
        logs.append(f"=== SHARD {index + 1}/{SHARD_COUNT} ===\n" + result.stdout)
        report = args.report_dir / f"junit-shard-{index}.xml"
        paths.append(report)
        problems = write_junit(result.stdout, result.returncode, report)
        cases, _ = evaluate_output(result.stdout, result.returncode)
        all_cases.extend(cases)
        all_failures.extend(f"Shard {index + 1}: {item}" for item in problems)
    (args.report_dir / "instrumentation.txt").write_text("\n".join(logs), encoding="utf-8")
    print("\n".join(logs))
    all_failures = verify_sharded_cases(all_cases, all_failures)
    combine_junit(paths, args.report_dir / "junit.xml", all_failures)
    if all_failures:
        raise SystemExit("\n".join(all_failures))
    if args.image_stress_rounds:
        expected = {(case["class"], case["name"]) for case in all_cases
                    if case["class"] == IMAGE_TEST_CLASS}
        failures = run_image_repeats(adb, args.report_dir, expected, args.image_stress_rounds)
        if failures:
            raise SystemExit("\n".join(failures))


if __name__ == "__main__":
    main()
