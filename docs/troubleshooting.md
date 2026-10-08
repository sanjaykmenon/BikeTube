# Troubleshooting

| Problem | Action |
| --- | --- |
| No USB device | Use a data cable, check the tablet port, and reconnect. |
| ADB reports unauthorized | Accept the USB debugging authorization prompt on the bike. |
| More than one device | Disconnect the others or pass `--serial SERIAL` before the command. |
| ADB not found | Install Platform Tools or pass `--adb /path/to/adb`. |
| APK not found | Build with Gradle first or pass `--apk FILE`. |
| App will not install | Read the reported Android installation error. No subscription workaround is assumed. |
| Different signing key | Use the key from the previous build. If starting over, restore Home before uninstalling. |
| Metrics do not appear | Allow display over other apps, then tap Show metrics. |
| Metrics show a dash | Open Settings to check the device and sensor service. Pedal briefly. Unsupported firmware can bind without providing frames. |
| Search keyboard does not open | Use BikeTube's native search box above the website. Move the metrics panel away from that box. |
| Fullscreen is difficult to exit | Tap Exit fullscreen in the video view or in the metrics panel. Android Back also exits the video view. |
| Panel is off screen | Open Settings and tap Reset metrics position. |
| YouTube does not load | Check Wi-Fi and tap Reload. The device's Android WebView must support the website. |
| Google sign-in is refused | Embedded website sign-in can be restricted by Google. YouTube playback can be used without signing in. |
| Home button returns to Peloton | Run `install --launcher` again and check that the Home change succeeds. |

For a compatibility report, include the tablet model, Android version, build, sensor service version, and what you tested. Omit account details, USB serial numbers, cookies, tokens, and extracted Peloton software.

`python3 scripts/device.py doctor` prints a short device report. The script does not upload it. Read it before sharing.
