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


IMAGE_TEARDOWN_CLASS = "com.pritam.liquidchat.ui.ProtectedImageCacheTest"
IMAGE_TEARDOWN_TESTS = {
    "tabReturnReusesBitmapAndAvatarMutationFetchesNewImage",
    "expiredImageTransferRenewsLinkOnceAutomatically",
    "expiredPermissionIsRecheckedAndRevokedImageDisappears",
    "repeatedTransferRejectionStopsAndManualRetryIsBounded",
    "serverLifetimeOverridesLongerLocalCacheAndForceBypassesIt",
}


def run_image_teardown_stress(adb, repetitions, report_dir):
    """Additional fresh runner processes; no retry can erase a failing repetition."""
    if not 1 <= repetitions <= 10:
        raise ValueError("Image teardown repetitions must be between 1 and 10.")
    report_dir.mkdir(parents=True, exist_ok=True)
    failures = []
    paths = []
    for index in range(repetitions):
        command = adb + ["shell", "am", "instrument", "-w", "-r",
                         "-e", "expectedExperimentalCalls", "true",
                         "-e", "class", IMAGE_TEARDOWN_CLASS, RUNNER]
        try:
            result = subprocess.run(command, text=True, stdout=subprocess.PIPE,
                                    stderr=subprocess.STDOUT, timeout=180)
        except subprocess.TimeoutExpired as error:
            output = error.stdout or ""
            if isinstance(output, bytes):
                output = output.decode("utf-8", errors="replace")
            result = subprocess.CompletedProcess(command, 124,
                                                 output + "\nImage teardown process timed out.\n")
        stem = report_dir / f"image-teardown-{index + 1}"
        stem.with_suffix(".txt").write_text(result.stdout, encoding="utf-8")
        report = stem.with_suffix(".xml")
        paths.append(report)
        problems = write_junit(result.stdout, result.returncode, report)
        cases, _ = evaluate_output(result.stdout, result.returncode)
        problems = verify_sharded_cases(cases, problems, minimum=len(IMAGE_TEARDOWN_TESTS))
        actual = {(case["class"], case["name"]) for case in cases}
        expected = {(IMAGE_TEARDOWN_CLASS, name) for name in IMAGE_TEARDOWN_TESTS}
        if actual != expected:
            problems.append("Image teardown fixture discovery differs from the mandatory test inventory.")
        failures.extend(f"Repetition {index + 1}: {problem}" for problem in problems)
        print(f"Image teardown repetition {index + 1}/{repetitions}: "
              f"{len(cases)} tests, {'FAIL' if problems else 'PASS'}", flush=True)
        # Preserve crash evidence even when AndroidJUnitRunner dies before a result.
        diagnostic = subprocess.run(adb + ["logcat", "-d", "-v", "threadtime",
                                          "AndroidRuntime:E", "libc:F", "ActivityManager:W", "*:S"],
                                    text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                                    timeout=30)
        stem.with_suffix(".logcat.txt").write_text(diagnostic.stdout, encoding="utf-8")
        if diagnostic.returncode:
            failures.append(f"Repetition {index + 1}: crash diagnostics could not be collected.")
    combine_junit(paths, report_dir / "junit.xml", failures)
    (report_dir / "summary.json").write_text(json.dumps({
        "class": IMAGE_TEARDOWN_CLASS,
        "repetitions": repetitions,
        "expected_tests_per_repetition": len(IMAGE_TEARDOWN_TESTS),
        "failures": failures,
        "root_cause": "Unconfirmed; repeat success alone is not permanent resolution.",
    }, indent=2) + "\n", encoding="utf-8")
    return failures


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("candidate", type=Path)
    parser.add_argument("tests", type=Path)
    parser.add_argument("report_dir", type=Path)
    parser.add_argument("--image-teardown-repetitions", type=int, default=0,
                        help="Additional complete protected-image fixture runs (0 disables, max 10).")
    args = parser.parse_args()
    if not 0 <= args.image_teardown_repetitions <= 10:
        parser.error("Image teardown repetitions must be between 0 and 10.")
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
        try:
            result = subprocess.run(command, text=True, stdout=subprocess.PIPE,
                                    stderr=subprocess.STDOUT, timeout=6 * 60)
        except subprocess.TimeoutExpired as error:
            output = error.stdout or ""
            if isinstance(output, bytes):
                output = output.decode("utf-8", errors="replace")
            result = subprocess.CompletedProcess(command, 124,
                                                 output + "\nInstrumentation shard timed out.\n")
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
    # Full-suite failures remain failures even if all additional runs pass.
    if args.image_teardown_repetitions:
        all_failures.extend(run_image_teardown_stress(
            adb, args.image_teardown_repetitions, args.report_dir / "image-teardown-stress"))
    if all_failures:
        raise SystemExit("\n".join(all_failures))


if __name__ == "__main__":
    main()
