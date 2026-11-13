package cn.toside.music.mobile.tv;

import android.app.Activity;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import cn.toside.music.mobile.BuildConfig;
import java.util.HashMap;
import java.util.Map;

public class TVNavigationModule extends ReactContextBaseJavaModule {
  TVNavigationModule(ReactApplicationContext context) { super(context); }
  @Override public String getName() { return "TVNavigation"; }
  @Override public Map<String, Object> getConstants() {
    Map<String, Object> constants = new HashMap<>();
    constants.put("enabled", BuildConfig.IS_TV);
    constants.put("applicationId", BuildConfig.APPLICATION_ID);
    return constants;
  }
  private View findVisible(View view, String id) {
    if (!view.isShown()) return null;
    if (id.equals(view.getTag(com.facebook.react.R.id.view_tag_native_id))) return view;
    if (view instanceof ViewGroup) {
      ViewGroup group = (ViewGroup) view;
      for (int i = 0; i < group.getChildCount(); i++) {
        View found = findVisible(group.getChildAt(i), id);
        if (found != null) return found;
      }
    }
    return null;
  }
  @ReactMethod public void focus(String id) {
    Activity activity = getCurrentActivity();
    if (activity == null) return;
    activity.runOnUiThread(() -> {
      View target = findVisible(activity.getWindow().getDecorView(), id);
      if (target != null && target.requestFocus()) {
        target.requestRectangleOnScreen(new Rect(0, 0, target.getWidth(), target.getHeight()), true);
      }
    });
  }
}
