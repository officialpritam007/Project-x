"""Release-safety checks for the Android 14+ call foreground-service contract."""
import unittest
from pathlib import Path
from xml.etree import ElementTree

ROOT=Path(__file__).resolve().parents[2]
ANDROID="{http://schemas.android.com/apk/res/android}"


class CallForegroundServicePolicyTest(unittest.TestCase):
    def test_user_started_mic_camera_service_not_exported(self):
        tree=ElementTree.parse(ROOT/"app/src/main/AndroidManifest.xml")
        svc=next(s for s in tree.findall(".//service") if s.attrib.get(ANDROID+"name")==".calls.ActiveCallService")
        self.assertEqual(svc.attrib.get(ANDROID+"exported"),"false")
        self.assertEqual(svc.attrib.get(ANDROID+"stopWithTask"),"true")
        self.assertEqual(set(svc.attrib.get(ANDROID+"foregroundServiceType","").split("|")),{"camera","microphone"})

    def test_foreground_runtime_permissions_exist(self):
        tree=ElementTree.parse(ROOT/"app/src/main/AndroidManifest.xml")
        permissions={p.attrib.get(ANDROID+"name") for p in tree.findall("uses-permission")}
        for name in ("android.permission.FOREGROUND_SERVICE","android.permission.FOREGROUND_SERVICE_CAMERA",
                     "android.permission.FOREGROUND_SERVICE_MICROPHONE","android.permission.CAMERA",
                     "android.permission.RECORD_AUDIO"):
            self.assertIn(name,permissions)

    def test_no_foreground_service_from_fcm_worker(self):
        worker=(ROOT/"app/src/main/java/com/pritam/liquidchat/data/Push.kt").read_text()
        self.assertNotIn("ActiveCallService.start(",worker)
        self.assertNotIn("startForegroundService(",worker)

    def test_explicit_call_notification_end_action_and_no_restart(self):
        src=(ROOT/"app/src/main/java/com/pritam/liquidchat/calls/ActiveCallService.kt").read_text()
        self.assertIn("return START_NOT_STICKY",src)
        self.assertIn("PendingIntent.FLAG_IMMUTABLE",src)
        self.assertIn("FOREGROUND_SERVICE_TYPE_MICROPHONE",src)
        self.assertIn("FOREGROUND_SERVICE_TYPE_CAMERA",src)
        self.assertIn("onHangup?.invoke()",src)

    def test_no_unexpected_paid_turn_provider(self):
        src=(ROOT/"app/src/main/java/com/pritam/liquidchat/calls/CallEngine.kt").read_text()
        self.assertIn("stun:stun.cloudflare.com:3478",src)
        self.assertNotIn("turn.cloudflare.com",src)

if __name__=="__main__":
    unittest.main()
