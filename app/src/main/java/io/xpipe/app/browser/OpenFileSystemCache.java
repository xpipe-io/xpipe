package io.xpipe.app.browser;

import io.xpipe.app.util.ApplicationHelper;

import java.util.HashMap;
import java.util.Map;

public class OpenFileSystemCache {

    private final OpenFileSystemModel model;
    private final Map<String, Boolean> installedApplications = new HashMap<>();

    public OpenFileSystemCache(OpenFileSystemModel model) {
        this.model = model;
    }

    public boolean isApplicationInPath(String app) {
        if (!installedApplications.containsKey(app)) {
            try {
                var b = ApplicationHelper.isInPath(model.getFileSystem().getShell().orElseThrow(), app);
                installedApplications.put(app, b);
            } catch (Exception e) {
                installedApplications.put(app, false);
            }
        }

        return installedApplications.get(app);
    }
}
