#!/usr/bin/env python3
"""Install, launch, diagnose, restore, and remove BikeTube through Android Debug Bridge."""
import argparse
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]
APP = 'local.peloton.probe'
VIEWER = APP + '/.MainActivity'
LAUNCHER = APP + '/.LauncherActivity'
STATE = ROOT / '.biketube-state.json'
COMPONENT = re.compile(r'^[A-Za-z0-9_.]+/[A-Za-z0-9_.$]+$')

class SetupError(Exception):
    pass

class Device:
    def __init__(self, adb, serial=None):
        self.adb = adb
        self.serial = self.select(serial)

    def run(self, *args, selected=True):
        command = [self.adb] + (['-s', self.serial] if selected else []) + list(args)
        result = subprocess.run(command, capture_output=True, text=True, timeout=120)
        output = (result.stdout + result.stderr).strip()
        # Android shell package commands sometimes report failures with exit code zero.
        if result.returncode or 'Error:' in output or 'Failure [' in output or output.startswith('Failed'):
            if 'INSTALL_FAILED_UPDATE_INCOMPATIBLE' in output:
                raise SetupError('The installed app has a different signing key. Use the same key as before. Restore Home before uninstalling if you choose a fresh install.')
            raise SetupError(output or 'ADB command failed.')
        return output

    def select(self, requested):
        listing = self.run('devices', selected=False)
        devices = [line.split()[:2] for line in listing.splitlines() if len(line.split()) >= 2 and not line.startswith(('List ', '*'))]
        if requested:
            status = dict(devices).get(requested)
            if status != 'device':
                raise SetupError('Selected device is not ready. Accept USB debugging on the bike, then try again.')
            return requested
        if len(devices) != 1:
            raise SetupError('Connect exactly one Android device, or choose one with --serial SERIAL.')
        serial, status = devices[0]
        if status != 'device':
            raise SetupError('Accept the USB debugging prompt on the bike. If it is offline, reconnect the data cable.')
        return serial

    def shell(self, *args):
        return self.run('shell', *args)

    def home(self):
        result = self.shell('cmd', 'package', 'resolve-activity', '--brief', '-a', 'android.intent.action.MAIN', '-c', 'android.intent.category.HOME')
        matches = [line.strip() for line in result.splitlines() if COMPONENT.fullmatch(line.strip())]
        return matches[-1] if matches else None

    def restore_component(self, state):
        original = state.get(self.serial, {}).get('home')
        if original and COMPONENT.fullmatch(original) and not original.startswith(APP + '/'):
            return original
        # A migrated prototype may predate the setup state file. Verify the original launcher exists.
        component = 'com.peloton.launcher/.LauncherActivity'
        result = self.shell('cmd', 'package', 'resolve-activity', '--brief', '-n', component)
        if any(line.strip().startswith('com.peloton.launcher/') for line in result.splitlines()):
            return component
        raise SetupError('Original Home is unknown. Choose another Home app in Android settings before removing BikeTube.')

    def restore(self, state):
        original = self.restore_component(state)
        self.shell('cmd', 'package', 'set-home-activity', original)
        if self.home() != original:
            # Android may expand a shorthand activity name.
            def expanded(c):
                package, activity = c.split('/', 1)
                return package + '/' + (package + activity if activity.startswith('.') else activity)
            if not self.home() or expanded(self.home()) != expanded(original):
                raise SetupError('Home restoration could not be verified. BikeTube has not been removed.')
        self.shell('input', 'keyevent', '3')
        print('Original Home restored.')


