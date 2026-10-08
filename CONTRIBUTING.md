# Contributing

Run the build, unit tests, Python tests, and Android lint before proposing a change. Test video playback, native typing, fullscreen entry and exit, panel dragging, compact mode, and Home restoration on hardware when the change affects those flows.

Keep sensor access read-only. Do not add resistance, calibration, fake data, bootloader, or firmware commands to the display app. Describe the hardware and firmware used for any new compatibility claim.

Include a short report of what changed and how it was checked. An emulator can verify the viewer and launcher, but it cannot prove live Peloton sensor compatibility.

Do not include extracted Peloton code, firmware, signing keys, USB serials, or account data in contributions. Preserve third-party attribution in NOTICE.
