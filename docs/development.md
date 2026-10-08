# Development

## Build

The app uses Java, Android's built-in views and WebView, Android Gradle Plugin 8.5.2, and Gradle 8.7. The compile platform is API 34. The minimum and target API levels are 30.

The target remains API 30 for the initial version to retain the Android 11 service and overlay behavior. The build is for direct installation and is not a Google Play submission. The Play target warning is disabled explicitly in Gradle. Raising the target requires testing foreground services, overlay touches, notification permissions, and fullscreen behavior on every supported version.

```sh
./gradlew assembleDebug testDebugUnitTest lintDebug
python3 -m unittest discover -s scripts -p 'test_*.py'
```

The GitHub Actions workflow runs those checks and uploads a debug APK. CI APKs use disposable debug signing keys. They are build artifacts rather than a stable public update channel.

## Signing

Android requires updates to use the same signing key as the installed app. Normal debug builds use the developer's local Android debug keystore. Keep the keystore if you want to update installs from that computer.

For a distribution key, supply the following environment variables before building:

```text
BIKETUBE_KEYSTORE=/absolute/path/to/distribution.jks
BIKETUBE_STORE_PASSWORD=your_store_password
BIKETUBE_KEY_ALIAS=your_key_alias
BIKETUBE_KEY_PASSWORD=your_key_password
```

Run `./gradlew assembleRelease`. With the variables supplied, Gradle signs the release APK. Without them, the release APK is unsigned. Do not commit keys or passwords, and do not use the local prototype key for public releases.

Release signing and a public release channel have not been configured in this repository. A maintainer needs to create and retain a distribution key before offering APK updates to other people.

## Source layout

| File | Purpose |
| --- | --- |
| `MainActivity.java` | YouTube viewer, native search, and fullscreen controls |
| `LauncherActivity.java` | Home screen and original Peloton entry |
| `SettingsActivity.java` | Units, panel reset, permissions, and device information |
| `MetricsService.java` | Floating display and foreground notification |
| `SensorClient.java` | Read-only V1 Binder callback connection |
| `BikeMath.java` | Power scaling, speed model, unit conversion, and freshness rules |
| `scripts/device.py` | Installation, diagnosis, Home restoration, and removal |

Sensor registration runs on a worker thread. Callback data is published as an immutable reading. The display refreshes twice a second and marks readings stale after three seconds.

The client verifies the Binder interface descriptor before sending registration transaction 1. It unregisters through transaction 2 during cleanup. It decodes only the first six primitive fields of the callback parcel, and it does not call motor, calibration, fake data, firmware, or resistance methods.

The V1 decoder expects the following fields: cadence, power in centiwatts, stepper position, load cell value, current resistance, and target resistance. Only cadence, power, and current resistance are displayed. The latter three hardware fields are not acted upon.

The service receives calibration notifications but does not use them. Malformed frames clear the visible reading. Sensor-service binding death triggers a new binding after a short delay. Other unsupported-interface errors are shown rather than hidden behind generated data.

## Public repository contents

Only original app code, build files, public project notices, docs, and tests belong in the public repository. The `.gitignore` excludes the local inspection directory, prototype, SDK and Java downloads, build outputs, device state, logs, and keystores.

The Gradle wrapper JAR is included because it is a standard public build tool. Peloton APKs and decompiled output remain local and must not be added to release archives.
