# Validation

## Scope

The viewer, V1 sensor connection, fullscreen exit, custom Home screen, APK update, and Home restoration have been exercised on Android 11 hardware. Public documentation contains no individual device identifiers, firmware dumps, account information, or screenshots from a personal device.

Compatibility is limited to the V1 sensor interface. Other tablet models and firmware require their own checks. Bike+, Tread, Row, Android 10, and other sensor interfaces are not supported. Audio quality, long rides, reboot recovery, and exact speed agreement with the original display have not been measured.

## Automated checks

The debug APK build, seven Java calculation tests, Android lint, and five Python setup tests passed locally. An extracted source archive also built successfully. GitHub Actions runs the same checks on pushes and pull requests.

The tests cover speed conversion, stale readings, bounds, device selection, and restoration failure behavior. They do not simulate the proprietary sensor service or certify compatibility with other tablets.

## Check an installation

First, run the setup script's doctor command and confirm that the sensor service is available. Install the app, open a video, and verify playback and fullscreen exit. Pedal at different cadences and confirm that power and cadence change. Change resistance using the bike's normal controls and check the display.

Then confirm that the dashboard can be dragged, collapsed, and hidden. Finally, run restore-home and verify that the original Home opens before choosing BikeTube as Home again.

When reporting a problem, omit serial numbers, account names, email addresses, cookies, authorization tokens, and personal screenshots. Share only the minimum information needed to reproduce the issue.
