# Setup

## Prepare the computer

Install [Android Studio](https://developer.android.com/studio) and Python 3. In Android Studio's SDK Manager, install SDK Platform 34, Build Tools 34.0.0, and Platform Tools. Use JDK 17 for the Gradle build. JDK 17 can be selected in Android Studio's Gradle settings or supplied through `JAVA_HOME` for terminal builds.

Open the repository root in Android Studio. Android Studio creates `local.properties` with the SDK location. For a terminal-only setup, create that file yourself with `sdk.dir=/path/to/Android/sdk`. The SDK path is local to your computer and is not included in the repository.

Run `./gradlew assembleDebug` on macOS or Linux, or `.\gradlew.bat assembleDebug` on Windows. The APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

## Connect the bike

Use a cable that supports data, connected to the tablet's USB port. A charging-only cable will not provide debugging access.

Enable Developer options by tapping Build number seven times in Android Settings, About tablet. Enable USB debugging in Developer options. Accept the authorization prompt on the bike when connecting the computer.

The location of Android Settings varies by Peloton software version. If Developer options are unavailable, stop and report your model and build. The installer does not reset the device or change its bootloader.

Run the following command from the repository root:

```sh
python3 scripts/device.py doctor
```

On Windows, use `python` instead of `python3`. Pass `--adb /path/to/adb` before `doctor` if Platform Tools is not on PATH. Android's [ADB documentation](https://developer.android.com/tools/adb) describes USB debugging and authorization.

## Install

```sh
python3 scripts/device.py install --launcher
```

The command installs the APK, grants floating display permission, saves the existing Home component locally, and opens the BikeTube launcher. Choose BikeTube, search for a video, then pedal to check cadence and power. Confirm sound, fullscreen exit, and the metrics panel.

To keep the current Home app, run `install` without `--launcher`. To install an APK from another location, add `--apk /path/to/biketube.apk` after `install`.

By default, setup accepts only the RB1VQ tablet model family and requires Android API 30 or newer. `--allow-untested` permits an explicit test of another model that meets those prerequisites. It does not add support for another sensor interface.

## Return to Peloton

The launcher includes a Peloton button. The original Peloton Home component used by the supported launcher integration is `com.peloton.launcher/.LauncherActivity`.

To restore the original Home app permanently:

```sh
python3 scripts/device.py restore-home
```

The installer records Home by device in `.biketube-state.json`. Preserve that file on the installation computer. A migrated prototype without that file can use the Peloton launcher fallback if the activity still exists.

To remove the app:

```sh
python3 scripts/device.py uninstall
```

Setup restores Home before removing BikeTube when BikeTube is the current Home app. If restoration fails, it leaves BikeTube installed.
