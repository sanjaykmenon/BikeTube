# Privacy

BikeTube has no developer server, analytics, or crash reporting service. Sensor readings stay on the tablet and are not uploaded by the app. The app stores display preferences locally and does not save ride history.

The YouTube viewer loads YouTube's website through Android WebView. YouTube and its content providers receive the website requests and may store website cookies. Any sign-in takes place in that website. BikeTube has no JavaScript bridge that reads the page or extracts credentials.

The app requests Internet access, display over other apps, and permission to run a foreground service. The sensor client reads an existing on-device Peloton service. BikeTube does not request camera, microphone, contacts, or location access.

The setup script stores the USB device identifier and original Home component in `.biketube-state.json` on the installation computer. The file is excluded from Git. The script does not upload it.

Uninstalling BikeTube removes its preferences and WebView data. Restore the original Home app before removing BikeTube if it is your current launcher.
