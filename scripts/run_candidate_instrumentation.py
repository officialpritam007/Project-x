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
    command = adb + ["shell", "am", "instrument", "-w", "-r",
                     "-e", "expectedExperimentalCalls", "true", RUNNER]
    try:
        result = subprocess.run(command, text=True, stdout=subprocess.PIPE,
                                stderr=subprocess.STDOUT, timeout=15 * 60)
    except subprocess.TimeoutExpired as error:
        output = error.stdout or ""
        if isinstance(output, bytes):
            output = output.decode("utf-8", errors="replace")
        result = subprocess.CompletedProcess(command, 124, output + "\nInstrumentation timed out.\n")
    (args.report_dir / "instrumentation.txt").write_text(result.stdout, encoding="utf-8")
    failures = write_junit(result.stdout, result.returncode, args.report_dir / "junit.xml")
    print(result.stdout)
    if failures:
        raise SystemExit("\n".join(failures))


if __name__ == "__main__":
    main()
