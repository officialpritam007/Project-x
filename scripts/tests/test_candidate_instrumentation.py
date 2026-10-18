"""Fail-closed result parsing and emulator safety; no device is contacted."""
import importlib.util
import io
import os
from pathlib import Path
import subprocess
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
