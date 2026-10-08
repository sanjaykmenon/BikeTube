package local.peloton.probe;

import android.app.Activity;
import android.content.*;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.widget.*;

public class SettingsActivity extends Activity {
  @Override
  public void onCreate(Bundle state) {
    super.onCreate(state);
    ScrollView scroll = new ScrollView(this);
    scroll.setFillViewport(true);
    scroll.setBackgroundColor(Ui.BG);
    LinearLayout root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(Ui.dp(this, 40), Ui.dp(this, 24), Ui.dp(this, 40), Ui.dp(this, 24));
    scroll.addView(root);
    root.addView(Ui.button(this, "Back", () -> finish()));
    add(root, "BikeTube settings", 30, Ui.TEXT);
    add(root, "Display", 18, Ui.ACCENT);
    Switch metric = new Switch(this);
    metric.setText(R.string.metric_units);
    metric.setTextColor(Ui.TEXT);
    metric.setMinHeight(Ui.dp(this, 56));
    metric.setChecked(getSharedPreferences("settings", 0).getBoolean("metric", false));
    metric.setOnCheckedChangeListener(
        (v, on) -> getSharedPreferences("settings", 0).edit().putBoolean("metric", on).apply());
    root.addView(metric);
    root.addView(
        Ui.button(
            this,
            "Reset metrics position",
            () -> {
              getSharedPreferences("panel", 0).edit().clear().apply();
              stopService(new Intent(this, MetricsService.class));
              if (Settings.canDrawOverlays(this)
                  && getSharedPreferences("settings", 0).getBoolean("metrics", true))
                startForegroundService(new Intent(this, MetricsService.class));
              Toast.makeText(this, "Metrics position reset", Toast.LENGTH_SHORT).show();
            }));
    add(root, "Access", 18, Ui.ACCENT);
    root.addView(
        Ui.button(
            this,
            "Floating metrics permission",
            () ->
                startActivity(
                    new Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName())))));
    root.addView(
        Ui.button(
            this,
            "Choose Home app",
            () -> {
              try {
                startActivity(new Intent(Settings.ACTION_HOME_SETTINGS));
              } catch (ActivityNotFoundException e) {
                Toast.makeText(
                        this, "Use the setup script to choose or restore Home", Toast.LENGTH_LONG)
                    .show();
              }
            }));
    add(root, "Device", 18, Ui.ACCENT);
    boolean service;
    try {
      getPackageManager().getPackageInfo("com.onepeloton.affernetservice", 0);
      service = true;
    } catch (Exception e) {
      service = false;
    }
    String version = "";
    try {
      version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
    } catch (Exception ignored) {
    }
    add(
        root,
        "BikeTube "
            + version
            + "\nModel: "
            + Build.MODEL
            + "\nAndroid: "
            + Build.VERSION.RELEASE
            + "\nSensor service: "
            + (service ? "installed" : "not found")
            + "\nFloating metrics: "
            + (Settings.canDrawOverlays(this) ? "allowed" : "permission needed"),
        15,
        Ui.TEXT);
    add(root, "Compatibility", 18, Ui.ACCENT);
    add(
        root,
        "Live metrics have been tested on PLTN-RB1VQ with Android 11. Other models and firmware"
            + " versions are unverified. Speed is an estimate calculated from power. BikeTube does"
            + " not change resistance or calibration.",
        15,
        Ui.MUTED);
    add(root, "Credits", 18, Ui.ACCENT);
    add(
        root,
        "The speed model comes from PeloMon and OpenRide. Grupetto demonstrated the floating"
            + " metrics approach. BikeTube loads YouTube's website. BikeTube is independent of"
            + " Peloton and YouTube.",
        15,
        Ui.MUTED);
    setContentView(scroll);
  }

  private void add(LinearLayout root, String value, int size, int color) {
    TextView v = Ui.text(this, value, size, color);
    v.setPadding(0, Ui.dp(this, 20), 0, Ui.dp(this, 12));
    root.addView(v);
  }
}
