package local.peloton.probe;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.*;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.view.*;
import android.view.WindowManager;
import android.widget.*;
import android.widget.TextView;
import java.util.Locale;

/** Reads only the verified V1 callback's first six primitive fields. No hardware commands. */
public class MetricsService extends Service {
  private final Handler handler = new Handler(Looper.getMainLooper());
  private SensorClient sensors;
  private WindowManager wm;
  private LinearLayout panel;
  private WindowManager.LayoutParams layout;
  private TextView speedUnits;
  private TextView speedValue, liveLabel, toggle;
  private LinearLayout detailRow;
  private GaugeView powerGauge, cadenceGauge, resistanceGauge;
  private boolean compact;
  private final Runnable refresh =
      new Runnable() {
        @Override
        public void run() {
          SensorClient.Reading r = sensors.reading();
          boolean live = r != null && BikeMath.fresh(SystemClock.elapsedRealtime(), r.time);
          boolean metric = getSharedPreferences("settings", 0).getBoolean("metric", false);
          speedUnits.setText(metric ? "km/h" : "mph");
          liveLabel.setText(live ? "LIVE" : "CONNECTING");
          liveLabel.setTextColor(live ? 0xFF76E2B9 : 0xFFFFC278);
          if (!live && r != null) liveLabel.setText(R.string.no_signal);
          if (!live) {
            String status = sensors.status();
            if (status.contains("unavailable")
                || status.contains("Unsupported")
                || status.contains("error")) liveLabel.setText(R.string.unavailable);
            liveLabel.setContentDescription(status);
          }
          speedValue.setText(
              live ? String.format(Locale.US, "%.1f", BikeMath.displaySpeed(r.mph, metric)) : "—");
          powerGauge.reading(live ? (float) r.watts : 0, live);
          cadenceGauge.reading(live ? r.rpm : 0, live);
          resistanceGauge.reading(live ? r.resistance : 0, live);
          handler.postDelayed(this, 500);
        }
      };

