package local.peloton.probe;

import android.content.Context;
import android.graphics.*;
import android.view.View;
import java.util.Locale;

/** A quiet instrument dial. Its scale bounds the needle, never the numeric reading. */
public final class GaugeView extends View {
  private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final String title, unit;
  private final float maximum;
  private final int accent;
  private float value, displayed;
  private boolean live;
  private long lastFrame;

  public GaugeView(Context context, String title, String unit, float maximum, int accent) {
    super(context);
    this.title = title;
    this.unit = unit;
    this.maximum = maximum;
    this.accent = accent;
    setLayerType(View.LAYER_TYPE_SOFTWARE, null);
  }

  public void reading(float number, boolean fresh) {
    value = Math.max(0, number);
    live = fresh;
    if (!fresh) displayed = 0;
    setContentDescription(title + ": " + (fresh ? Math.round(value) + " " + unit : "No signal"));
    invalidate();
  }

  private void text(Canvas c, String s, float x, float y, float size, int color) {
    paint.setStyle(Paint.Style.FILL);
    paint.setColor(color);
    paint.setTextAlign(Paint.Align.CENTER);
    paint.setTextSize(size);
    paint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
    c.drawText(s, x, y, paint);
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    long now = android.os.SystemClock.elapsedRealtime();
    float elapsed = lastFrame == 0 ? .016f : Math.min(.1f, (now - lastFrame) / 1000f);
    lastFrame = now;
    float target = live ? Math.min(value, maximum) : 0;
    displayed += (target - displayed) * (1 - (float) Math.exp(-elapsed * 8));
    float scale = Math.min(getWidth() / 180f, getHeight() / 110f);
    canvas.save();
    canvas.translate((getWidth() - 180 * scale) / 2, (getHeight() - 110 * scale) / 2);
    canvas.scale(scale, scale);
    float cx = 90, cy = 63, radius = 53;
    RectF dial = new RectF(cx - radius, cy - radius, cx + radius, cy + radius);
    paint.setStyle(Paint.Style.STROKE);
    paint.setStrokeCap(Paint.Cap.ROUND);
    paint.setStrokeWidth(2);
    paint.setColor(0xFF2C3948);
    canvas.drawArc(dial, 180, 180, false, paint);
    paint.setColor(live ? accent : 0xFF526173);
    canvas.drawArc(dial, 180, 180 * displayed / maximum, false, paint);
    for (int i = 0; i <= 20; i++) {
      double a = Math.toRadians(180 + i * 9);
      float outer = 46, inner = i % 5 == 0 ? 39 : 43;
      paint.setColor(i % 5 == 0 ? 0xFF8B9BAD : 0xFF3C4B5D);
      paint.setStrokeWidth(i % 5 == 0 ? 1.5f : 1);
      canvas.drawLine(
          cx + (float) Math.cos(a) * inner,
          cy + (float) Math.sin(a) * inner,
          cx + (float) Math.cos(a) * outer,
          cy + (float) Math.sin(a) * outer,
          paint);
    }
    double angle = Math.toRadians(180 + 180 * displayed / maximum);
    paint.setStrokeWidth(2);
    paint.setColor(live ? accent : 0xFF526173);
    canvas.drawLine(
        cx, cy, cx + (float) Math.cos(angle) * 34, cy + (float) Math.sin(angle) * 34, paint);
    paint.setStyle(Paint.Style.FILL);
    paint.setColor(live ? accent : 0xFF526173);
    canvas.drawCircle(cx, cy, 2.5f, paint);
    text(canvas, live ? String.format(Locale.US, "%.0f", value) : "—", cx, 86, 25, 0xFFF4F8FF);
    text(canvas, title + "  " + unit, cx, 105, 10, 0xFFBBC8D8);
    canvas.restore();
    if (Math.abs(target - displayed) > .05f) postInvalidateOnAnimation();
  }
}
