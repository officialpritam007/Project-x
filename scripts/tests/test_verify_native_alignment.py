import importlib.util
import struct
import tempfile
import unittest
import zipfile
from pathlib import Path

spec = importlib.util.spec_from_file_location(
    "verify_artifacts", Path(__file__).resolve().parents[1] / "verify_artifacts.py"
)
verify = importlib.util.module_from_spec(spec)
spec.loader.exec_module(verify)


def fake_elf(wide, alignment):
    header = 64 if wide else 52
    ph_size = 56 if wide else 32
    elf = bytearray(header + ph_size)
    elf[:4] = b"\x7fELF"
    elf[4] = 2 if wide else 1
    elf[5] = 1
    if wide:
        struct.pack_into("<Q", elf, 32, header)
        struct.pack_into("<HH", elf, 54, ph_size, 1)
        struct.pack_into("<I", elf, header, 1)
        struct.pack_into("<Q", elf, header + 48, alignment)
    else:
        struct.pack_into("<I", elf, 28, header)
        struct.pack_into("<HH", elf, 42, ph_size, 1)
        struct.pack_into("<I", elf, header, 1)
        struct.pack_into("<I", elf, header + 28, alignment)
    return bytes(elf)


class ElfPageSizeTest(unittest.TestCase):
    def verify(self, name, wide, align):
        with tempfile.TemporaryDirectory() as root:
            output = Path(root) / "candidate.apk"
            with zipfile.ZipFile(output, "w") as archive:
                archive.writestr(name, fake_elf(wide, align))
            return verify.verify_native(output)

    def test_32bit_arm_4k_alignment_is_supported(self):
        libraries = self.verify("lib/armeabi-v7a/libjingle_peerconnection_so.so", False, 4096)
        self.assertEqual(libraries[0]["load_alignment"], 4096)

    def test_32bit_x86_4k_alignment_is_supported(self):
        self.assertEqual(self.verify("lib/x86/libexample.so", False, 4096)[0]["load_alignment"], 4096)

    def test_32bit_2k_alignment_is_rejected(self):
        with self.assertRaisesRegex(ValueError, "4 KB alignment"):
            self.verify("lib/armeabi-v7a/libexample.so", False, 2048)

    def test_64bit_arm_4k_alignment_is_rejected(self):
        with self.assertRaisesRegex(ValueError, "16 KB alignment"):
            self.verify("lib/arm64-v8a/libjingle_peerconnection_so.so", True, 4096)

    def test_64bit_x86_4k_alignment_is_rejected(self):
        with self.assertRaisesRegex(ValueError, "16 KB alignment"):
            self.verify("base/lib/x86_64/libexample.so", True, 4096)

    def test_64bit_16k_alignment_is_accepted(self):
        self.assertEqual(self.verify("base/lib/arm64-v8a/libexample.so", True, 16384)[0]["load_alignment"], 16384)

    def test_64bit_64k_alignment_is_accepted(self):
        self.assertEqual(self.verify("lib/x86_64/libexample.so", True, 65536)[0]["load_alignment"], 65536)

    def test_64bit_non_power_of_two_alignment_is_rejected(self):
        with self.assertRaisesRegex(ValueError, "16 KB alignment"):
            self.verify("lib/x86_64/libexample.so", True, 24576)

    def test_unexpected_archive_layout_is_rejected(self):
        with self.assertRaisesRegex(ValueError, "Unexpected native library packaging"):
            self.verify("assets/random.so", True, 16384)


if __name__ == "__main__":
    unittest.main()