  @Override
  public void onCreate() {
    super.onCreate();
    NotificationManager nm = getSystemService(NotificationManager.class);
    nm.createNotificationChannel(
        new NotificationChannel("metrics", "Bike metrics", NotificationManager.IMPORTANCE_LOW));
    PendingIntent open =
        PendingIntent.getActivity(
            this,
            0,
            new Intent(this, MainActivity.class),
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    startForeground(
        1,
        new Notification.Builder(this, "metrics")
            .setContentTitle("Bike metrics running")
            .setContentText("Open BikeTube to watch YouTube")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(open)
            .build());
    sensors = new SensorClient(this);
    wm = getSystemService(WindowManager.class);
    panel = new LinearLayout(this);
    panel.setOrientation(LinearLayout.VERTICAL);
    panel.setPadding(dp(12), dp(0), dp(12), dp(4));
    GradientDrawable background = new GradientDrawable();
    background.setColor(0xFF141D29);
    background.setCornerRadius(dp(18));
    background.setStroke(dp(1), 0xFF354253);
    panel.setBackground(background);
    panel.setElevation(dp(10));
    LinearLayout header = new LinearLayout(this);
    header.setGravity(Gravity.CENTER_VERTICAL);
    TextView grip = new DragHandle(this);
    grip.setText("BIKE  ··");
    grip.setTextSize(12);
    grip.setTextColor(0xFFBAC6D5);
    grip.setPadding(0, dp(6), 0, dp(6));
    grip.setContentDescription("Drag bike metrics to move");
    grip.setMinHeight(dp(48));
    header.addView(grip, new LinearLayout.LayoutParams(0, -2, 1));
    liveLabel = label("CONNECTING", 10, 0xFFFFC278);
    header.addView(liveLabel);
    TextView close = label("×", 24, 0xFFBAC6D5);
    close.setGravity(Gravity.CENTER);
    close.setMinWidth(dp(48));
    close.setMinHeight(dp(48));
    close.setContentDescription("Hide metrics");
    close.setOnClickListener(
        v -> {
          getSharedPreferences("settings", 0).edit().putBoolean("metrics", false).apply();
          stopSelf();
        });
    header.addView(close, new LinearLayout.LayoutParams(dp(48), dp(48)));
    panel.addView(header);
    speedValue = label("—", 20, 0xFFF4F8FF);
    speedValue.setPadding(dp(20), 0, dp(6), 0);
    header.addView(speedValue);
    speedUnits = label("mph", 12, 0xFF93A3B8);
    speedUnits.setPadding(0, 0, dp(12), 0);
    header.addView(speedUnits);
    detailRow = new LinearLayout(this);
    detailRow.setGravity(Gravity.CENTER);
    powerGauge = new GaugeView(this, "POWER", "WATTS", 500, 0xFF76E2B9);
    cadenceGauge = new GaugeView(this, "CADENCE", "RPM", 160, 0xFF73C9FF);
    resistanceGauge = new GaugeView(this, "RESISTANCE", "/ 100", 100, 0xFFC1ABFF);
    detailRow.addView(powerGauge, new LinearLayout.LayoutParams(0, dp(90), 1));
    detailRow.addView(cadenceGauge, new LinearLayout.LayoutParams(0, dp(90), 1));
    detailRow.addView(resistanceGauge, new LinearLayout.LayoutParams(0, dp(90), 1));
    panel.addView(detailRow);
    toggle = label("Collapse", 11, 0xFFBAC6D5);
    toggle.setPadding(dp(12), 0, dp(8), 0);
    toggle.setMinHeight(dp(48));
    toggle.setGravity(Gravity.CENTER);
    toggle.setOnClickListener(
        v -> {
          compact = !compact;
          applyCompact();
          getSharedPreferences("panel", 0).edit().putBoolean("compact", compact).apply();
        });
    header.addView(toggle);
    compact = getSharedPreferences("panel", 0).getBoolean("compact", false);
    applyCompact();
    layout =
        new WindowManager.LayoutParams(
            Math.min(dp(540), getResources().getDisplayMetrics().widthPixels - dp(32)),
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT);
    layout.gravity = Gravity.TOP | Gravity.START;
    boolean dashboard = getSharedPreferences("panel", 0).getBoolean("slimDashboard", false);
    if (!dashboard) {
      getSharedPreferences("panel", 0)
          .edit()
          .remove("x")
          .remove("y")
          .putBoolean("compact", false)
          .putBoolean("slimDashboard", true)
          .apply();
      compact = false;
      applyCompact();
    }
    layout.x =
        getSharedPreferences("panel", 0)
            .getInt("x", (getResources().getDisplayMetrics().widthPixels - layout.width) / 2);
    layout.y =
        getSharedPreferences("panel", 0)
            .getInt("y", getResources().getDisplayMetrics().heightPixels - dp(350));
    grip.setOnTouchListener(
        new View.OnTouchListener() {
          float x, y;
          int ox, oy;

          public boolean onTouch(View v, MotionEvent e) {
            if (e.getAction() == MotionEvent.ACTION_DOWN) {
              x = e.getRawX();
              y = e.getRawY();
              ox = layout.x;
              oy = layout.y;
              return true;
            }
            if (e.getAction() == MotionEvent.ACTION_MOVE) {
              layout.x =
                  Math.max(
                      0,
                      Math.min(
                          getResources().getDisplayMetrics().widthPixels - panel.getWidth(),
                          ox + (int) (e.getRawX() - x)));
              layout.y =
                  Math.max(
                      0,
                      Math.min(
                          getResources().getDisplayMetrics().heightPixels
                              - panel.getHeight()
                              - dp(48),
                          oy + (int) (e.getRawY() - y)));
              wm.updateViewLayout(panel, layout);
              return true;
            }
            if (e.getAction() == MotionEvent.ACTION_UP) {
              getSharedPreferences("panel", 0)
                  .edit()
                  .putInt("x", layout.x)
                  .putInt("y", layout.y)
                  .apply();
              v.performClick();
            }
            return true;
          }
        });
    try {
      wm.addView(panel, layout);
      panel.post(
          () -> {
            if (!getSharedPreferences("panel", 0).contains("y"))
              layout.y =
                  getResources().getDisplayMetrics().heightPixels - panel.getHeight() - dp(48);
            clampPosition();
          });
    } catch (Exception e) {
      stopSelf();
      return;
    }
    sensors.start();
    handler.post(refresh);
  }

  private int dp(float n) {
    return Math.round(n * getResources().getDisplayMetrics().density);
  }

  private TextView label(String text, float size, int color) {
    TextView v = new TextView(this);
    v.setText(text);
    v.setTextSize(size);
    v.setTextColor(color);
    v.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
    return v;
  }

  private TextView metricCell(LinearLayout parent, String name, String unit) {
    LinearLayout cell = new LinearLayout(this);
    cell.setOrientation(LinearLayout.VERTICAL);
    TextView title = label(name, 9, 0xFF93A3B8);
    title.setLetterSpacing(0.04f);
    cell.addView(title);
    TextView value = label("—", 23, 0xFFF4F8FF);
    value.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
    cell.addView(value);
    cell.addView(label(unit, 11, 0xFF93A3B8));
    parent.addView(cell, new LinearLayout.LayoutParams(0, -2, 1));
    return value;
  }

  private void applyCompact() {
    detailRow.setVisibility(compact ? View.GONE : View.VISIBLE);
    toggle.setText(compact ? "Gauges" : "Collapse");
    if (layout != null) panel.post(() -> clampPosition());
  }

  private void clampPosition() {
    if (panel.getParent() == null) return;
    layout.x =
        BikeMath.clamp(layout.x, getResources().getDisplayMetrics().widthPixels - panel.getWidth());
    layout.y =
        BikeMath.clamp(
            layout.y, getResources().getDisplayMetrics().heightPixels - panel.getHeight() - dp(48));
    wm.updateViewLayout(panel, layout);
  }

  @Override
  public void onConfigurationChanged(android.content.res.Configuration config) {
    super.onConfigurationChanged(config);
    panel.post(() -> clampPosition());
  }

  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int id) {
    return START_NOT_STICKY;
  }

  @Override
  public void onDestroy() {
    handler.removeCallbacks(refresh);
    if (sensors != null) sensors.close();
    if (panel != null && panel.getParent() != null) wm.removeView(panel);
    super.onDestroy();
  }
}
