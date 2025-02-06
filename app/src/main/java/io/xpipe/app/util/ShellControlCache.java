package io.xpipe.app.util;

import io.xpipe.app.prefs.ExternalApplicationType;
import io.xpipe.app.prefs.ExternalEditorType;
import io.xpipe.core.process.OsType;
import io.xpipe.core.process.ShellControl;
import io.xpipe.core.store.FilePath;
import io.xpipe.core.util.FailableSupplier;

import lombok.Getter;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

@Getter
public class ShellControlCache {

    private final ShellControl shellControl;
    private final Map<String, Boolean> installedApplications = new HashMap<>();
    private final Map<String, Object> multiPurposeCache = new HashMap<>();

    public ShellControlCache(ShellControl shellControl) {
        this.shellControl = shellControl;
    }

    public boolean has(String key) {
        return multiPurposeCache.containsKey(key);
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

    public void setIfAbsentFailable(String key, FailableSupplier<Object> value) throws Exception {
        if (multiPurposeCache.get(key) == null) {
            multiPurposeCache.put(key, value.get());
        }
    }

    public boolean isApplicationInPath(String app) {
        if (!installedApplications.containsKey(app)) {
            try {
                var b = CommandSupport.isInPath(shellControl, app);
                installedApplications.put(app, b);
            } catch (Exception e) {
                installedApplications.put(app, false);
            }
        }

        return installedApplications.get(app);
    }
}
