package cn.toside.music.mobile.tv;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.FocusFinder;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.HorizontalScrollView;
import android.widget.SeekBar;
import com.facebook.react.views.view.ReactViewGroup;
import com.facebook.react.views.textinput.ReactEditText;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.uimanager.util.ReactFindViewUtil;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

/** Native D-pad focus and highlighting, shared by screens and RN dialog windows.
 * Clicks and media keys retain their original Android/RN event path.
 */
public class TVFocusRoot extends ReactViewGroup {
  String backFocusID;
  private final Paint outline = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint shade = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Rect rect = new Rect();
  private final float density;
  private WeakReference<View> lastFocus = new WeakReference<>(null);
  private String lastNativeID;
  private boolean focusPosted;
  private boolean scrolling;
  private boolean needsInitialFocus = true;

  private final ViewTreeObserver.OnGlobalFocusChangeListener focusListener = (oldView, newView) -> {
    if (contains(newView) && isControl(newView)) {
      lastFocus = new WeakReference<>(newView);
      Object id = newView.getTag(com.facebook.react.R.id.view_tag_native_id);
      lastNativeID = id instanceof String ? (String) id : null;
    }
    invalidate();
  };
  private final ViewTreeObserver.OnGlobalLayoutListener layoutListener = this::scheduleFocus;
  private final ViewTreeObserver.OnScrollChangedListener scrollListener = this::invalidate;

  public TVFocusRoot(Context context) {
    super(context);
    density = getResources().getDisplayMetrics().density;
    setFocusable(false);
    setDescendantFocusability(FOCUS_AFTER_DESCENDANTS);
    outline.setColor(Color.rgb(255, 202, 74));
    outline.setStyle(Paint.Style.STROKE);
    outline.setStrokeWidth(3 * density);
    shade.setColor(Color.argb(28, 255, 202, 74));
  }

