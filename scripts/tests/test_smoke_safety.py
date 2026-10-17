"""Safety boundary tests: no real ADB, device, APK install or account data."""
import json
import os
from pathlib import Path
import subprocess
import tempfile
import unittest


SCRIPT = Path(__file__).resolve().parents[1] / "smoke_candidate.sh"


class SmokeSafetyTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.log = self.root / "adb.jsonl"
        self.apk = self.root / "fixture.apk"
        self.apk.write_bytes(b"not-an-installable-apk")
        fake = self.root / "adb"
        fake.write_text('''#!/usr/bin/env python3
import json,os,sys
args=sys.argv[1:]
with open(os.environ['FAKE_ADB_LOG'],'a') as log: log.write(json.dumps(args)+'\\n')
if args==['-e','get-serialno']:
    print(os.environ.get('FAKE_SERIAL','emulator-5554'));sys.exit(0)
if args[:1]==['-s']: args=args[2:]
if args==['shell','getprop','ro.kernel.qemu']:
    if os.environ.get('FAKE_PROP_FAILURE')=='1': sys.exit(1)
    print(os.environ.get('FAKE_QEMU','1')+'\\r');sys.exit(0)
if args[:2]==['shell','pidof']: print('1234')
elif args[:3]==['shell','am','start']: print('Status: ok')
elif args[:1]==['install']: print('Success')
elif args[:2]==['exec-out','screencap']: sys.stdout.buffer.write(b'fixture screenshot')
''')
        fake.chmod(0o700)
        sleep = self.root / "sleep"
        sleep.write_text("#!/bin/sh\nexit 0\n")
        sleep.chmod(0o700)
        self.env = {**os.environ, "PATH": str(self.root) + os.pathsep + os.environ['PATH'],
                    "FAKE_ADB_LOG": str(self.log)}
        self.env.pop("LIQUID_DISPOSABLE_EMULATOR", None)

    def invoke(self, **env):
        return subprocess.run(["bash", str(SCRIPT), str(self.apk), str(self.root / 'reports')],
                              env={**self.env, **env}, capture_output=True, timeout=10)

    def calls(self):
        return [json.loads(line) for line in self.log.read_text().splitlines()] if self.log.exists() else []

    def assertNoDestructiveCalls(self, result):
        self.assertNotEqual(result.returncode, 0)
        self.assertFalse(any('uninstall' in call or 'install' in call for call in self.calls()))

    def test_refuses_without_disposable_emulator_opt_in(self):
        self.assertNoDestructiveCalls(self.invoke())
        self.assertEqual(self.calls(), [])

    def test_refuses_physical_device_serial_even_with_opt_in(self):
        self.assertNoDestructiveCalls(self.invoke(LIQUID_DISPOSABLE_EMULATOR='1', FAKE_SERIAL='physical-123'))

    def test_refuses_emulator_named_device_without_qemu_property(self):
        self.assertNoDestructiveCalls(self.invoke(LIQUID_DISPOSABLE_EMULATOR='1', FAKE_QEMU='0'))

    def test_property_failure_cannot_fall_through_to_uninstall(self):
        self.assertNoDestructiveCalls(self.invoke(LIQUID_DISPOSABLE_EMULATOR='1', FAKE_PROP_FAILURE='1'))

    def test_disposable_emulator_uses_pinned_serial_for_the_exact_apk(self):
        result = self.invoke(LIQUID_DISPOSABLE_EMULATOR='1')
        self.assertEqual(result.returncode, 0, result.stderr.decode())
        calls = self.calls()
        self.assertIn(['-s', 'emulator-5554', 'install', str(self.apk)], calls)
        self.assertEqual(sum('uninstall' in call for call in calls), 2)
        self.assertTrue(all(call[:2] == ['-s', 'emulator-5554'] for call in calls[1:]))
        self.assertEqual((self.root / 'reports/process.txt').read_text().strip(), '1234')


if __name__ == "__main__":
    unittest.main()
