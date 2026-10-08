package local.peloton.probe;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.widget.TextView;

/** Shared colors and touch targets for the tablet UI. */
final class Ui {
  static final int BG = 0xFF101722,
      CARD = 0xFF1B2533,
      TEXT = 0xFFF4F8FF,
      MUTED = 0xFF93A3B8,
      ACCENT = 0xFF76E2B9;

  static int dp(Context c, float v) {
    return Math.round(v * c.getResources().getDisplayMetrics().density);
  }

  static TextView text(Context c, String value, float size, int color) {
    TextView v = new TextView(c);
    v.setText(value);
    v.setTextSize(size);
    v.setTextColor(color);
    v.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
    return v;
  }

  static GradientDrawable background(Context c, int color, int radius) {
    GradientDrawable bg = new GradientDrawable();
    bg.setColor(color);
    bg.setCornerRadius(dp(c, radius));
    return bg;
  }

  static TextView button(Context c, String value, Runnable action) {
    TextView v = text(c, value, 14, TEXT);
    v.setGravity(Gravity.CENTER);
    v.setMinHeight(dp(c, 48));
    v.setMinWidth(dp(c, 72));
    v.setPadding(dp(c, 16), dp(c, 8), dp(c, 16), dp(c, 8));
    v.setBackground(background(c, CARD, 12));
    v.setClickable(true);
    v.setFocusable(true);
    v.setOnClickListener(view -> action.run());
    return v;
  }
}
