package io.xpipe.app.util;

import io.xpipe.core.process.ShellControl;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

@Getter
public class ShellControlCache {

    private final ShellControl shellControl;
    private final Map<String, Boolean> installedApplications = new HashMap<>();
    private final Map<String, Object> multiPurposeCache = new HashMap<>();

    public ShellControlCache(ShellControl shellControl) {
        this.shellControl = shellControl;
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) multiPurposeCache.get(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T getOrDefault(String key, T val) {
        return (T) multiPurposeCache.getOrDefault(key, val);
    }

    public void set(String key, Object value) {
        multiPurposeCache.put(key, value);
    }

    public void setIfAbsent(String key, Supplier<Object> value) {
        multiPurposeCache.computeIfAbsent(key, s -> value.get());
    }

    public boolean isApplicationInPath(String app) {
        if (!installedApplications.containsKey(app)) {
            try {
                var b = ApplicationHelper.isInPath(shellControl, app);
                installedApplications.put(app, b);
            } catch (Exception e) {
                installedApplications.put(app, false);
            }
        }

        return installedApplications.get(app);
    }
}
