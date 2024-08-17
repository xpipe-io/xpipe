package io.xpipe.app.browser.fs;

import io.xpipe.app.util.ShellControlCache;
import io.xpipe.core.process.CommandBuilder;
import io.xpipe.core.process.OsType;
import io.xpipe.core.process.ShellControl;
import io.xpipe.core.process.ShellDialect;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public class OpenFileSystemCache extends ShellControlCache {

    private final OpenFileSystemModel model;
    private final String username;
    private final Map<Integer, String> users = new HashMap<>();
    private final Map<Integer, String> groups = new HashMap<>();

    public OpenFileSystemCache(OpenFileSystemModel model) throws Exception {
        super(model.getFileSystem().getShell().orElseThrow());
        this.model = model;

        ShellControl sc = model.getFileSystem().getShell().get();
        ShellDialect d = sc.getShellDialect();
        // If there is no id command, we should still be fine with just assuming root
        username = d.printUsernameCommand(sc).readStdoutIfPossible().orElse("root");
        loadUsers();
        loadGroups();
    }

    public int getUidForUser(String name) {
        return users.entrySet().stream().filter(e -> e.getValue().equals(name)).findFirst().map(e -> e.getKey()).orElse(0);
    }

    public int getGidForGroup(String name) {
        return groups.entrySet().stream().filter(e -> e.getValue().equals(name)).findFirst().map(e -> e.getKey()).orElse(0);
    }

    private void loadUsers() throws Exception {
        var sc = model.getFileSystem().getShell().orElseThrow();
        if (sc.getOsType() == OsType.WINDOWS) {
            return;
        }

        var lines = sc.command(CommandBuilder.of().add("cat").addFile("/etc/passwd")).readStdoutOrThrow();
        lines.lines().forEach(s -> {
            var split = s.split(":");
            users.put(Integer.parseInt(split[2]), split[0]);
        });
    }

    private void loadGroups() throws Exception {
        var sc = model.getFileSystem().getShell().orElseThrow();
        if (sc.getOsType() == OsType.WINDOWS) {
            return;
        }

        var lines = sc.command(CommandBuilder.of().add("cat").addFile("/etc/group")).readStdoutOrThrow();
        lines.lines().forEach(s -> {
            var split = s.split(":");
            groups.put(Integer.parseInt(split[2]), split[0]);
        });
    }

    public boolean isRoot() {
        return username.equals("root");
    }
}
