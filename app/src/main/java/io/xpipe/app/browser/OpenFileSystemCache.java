package io.xpipe.app.browser;

import io.xpipe.app.util.ApplicationHelper;
import io.xpipe.core.process.ShellControl;
import io.xpipe.core.process.ShellDialect;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public class OpenFileSystemCache {

    private final OpenFileSystemModel model;
    private final Map<String, Boolean> installedApplications = new HashMap<>();
    private String username;
    private boolean canElevate;

    public OpenFileSystemCache(OpenFileSystemModel model) {
        this.model = model;
    }

    public void init() throws Exception {
        ShellControl sc = model.getFileSystem().getShell().get();
        ShellDialect d = sc.getShellDialect();
        username = sc.executeSimpleStringCommand(d.getPrintVariableCommand(d.getUsernameVariableName()));
        canElevate = sc.checkCanElevate();
    }

    public boolean isRoot() {
        return username.equals("root");
    }

    public boolean isApplicationInPath(String app) {
        if (!installedApplications.containsKey(app)) {
            try {
                var b = ApplicationHelper.isInPath(
                        model.getFileSystem().getShell().orElseThrow(), app);
                installedApplications.put(app, b);
            } catch (Exception e) {
                installedApplications.put(app, false);
            }
        }

        return installedApplications.get(app);
    }
}
