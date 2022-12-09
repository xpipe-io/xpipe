package io.xpipe.extension.prefs;

import com.dlsc.preferencesfx.model.Setting;

import java.util.List;

public interface PrefsHandler {

    void addSetting(List<String> category, String group, Setting<?,?> setting, Class<?> c);
}
