import importlib.util
from pathlib import Path
import unittest
from unittest.mock import patch

spec = importlib.util.spec_from_file_location('device', Path(__file__).with_name('device.py'))
device = importlib.util.module_from_spec(spec)
spec.loader.exec_module(device)

class DeviceSelectionTest(unittest.TestCase):
    def select(self, listing, serial=None):
        obj = object.__new__(device.Device)
        with patch.object(obj, 'run', return_value=listing):
            return obj.select(serial)
    def test_refuses_multiple_even_if_only_one_is_authorized(self):
        with self.assertRaises(device.SetupError):
            self.select('List of devices attached\na\tdevice\nb\tunauthorized\n')
    def test_unauthorized_is_not_ready(self):
        with self.assertRaises(device.SetupError):
            self.select('List of devices attached\na\tunauthorized\n')
    def test_explicit_selection_does_not_use_another_device(self):
        with self.assertRaises(device.SetupError):
            self.select('List of devices attached\na\tdevice\nb\tunauthorized\n', 'b')
        self.assertEqual('a', self.select('List of devices attached\na\tdevice\nb\toffline\n', 'a'))
    def test_restore_failure_does_not_proceed(self):
        obj = object.__new__(device.Device)
        obj.serial = 'test'
        with patch.object(obj, 'shell', side_effect=device.SetupError('restore rejected')) as shell:
            with self.assertRaises(device.SetupError):
                obj.restore({'test': {'home': 'com.peloton.launcher/.LauncherActivity'}})
            self.assertEqual(1, shell.call_count)
    def test_biketube_cannot_be_its_own_restore_target(self):
        obj = object.__new__(device.Device)
        obj.serial = 'test'
        with patch.object(obj, 'shell', return_value='No activity found'):
            with self.assertRaises(device.SetupError):
                obj.restore_component({'test': {'home': device.LAUNCHER}})

if __name__ == '__main__':
    unittest.main()