def read_state():
    if not STATE.exists():
        return {}
    try:
        value = json.loads(STATE.read_text())
        if not isinstance(value, dict):
            raise ValueError()
        return value
    except (ValueError, OSError) as e:
        raise SetupError('The local setup state file cannot be read. Preserve it and repair it before changing Home.') from e


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--adb', default=os.environ.get('ADB', 'adb'), help='Path to Android platform-tools adb')
    parser.add_argument('--serial', help='Choose a device when more than one is connected')
    sub = parser.add_subparsers(dest='action', required=True)
    install = sub.add_parser('install', help='Install or update BikeTube')
    install.add_argument('--apk', type=Path, default=ROOT / 'app/build/outputs/apk/debug/app-debug.apk')
    install.add_argument('--launcher', action='store_true', help='Make BikeTube the Home app')
    install.add_argument('--allow-untested', action='store_true', help='Try a model other than the verified RB1VQ')
    for action in ('doctor', 'open', 'restore-home', 'uninstall'):
        sub.add_parser(action)
    args = parser.parse_args(argv)
    adb = shutil.which(args.adb)
    if not adb:
        raise SetupError('ADB was not found. Install Android SDK platform-tools, or pass --adb /path/to/adb.')
    device = Device(adb, args.serial)
    state = read_state()
    if args.action == 'doctor':
        for name, prop in [('Model', 'ro.product.model'), ('Android', 'ro.build.version.release'), ('API', 'ro.build.version.sdk'), ('Build', 'ro.build.display.id')]:
            print(name + ': ' + device.shell('getprop', prop))
        print('Sensor service: ' + ('installed' if device.shell('pm', 'path', 'com.onepeloton.affernetservice').startswith('package:') else 'missing'))
        print('Home: ' + str(device.home()))
        installed = device.shell('pm', 'path', APP).startswith('package:')
        print('Overlay: ' + (device.shell('appops', 'get', APP, 'SYSTEM_ALERT_WINDOW') if installed else 'app not installed'))
    elif args.action == 'install':
        if not args.apk.is_file():
            raise SetupError('APK not found. Build it with ./gradlew assembleDebug or pass --apk FILE.')
        model = device.shell('getprop', 'ro.product.model')
        try:
            api = int(device.shell('getprop', 'ro.build.version.sdk'))
        except ValueError as e:
            raise SetupError('Cannot determine the Android version.') from e
        if api < 30:
            raise SetupError('BikeTube requires Android 11 (API 30) or later.')
        if model not in ('PLTN-RB1VQ', 'PLTN_RB1VQ', 'RB1VQ') and not args.allow_untested:
            raise SetupError('This model has not been tested. Use --allow-untested only for an explicit compatibility test.')
        if not device.shell('pm', 'path', 'com.onepeloton.affernetservice').startswith('package:'):
            raise SetupError('Peloton sensor service was not found. No changes were made.')
        home = device.home()
        if args.launcher:
            original = home if home and not home.startswith(APP + '/') and 'ResolverActivity' not in home else device.restore_component(state)
            state[device.serial] = {'home': original}
            # Save restoration information before the first Home change.
            STATE.write_text(json.dumps(state, indent=2) + '\n')
        device.run('install', '-r', '--no-incremental', str(args.apk.resolve()))
        device.shell('appops', 'set', APP, 'SYSTEM_ALERT_WINDOW', 'allow')
        if args.launcher:
            device.shell('cmd', 'package', 'set-home-activity', LAUNCHER)
            if device.home() not in (LAUNCHER, APP + '/local.peloton.probe.LauncherActivity'):
                raise SetupError('The app installed, but Home could not be verified. Run restore-home to return to the previous Home.')
            device.shell('input', 'keyevent', '3')
        else:
            device.shell('am', 'start', '-n', VIEWER)
        print('BikeTube installed. Check YouTube playback and pedal to verify live readings.')
    elif args.action == 'open':
        device.shell('am', 'start', '-n', VIEWER)
    elif args.action == 'restore-home':
        device.restore(state)
    elif args.action == 'uninstall':
        if (device.home() or '').startswith(APP + '/'):
            device.restore(state)
        device.run('uninstall', APP)
        print('BikeTube removed. Its local settings and website cookies were deleted.')
    return 0

if __name__ == '__main__':
    try:
        sys.exit(main())
    except (SetupError, subprocess.TimeoutExpired, OSError) as error:
        print('Setup stopped: ' + str(error), file=sys.stderr)
        sys.exit(1)
