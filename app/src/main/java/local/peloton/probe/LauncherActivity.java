package local.peloton.probe;

import android.app.Activity;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.*;

public class LauncherActivity extends Activity {
  private int dp(int n) {
    return Math.round(n * getResources().getDisplayMetrics().density);
  }

  private TextView text(String s, int size, int color) {
    TextView v = new TextView(this);
    v.setText(s);
    v.setTextSize(size);
    v.setTextColor(color);
    return v;
  }

  @Override
  public void onCreate(Bundle state) {
    super.onCreate(state);
    LinearLayout root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setGravity(Gravity.CENTER);
    root.setPadding(dp(64), dp(32), dp(64), dp(32));
    root.setBackgroundColor(0xFF101722);
    TextView title = text("Your bike", 38, Color.WHITE);
    title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
    root.addView(title);
    TextView subtitle = text("Choose an app to get started", 17, 0xFF93A3B8);
    subtitle.setPadding(0, dp(10), 0, dp(32));
    root.addView(subtitle);
    LinearLayout row = new LinearLayout(this);
    row.setGravity(Gravity.CENTER);
    card(
        row,
        "BikeTube",
        "YouTube + live cycling metrics",
        0xFF76E2B9,
        () -> startActivity(new Intent(this, MainActivity.class)));
    card(row, "Peloton", "Open the original Peloton screen", 0xFFF4F8FF, () -> openPeloton());
    root.addView(row);
    TextView note = text("Press the Peloton Home button to return here", 14, 0xFF93A3B8);
    note.setPadding(0, dp(32), 0, 0);
    root.addView(note);
    root.addView(
        Ui.button(this, "Settings", () -> startActivity(new Intent(this, SettingsActivity.class))));
    setContentView(root);
  }

  private void card(LinearLayout row, String title, String subtitle, int accent, Runnable click) {
    LinearLayout card = new LinearLayout(this);
    card.setOrientation(LinearLayout.VERTICAL);
    card.setGravity(Gravity.CENTER_VERTICAL);
    card.setPadding(dp(28), dp(28), dp(28), dp(28));
    GradientDrawable bg = new GradientDrawable();
    bg.setColor(0xFF1B2533);
    bg.setCornerRadius(dp(20));
    bg.setStroke(dp(1), 0xFF354253);
    card.setBackground(bg);
    card.addView(text(title, 30, accent));
    TextView sub = text(subtitle, 15, 0xFFBAC6D5);
    sub.setPadding(0, dp(12), 0, 0);
    card.addView(sub);
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(350), dp(175));
    lp.setMargins(dp(12), 0, dp(12), 0);
    row.addView(card, lp);
    card.setClickable(true);
    card.setFocusable(true);
    card.setOnClickListener(v -> click.run());
    card.setContentDescription("Open " + title);
  }

  private void openPeloton() {
    try {
      stopService(new Intent(this, MetricsService.class));
      startActivity(
          new Intent()
              .setComponent(
                  new ComponentName(
                      "com.peloton.launcher", "com.peloton.launcher.LauncherActivity")));
    } catch (ActivityNotFoundException e) {
      Toast.makeText(this, "Peloton Home was not found on this device", Toast.LENGTH_LONG).show();
    }
  }

  @Override
  public void onBackPressed() {}
}