  @Override protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    needsInitialFocus = true;
    getViewTreeObserver().addOnGlobalFocusChangeListener(focusListener);
    getViewTreeObserver().addOnGlobalLayoutListener(layoutListener);
    getViewTreeObserver().addOnScrollChangedListener(scrollListener);
    scheduleFocus();
  }

  @Override protected void onDetachedFromWindow() {
    ViewTreeObserver observer = getViewTreeObserver();
    if (observer.isAlive()) {
      observer.removeOnGlobalFocusChangeListener(focusListener);
      observer.removeOnGlobalLayoutListener(layoutListener);
      observer.removeOnScrollChangedListener(scrollListener);
    }
    super.onDetachedFromWindow();
  }

  @Override public void onWindowFocusChanged(boolean focused) {
    super.onWindowFocusChanged(focused);
    if (focused) scheduleFocus();
  }

  private boolean contains(View view) {
    while (view != null) {
      if (view == this) return true;
      ViewParent parent = view.getParent();
      view = parent instanceof View ? (View) parent : null;
    }
    return false;
  }

  private boolean isControl(View view) {
    if (view == null) return false;
    Object accessibility = view.getTag(com.facebook.react.R.id.accessibility_state);
    if (accessibility instanceof ReadableMap) {
      ReadableMap state = (ReadableMap) accessibility;
      if (state.hasKey("disabled") && !state.isNull("disabled") && state.getBoolean("disabled")) return false;
    }
    for (View item = view; item != null && item != this;) {
      if ("tv-display-only".equals(item.getTag(com.facebook.react.R.id.view_tag_native_id))) return false;
      ViewParent parent = item.getParent();
      item = parent instanceof View ? (View) parent : null;
    }
    return view != null && view != this && view.isEnabled() && view.isShown()
        && view.isFocusable() && (view.isClickable() || view instanceof EditText
        || view instanceof SeekBar || view instanceof ScrollView || view instanceof HorizontalScrollView);
  }

  @Override public void addFocusables(ArrayList<View> views, int direction, int mode) {
    int start = views.size();
    super.addFocusables(views, direction, mode);
    for (int i = views.size() - 1; i >= start; i--) {
      if (!isControl(views.get(i))) views.remove(i);
    }
  }

  private void scheduleFocus() {
    if (focusPosted) return;
    focusPosted = true;
    post(() -> {
      focusPosted = false;
      if (!isAttachedToWindow() || !isShown() || !hasWindowFocus() || getWidth() == 0) return;
      View focused = getRootView().findFocus();
      // Another screen/overlay owns focus: do not steal it during background updates.
      if (!needsInitialFocus && isControl(focused)) return;
      if (contains(focused) && isControl(focused)) { needsInitialFocus = false; return; }
      View previous = lastFocus.get();
      if (contains(previous) && isControl(previous) && previous.requestFocus()) { needsInitialFocus = false; return; }
      if (lastNativeID != null) {
        View restored = ReactFindViewUtil.findView(this, lastNativeID);
        if (isControl(restored) && restored.requestFocus()) { needsInitialFocus = false; return; }
      }
      ArrayList<View> candidates = new ArrayList<>();
      addFocusables(candidates, FOCUS_FORWARD, FOCUSABLES_ALL);
      for (View candidate : candidates) {
        if (candidate.getGlobalVisibleRect(rect) && candidate.requestFocus()) { needsInitialFocus = false; break; }
      }
    });
  }

  @Override protected void dispatchDraw(Canvas canvas) {
    super.dispatchDraw(canvas);
    View focused = findFocus();
    if (!hasWindowFocus() || !isControl(focused) || !focused.getGlobalVisibleRect(rect)) return;
    int[] origin = new int[2];
    getLocationOnScreen(origin);
    RectF bounds = new RectF(rect);
    bounds.offset(-origin[0], -origin[1]);
    // Inset the outline so it stays visible at clipped list/window edges.
    bounds.inset(2 * density, 2 * density);
    canvas.drawRoundRect(bounds, 7 * density, 7 * density, shade);
    canvas.drawRoundRect(bounds, 7 * density, 7 * density, outline);
  }

  private ScrollView verticalScrollParent(View focused) {
    ViewParent parent = focused.getParent();
    while (parent instanceof View && parent != this) {
      if (parent instanceof ScrollView) return (ScrollView) parent;
      parent = parent.getParent();
    }
    return null;
  }

  private boolean inside(View child, ViewGroup parent) {
    for (ViewParent p = child == null ? null : child.getParent(); p != null; p = p.getParent()) {
      if (p == parent) return true;
    }
    return false;
  }

  private boolean moveFocus(int direction) {
    View current = findFocus();
    if (current == null) { scheduleFocus(); return true; }
    // Let text editing and sliders handle their own keys, including the input method.
    if (current instanceof EditText || current instanceof SeekBar) return false;
    if (scrolling) return true;
    // Keep a focused text-only container scrollable (first-run agreement/help).
    // Only move to controls outside it once its content reaches the boundary.
    if (current instanceof ScrollView && (direction == FOCUS_UP || direction == FOCUS_DOWN)
        && current.canScrollVertically(direction == FOCUS_DOWN ? 1 : -1)) return false;
    if (current instanceof HorizontalScrollView && (direction == FOCUS_LEFT || direction == FOCUS_RIGHT)
        && current.canScrollHorizontally(direction == FOCUS_RIGHT ? 1 : -1)) return false;
    View nav = ReactFindViewUtil.findView(this, "tv-navigation");
    if (nav instanceof ViewGroup && inside(current, (ViewGroup) nav)) {
      if (direction == FOCUS_LEFT) return true;
      if (direction == FOCUS_UP || direction == FOCUS_DOWN) {
        View neighbor = FocusFinder.getInstance().findNextFocus((ViewGroup) nav, current, direction);
        if (neighbor != null) focusAndReveal(neighbor, direction);
        return true;
      }
    }
    // Off-screen list rows can sit beside the player bar; keep left/right inside the bar.
    View bar = ReactFindViewUtil.findView(this, "tv-player-bar");
    if (bar instanceof ViewGroup && inside(current, (ViewGroup) bar)
        && (direction == FOCUS_LEFT || direction == FOCUS_RIGHT)) {
      View neighbor = FocusFinder.getInstance().findNextFocus((ViewGroup) bar, current, direction);
      if (neighbor != null) focusAndReveal(neighbor, direction);
      return true;
    }
    View next = FocusFinder.getInstance().findNextFocus(this, current, direction);
    ScrollView scroll = verticalScrollParent(current);
    int sign = direction == FOCUS_DOWN ? 1 : -1;
    if ((direction == FOCUS_UP || direction == FOCUS_DOWN) && scroll != null
        && scroll.canScrollVertically(sign) && (next == null || !inside(next, scroll))) {
      // A virtualized list has not mounted its next row yet. Scroll first, then retry.
      scrolling = true;
      scroll.scrollBy(0, sign * Math.max(current.getHeight(), scroll.getHeight() / 3));
      postDelayed(() -> {
        scrolling = false;
        if (!isAttachedToWindow() || !hasWindowFocus()) return;
        if (!contains(current)) { scheduleFocus(); return; }
        View target = FocusFinder.getInstance().findNextFocus(this, current, direction);
        if (target != null && inside(target, scroll)) focusAndReveal(target, direction);
        else scheduleFocus();
      }, 100);
      return true;
    }
    if (next != null && next != current) return focusAndReveal(next, direction);
    // A text-only ScrollView (e.g. the agreement) still needs normal D-pad scrolling.
    return !(current instanceof ScrollView || current instanceof HorizontalScrollView);
  }

  private boolean focusAndReveal(View target, int direction) {
    if (target instanceof ReactEditText) {
      // RN 0.73 intentionally ignores ordinary Android requestFocus on TextInput.
      ((ReactEditText) target).requestFocusFromJS();
      if (!target.isFocused()) return false;
    } else if (!target.requestFocus(direction)) return false;
    Rect local = new Rect(0, 0, target.getWidth(), target.getHeight());
    target.requestRectangleOnScreen(local, true);
    return true;
  }

  @Override public boolean dispatchKeyEvent(KeyEvent event) {
    int direction = 0;
    switch (event.getKeyCode()) {
      case KeyEvent.KEYCODE_DPAD_UP: direction = FOCUS_UP; break;
      case KeyEvent.KEYCODE_DPAD_DOWN: direction = FOCUS_DOWN; break;
      case KeyEvent.KEYCODE_DPAD_LEFT: direction = FOCUS_LEFT; break;
      case KeyEvent.KEYCODE_DPAD_RIGHT: direction = FOCUS_RIGHT; break;
      case KeyEvent.KEYCODE_BACK:
        if (backFocusID != null) {
          View navigation = ReactFindViewUtil.findView(this, backFocusID);
          View focused = findFocus();
          View navGroup = ReactFindViewUtil.findView(this, "tv-navigation");
          if (navigation != null && focused != null && navGroup instanceof ViewGroup
              && !inside(focused, (ViewGroup) navGroup)) {
            if (event.getAction() == KeyEvent.ACTION_UP) navigation.requestFocus();
            return true;
          }
        }
        break;
    }
    if (direction != 0 && event.getAction() == KeyEvent.ACTION_DOWN && moveFocus(direction)) return true;
    return super.dispatchKeyEvent(event);
  }
}
