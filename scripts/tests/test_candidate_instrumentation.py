"""Fail-closed result parsing and emulator safety; no device is contacted."""
import importlib.util
import io
import os
from pathlib import Path
import subprocess
import tempfile
import json
import re
import unittest
from unittest.mock import patch
from xml.etree import ElementTree


SCRIPT = Path(__file__).resolve().parents[1] / "run_candidate_instrumentation.py"
spec = importlib.util.spec_from_file_location("candidate_instrumentation", SCRIPT)
runner = importlib.util.module_from_spec(spec)
spec.loader.exec_module(runner)


def result(code=0, count=1):
    return f"""INSTRUMENTATION_STATUS: class=fixture.Test
INSTRUMENTATION_STATUS: test=first
INSTRUMENTATION_STATUS: numtests={count}
INSTRUMENTATION_STATUS_CODE: 1
INSTRUMENTATION_STATUS: class=fixture.Test
INSTRUMENTATION_STATUS: test=first
INSTRUMENTATION_STATUS: numtests={count}
INSTRUMENTATION_STATUS: stack=First failure line
Second failure line
INSTRUMENTATION_STATUS_CODE: {code}
INSTRUMENTATION_CODE: -1
"""


class InstrumentationResultTest(unittest.TestCase):
    def test_complete_success_passes(self):
        cases, failures = runner.evaluate_output(result())
        self.assertFalse(failures)
        self.assertEqual(len(cases), 1)

    def test_test_failure_fails_even_when_adb_exits_zero(self):
        cases, failures = runner.evaluate_output(result(-2))
        self.assertTrue(failures)
        self.assertEqual(cases[0]["stack"], "First failure line\nSecond failure line")

    def test_crash_partial_run_empty_run_and_adb_failure_fail(self):
        for output, code in ((result().replace("INSTRUMENTATION_CODE: -1", ""), 0),
                             (result(count=2), 0), ("INSTRUMENTATION_CODE: -1", 0),
                             (result(), 1)):
            with self.subTest(output=output, code=code):
                self.assertTrue(runner.evaluate_output(output, code)[1])

    def test_skipped_test_is_recorded(self):
        output = io.BytesIO()
        self.assertFalse(runner.write_junit(result(-3), 0, output))
        suite = ElementTree.fromstring(output.getvalue())
        self.assertEqual(suite.attrib["skipped"], "1")


class ShardCoverageTest(unittest.TestCase):
    def test_exact_once_coverage_passes(self):
        cases=[{"class":"fixture.One","name":"first","code":0},
               {"class":"fixture.Two","name":"second","code":0}]
        self.assertFalse(runner.verify_sharded_cases(cases, [], minimum=2))

    def test_missing_duplicate_or_skipped_tests_are_never_green(self):
        a={"class":"fixture.One","name":"first","code":0}
        for cases, minimum in (([a],2),([a,a],1),
                               ([{"class":"fixture.Two","name":"skipped","code":-3}],1)):
            with self.subTest(cases=cases,minimum=minimum):
                self.assertTrue(runner.verify_sharded_cases(cases, [],minimum=minimum))

    def test_known_failure_is_not_reclassified_as_success(self):
        self.assertTrue(runner.verify_sharded_cases(
            [{"class":"fixture.Test","name":"one","code":-2}],
            ["Shard 1: One or more instrumentation tests failed."],minimum=1))


