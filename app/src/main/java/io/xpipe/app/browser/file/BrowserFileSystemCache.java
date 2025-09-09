package io.xpipe.app.browser.file;

import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.ShellControl;
import io.xpipe.app.process.ShellDialect;
import io.xpipe.app.util.PasswdFile;
import io.xpipe.app.process.ShellControlCache;
import io.xpipe.core.OsType;

import lombok.Getter;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
public class BrowserFileSystemCache extends ShellControlCache {

    private final BrowserFileSystemTabModel model;
    private final String username;
    private final PasswdFile passwdFile;
    private final Map<Integer, String> groups = new LinkedHashMap<>();

    public BrowserFileSystemCache(BrowserFileSystemTabModel model) throws Exception {
        super(model.getFileSystem().getShell().orElseThrow());
        this.model = model;

        ShellControl sc = model.getFileSystem().getShell().get();
        ShellDialect d = sc.getShellDialect();
        // If there is no id command, we should still be fine with just assuming root
        username = d.printUsernameCommand(sc).readStdoutIfPossible().orElse("root");
        passwdFile = PasswdFile.parse(sc);
        loadGroups();
    }

    public Map<Integer, String> getUsers() {
        return passwdFile.getUsers();
    }

    public int getUidForUser(String name) {
        return passwdFile.getUidForUser(name);
    }

    public int getGidForGroup(String name) {
        return groups.entrySet().stream()
                .filter(e -> e.getValue().equals(name))
                .findFirst()
                .map(e -> e.getKey())
                .orElse(0);
    }

    private void loadGroups() throws Exception {
        var sc = model.getFileSystem().getShell().orElseThrow();
        if (sc.getOsType() == OsType.WINDOWS || sc.getOsType() == OsType.MACOS) {
            return;
        }

        var lines = sc.command(CommandBuilder.of().add("cat").addFile("/etc/group"))
                .sensitive()
                .readStdoutIfPossible()
                .orElse("");
        lines.lines().forEach(s -> {
            var split = s.split(":");
            try {
                groups.putIfAbsent(Integer.parseInt(split[2]), split[0]);
            } catch (Exception ignored) {
            }
        });

        if (groups.isEmpty()) {
            groups.put(0, "root");
        }
    }

    public boolean isRoot() {
        return username.equals("root");
    }
}
