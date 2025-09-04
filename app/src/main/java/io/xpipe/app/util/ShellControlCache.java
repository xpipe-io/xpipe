package io.xpipe.app.util;

import io.xpipe.app.process.ShellControl;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public class ShellControlCache {

    private final ShellControl shellControl;
    private final Map<String, Boolean> installedApplications = new HashMap<>();

    public ShellControlCache(ShellControl shellControl) {
        this.shellControl = shellControl;
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
