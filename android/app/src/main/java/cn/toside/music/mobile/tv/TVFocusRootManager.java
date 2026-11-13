package cn.toside.music.mobile.tv;

import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.annotations.ReactProp;
import com.facebook.react.views.view.ReactViewGroup;
import com.facebook.react.views.view.ReactViewManager;

public class TVFocusRootManager extends ReactViewManager {
  @Override public String getName() { return "TVFocusRoot"; }
  @Override public ReactViewGroup createViewInstance(ThemedReactContext context) {
    return new TVFocusRoot(context);
  }
  @ReactProp(name = "backFocusID")
  public void setBackFocusID(ReactViewGroup view, String value) {
    ((TVFocusRoot) view).backFocusID = value;
  }
}

