package local.peloton.probe;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.*;
import android.view.inputmethod.*;
import android.webkit.*;
import android.widget.*;

/** YouTube website viewer with native search and an independent fullscreen escape control. */
public class MainActivity extends Activity {
  private WebView web;
  private FrameLayout container;
  private View fullVideo;
  private WebChromeClient.CustomViewCallback fullCallback;
  private TextView metricsButton, error;
  private ProgressBar progress;
  private EditText query;
  private final android.content.SharedPreferences.OnSharedPreferenceChangeListener
      preferencesListener =
          (prefs, key) -> {
            if ("metrics".equals(key)) runOnUiThread(() -> updateMetricsButton());
          };

  @Override
  public void onCreate(Bundle state) {
    super.onCreate(state);
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    container = new FrameLayout(this);
    LinearLayout column = new LinearLayout(this);
    column.setOrientation(LinearLayout.VERTICAL);
    column.setFocusableInTouchMode(true);
    column.setBackgroundColor(Ui.BG);
    LinearLayout toolbar = new LinearLayout(this);
    toolbar.setGravity(Gravity.CENTER_VERTICAL);
    toolbar.setPadding(Ui.dp(this, 16), Ui.dp(this, 8), Ui.dp(this, 16), Ui.dp(this, 8));
    TextView brand = Ui.text(this, "BikeTube", 22, Ui.ACCENT);
    toolbar.addView(brand, new LinearLayout.LayoutParams(0, -2, 1));
    toolbar.addView(Ui.button(this, "Back", () -> goBack()));
    toolbar.addView(Ui.button(this, "YouTube", () -> web.loadUrl("https://m.youtube.com")));
    toolbar.addView(Ui.button(this, "Reload", () -> web.reload()));
    metricsButton = Ui.button(this, "Metrics", () -> toggleMetrics());
    toolbar.addView(metricsButton);
    toolbar.addView(
        Ui.button(this, "Settings", () -> startActivity(new Intent(this, SettingsActivity.class))));
    toolbar.addView(
        Ui.button(this, "Launcher", () -> startActivity(new Intent(this, LauncherActivity.class))));
    column.addView(toolbar);
    LinearLayout searchBar = new LinearLayout(this);
    searchBar.setGravity(Gravity.CENTER_VERTICAL);
    searchBar.setPadding(Ui.dp(this, 16), 0, Ui.dp(this, 16), Ui.dp(this, 8));
    query = new EditText(this);
    query.setHint("Search YouTube");
    query.setSingleLine(true);
    query.setTextSize(17);
    query.setTextColor(Ui.TEXT);
    query.setHintTextColor(Ui.MUTED);
    query.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
    query.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
    query.setMinHeight(Ui.dp(this, 48));
    searchBar.addView(query, new LinearLayout.LayoutParams(0, -2, 1));
    searchBar.addView(Ui.button(this, "Search", () -> search()));
    column.addView(searchBar);
    query.setOnEditorActionListener(
        (v, action, event) -> {
          if (action == EditorInfo.IME_ACTION_SEARCH
              || (event != null
                  && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                  && event.getAction() == KeyEvent.ACTION_UP)) {
            search();
            return true;
          }
          return false;
        });
    query.setOnClickListener(
        v -> {
          query.requestFocus();
          getSystemService(InputMethodManager.class)
              .showSoftInput(query, InputMethodManager.SHOW_IMPLICIT);
        });
    progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
    progress.setMax(100);
    column.addView(progress, new LinearLayout.LayoutParams(-1, Ui.dp(this, 2)));
    error = Ui.text(this, "", 14, 0xFFFFC278);
    error.setPadding(Ui.dp(this, 16), Ui.dp(this, 8), Ui.dp(this, 16), Ui.dp(this, 8));
    error.setVisibility(View.GONE);
    column.addView(error);
    web = new WebView(this);
    web.setBackgroundColor(Color.BLACK);
    WebSettings settings = web.getSettings();
    settings.setJavaScriptEnabled(true);
    settings.setDomStorageEnabled(true);
    settings.setMediaPlaybackRequiresUserGesture(true);
    settings.setAllowFileAccess(false);
    settings.setAllowContentAccess(false);
    settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
    web.setWebViewClient(
        new WebViewClient() {
          @Override
          public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            if ("https".equals(request.getUrl().getScheme())) return false;
            error.setText(R.string.app_link_error);
            error.setVisibility(View.VISIBLE);
            return true;
          }

          @Override
          public void onPageStarted(WebView v, String url, android.graphics.Bitmap icon) {
            error.setVisibility(View.GONE);
          }

          @Override
          public void onReceivedError(WebView v, WebResourceRequest req, WebResourceError issue) {
            if (req.isForMainFrame()) {
              error.setText(R.string.network_error);
              error.setVisibility(View.VISIBLE);
            }
          }

          @Override
          public void onReceivedHttpError(
              WebView v, WebResourceRequest req, WebResourceResponse response) {
            if (req.isForMainFrame() && response.getStatusCode() >= 400) {
              error.setText(R.string.http_error);
              error.setVisibility(View.VISIBLE);
            }
          }
        });
    web.setWebChromeClient(
        new WebChromeClient() {
          @Override
          public void onProgressChanged(WebView v, int value) {
            progress.setProgress(value);
            progress.setVisibility(value == 100 ? View.INVISIBLE : View.VISIBLE);
          }

          @Override
          public void onShowCustomView(View view, CustomViewCallback cb) {
            if (fullVideo != null) {
              cb.onCustomViewHidden();
              return;
            }
            fullVideo = view;
            fullCallback = cb;
            FrameLayout full = new FrameLayout(MainActivity.this);
            full.setBackgroundColor(Color.BLACK);
            full.addView(view, new FrameLayout.LayoutParams(-1, -1));
            TextView exit = Ui.button(MainActivity.this, "Exit fullscreen", () -> exitFull());
            FrameLayout.LayoutParams button =
                new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.END);
            button.setMargins(0, Ui.dp(MainActivity.this, 12), Ui.dp(MainActivity.this, 12), 0);
            full.addView(exit, button);
            container.addView(full, new FrameLayout.LayoutParams(-1, -1));
            getWindow()
                .getDecorView()
                .setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
          }

          @Override
          public void onHideCustomView() {
            exitFull();
          }
        });
    column.addView(web, new LinearLayout.LayoutParams(-1, 0, 1));
    container.addView(column, new FrameLayout.LayoutParams(-1, -1));
    setContentView(container);
    if (state == null || web.restoreState(state) == null) web.loadUrl("https://m.youtube.com");
    consumeIntent(getIntent());
  }

  private void search() {
    String value = query.getText().toString().trim();
    if (value.isEmpty()) return;
    web.loadUrl("https://m.youtube.com/results?search_query=" + Uri.encode(value));
    getSystemService(InputMethodManager.class).hideSoftInputFromWindow(query.getWindowToken(), 0);
    web.requestFocus();
  }

  private void toggleMetrics() {
    boolean enabled = getSharedPreferences("settings", 0).getBoolean("metrics", true);
    if (enabled) {
      getSharedPreferences("settings", 0).edit().putBoolean("metrics", false).apply();
      stopService(new Intent(this, MetricsService.class));
    } else if (Settings.canDrawOverlays(this)) {
      getSharedPreferences("settings", 0).edit().putBoolean("metrics", true).apply();
      startForegroundService(new Intent(this, MetricsService.class));
    } else
      new android.app.AlertDialog.Builder(this)
          .setTitle("Allow floating metrics")
          .setMessage("Enable display over other apps for BikeTube, then return here.")
          .setPositiveButton(
              "Open settings",
              (d, w) -> {
                getSharedPreferences("settings", 0).edit().putBoolean("metrics", true).apply();
                startActivity(
                    new Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName())));
              })
          .setNegativeButton("Later", null)
          .show();
    updateMetricsButton();
  }

  private void updateMetricsButton() {
    metricsButton.setText(
        getSharedPreferences("settings", 0).getBoolean("metrics", true)
                && Settings.canDrawOverlays(this)
            ? "Hide metrics"
            : "Show metrics");
  }

  @Override
  protected void onResume() {
    super.onResume();
    web.onResume();
    getSharedPreferences("settings", 0)
        .registerOnSharedPreferenceChangeListener(preferencesListener);
    if (Settings.canDrawOverlays(this)
        && getSharedPreferences("settings", 0).getBoolean("metrics", true))
      startForegroundService(new Intent(this, MetricsService.class));
    updateMetricsButton();
  }

  @Override
  protected void onPause() {
    getSharedPreferences("settings", 0)
        .unregisterOnSharedPreferenceChangeListener(preferencesListener);
    web.onPause();
    super.onPause();
  }

  @Override
  protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    setIntent(intent);
    consumeIntent(intent);
  }

  private void consumeIntent(Intent intent) {
    if (intent.getBooleanExtra("exit_fullscreen", false)) {
      intent.removeExtra("exit_fullscreen");
      exitFull();
    }
  }

  private void exitFull() {
    if (fullVideo == null) return;
    View parent = (View) fullVideo.getParent();
    WebChromeClient.CustomViewCallback callback = fullCallback;
    fullVideo = null;
    fullCallback = null;
    container.removeView(parent);
    getWindow().getDecorView().setSystemUiVisibility(0);
    if (callback != null) callback.onCustomViewHidden();
  }

  private void goBack() {
    if (fullVideo != null) exitFull();
    else if (web.canGoBack()) web.goBack();
    else startActivity(new Intent(this, LauncherActivity.class));
  }

  @Override
  public void onBackPressed() {
    goBack();
  }

  @Override
  protected void onSaveInstanceState(Bundle state) {
    web.saveState(state);
    super.onSaveInstanceState(state);
  }

  @Override
  protected void onDestroy() {
    exitFull();
    container.removeAllViews();
    web.destroy();
    super.onDestroy();
  }
}
