package io.xpipe.app.ext;

import com.dlsc.preferencesfx.model.Setting;

import java.util.List;

public interface PrefsHandler {

    void addSetting(List<String> category, String group, Setting<?, ?> setting, Class<?> c);
}
