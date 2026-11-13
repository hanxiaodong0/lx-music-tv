package cn.toside.music.mobile.tv;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.widget.SeekBar;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.common.MapBuilder;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.annotations.ReactProp;
import com.facebook.react.uimanager.events.RCTEventEmitter;
import java.util.Map;

public class TVSeekBarManager extends SimpleViewManager<SeekBar> {
  @Override public String getName() { return "TVSeekBar"; }
  @Override protected SeekBar createViewInstance(ThemedReactContext context) {
    SeekBar view = new SeekBar(context);
    view.setFocusable(true);
    view.setThumbTintList(ColorStateList.valueOf(Color.rgb(255, 202, 74)));
    view.setProgressTintList(ColorStateList.valueOf(Color.rgb(255, 202, 74)));
    view.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
      @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
        if (!fromUser) return;
        WritableMap event = Arguments.createMap();
        event.putDouble("value", progress);
        context.getJSModule(RCTEventEmitter.class).receiveEvent(bar.getId(), "topSeek", event);
      }
      @Override public void onStartTrackingTouch(SeekBar bar) {}
      @Override public void onStopTrackingTouch(SeekBar bar) {}
    });
    return view;
  }
  @ReactProp(name = "duration") public void setDuration(SeekBar view, double value) {
    int duration = Double.isNaN(value) || Double.isInfinite(value) ? 0 : (int) Math.max(0, value);
    view.setMax(duration);
    view.setKeyProgressIncrement(10);
    view.setEnabled(duration > 0);
  }
  @ReactProp(name = "value") public void setValue(SeekBar view, double value) {
    if (!view.isPressed()) view.setProgress((int) Math.max(0, value));
  }
  @Override public Map<String, Object> getExportedCustomDirectEventTypeConstants() {
    return MapBuilder.of("topSeek", MapBuilder.of("registrationName", "onSeek"));
  }
}
