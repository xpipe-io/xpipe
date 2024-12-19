package io.xpipe.app.util;

import io.xpipe.core.process.CommandBuilder;
import io.xpipe.core.process.OsType;
import io.xpipe.core.process.ShellControl;

import lombok.Getter;

import java.util.LinkedHashMap;
import java.util.Map;

public class PasswdFile {

    public static PasswdFile parse(ShellControl sc) throws Exception {
        var passwdFile = new PasswdFile();
        passwdFile.loadUsers(sc);
        return passwdFile;
    }

    @Getter
    private final Map<Integer, String> users = new LinkedHashMap<>();

    public int getUidForUser(String name) {
        return users.entrySet().stream()
                .filter(e -> e.getValue().equals(name))
                .findFirst()
                .map(e -> e.getKey())
                .orElse(0);
    }

    private void loadUsers(ShellControl sc) throws Exception {
        if (sc.getOsType() == OsType.WINDOWS || sc.getOsType() == OsType.MACOS) {
            return;
        }

        var lines = sc.command(CommandBuilder.of().add("cat").addFile("/etc/passwd"))
                .readStdoutIfPossible()
                .orElse("");
        lines.lines().forEach(s -> {
            var split = s.split(":");
            try {
                users.putIfAbsent(Integer.parseInt(split[2]), split[0]);
            } catch (Exception ignored) {
            }
        });

        if (users.isEmpty()) {
            users.put(0, "root");
        }
    }
}
