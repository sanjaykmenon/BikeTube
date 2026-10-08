package local.peloton.probe;

import android.content.Context;
import android.widget.TextView;

/** Drag listener invokes performClick on release, preserving Android accessibility semantics. */
final class DragHandle extends TextView {
  DragHandle(Context context) {
    super(context);
    setFocusable(true);
    setClickable(true);
  }

  @Override
  public boolean performClick() {
    super.performClick();
    return true;
  }
}