class ImageTeardownStressTest(unittest.TestCase):
    def output(self, missing=False, skipped=False):
        names = sorted(runner.IMAGE_TEARDOWN_TESTS)
        cases = []
        for index, name in enumerate(names[:-1] if missing else names):
            cases.append(f"INSTRUMENTATION_STATUS: class={runner.IMAGE_TEARDOWN_CLASS}\n"
                         f"INSTRUMENTATION_STATUS: test={name}\n"
                         f"INSTRUMENTATION_STATUS: numtests={len(names)}\n"
                         f"INSTRUMENTATION_STATUS_CODE: {-3 if skipped and index == 0 else 0}\n")
        return "".join(cases) + "INSTRUMENTATION_CODE: -1\n"

    def run_stress(self, directory, outputs, diagnostics_code=0):
        responses = []
        for output in outputs:
            if isinstance(output, Exception):
                responses.append(output)
            else:
                responses.append(subprocess.CompletedProcess([], 0, output))
            responses.append(subprocess.CompletedProcess([], diagnostics_code, "fixture diagnostics"))
        with patch.object(subprocess, "run", side_effect=responses) as invoke:
            failures = runner.run_image_teardown_stress(["adb", "-s", "emulator-5554"],
                                                        len(outputs), Path(directory))
        self.assertEqual(invoke.call_count, len(outputs) * 2)
        for call in invoke.call_args_list[::2]:
            command = call.args[0]
            self.assertIn(runner.IMAGE_TEARDOWN_CLASS, command)
            self.assertNotIn("shardIndex", command)
        return failures

    def test_repeats_the_complete_fixture_in_fresh_processes(self):
        with tempfile.TemporaryDirectory() as directory:
            self.assertFalse(self.run_stress(directory, [self.output()] * 3))
            summary = json.loads((Path(directory) / "summary.json").read_text())
            self.assertEqual(summary["repetitions"], 3)
            self.assertEqual(len(list(Path(directory).glob("*.logcat.txt"))), 3)

    def test_partial_or_skipped_repetition_never_becomes_green_after_success(self):
        for first in (self.output(missing=True), self.output(skipped=True),
                      subprocess.TimeoutExpired("adb", 180, output=b"fixture timeout")):
            with self.subTest(first=first), tempfile.TemporaryDirectory() as directory:
                failures = self.run_stress(directory, [first, self.output()])
                self.assertTrue(failures)
                self.assertTrue(any("Repetition 1" in item for item in failures))
                suite = ElementTree.parse(Path(directory) / "junit.xml").getroot()
                self.assertGreater(int(suite.attrib["failures"]), 0)

    def test_missing_crash_diagnostics_are_reported(self):
        with tempfile.TemporaryDirectory() as directory:
            self.assertTrue(self.run_stress(directory, [self.output()], diagnostics_code=1))

    def test_inventory_tracks_every_image_fixture_test(self):
        source = SCRIPT.parents[1] / "app/src/androidTest/java/com/pritam/liquidchat/ui/ProtectedImageCacheTest.kt"
        self.assertEqual(set(re.findall(r"@Test\s+fun\s+(\w+)\(", source.read_text())),
                         runner.IMAGE_TEARDOWN_TESTS)

    def test_unbounded_or_empty_stress_is_rejected_before_adb(self):
        with patch.object(subprocess, "run") as invoke:
            for repetitions in (0, -1, 11):
                with self.assertRaises(ValueError):
                    runner.run_image_teardown_stress([], repetitions, Path("unused"))
            invoke.assert_not_called()


class EmulatorSafetyTest(unittest.TestCase):
    @patch.dict(os.environ, {}, clear=True)
    @patch.object(subprocess, "check_output")
    def test_missing_opt_in_never_contacts_device(self, invoke):
        with self.assertRaises(RuntimeError):
            runner.disposable_emulator()
        invoke.assert_not_called()

    @patch.dict(os.environ, {"LIQUID_DISPOSABLE_EMULATOR": "1"})
    @patch.object(subprocess, "check_output")
    def test_physical_serial_and_fake_emulator_are_refused(self, invoke):
        for replies in (("physical-123\n",), ("emulator-5554\n", "0\n")):
            invoke.side_effect = replies
            with self.assertRaises(RuntimeError):
                runner.disposable_emulator()

    @patch.dict(os.environ, {"LIQUID_DISPOSABLE_EMULATOR": "1"})
    @patch.object(subprocess, "check_output", side_effect=["emulator-5554\r\n", "1\r\n"])
    def test_emulator_calls_are_pinned(self, invoke):
        self.assertEqual(runner.disposable_emulator(), ["adb", "-s", "emulator-5554"])
        self.assertEqual(invoke.call_args.args[0],
                         ["adb", "-s", "emulator-5554", "shell", "getprop", "ro.kernel.qemu"])


if __name__ == "__main__":
    unittest.main()
