package io.xpipe.app.util;

import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.ShellControl;
import io.xpipe.core.OsType;

import lombok.Getter;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.OptionalInt;

@Getter
public class PasswdFile {

    public static PasswdFile parse(ShellControl sc) throws Exception {
        var passwdFile = new PasswdFile();
        passwdFile.loadUsers(sc);
        return passwdFile;
    }

    private final Map<Integer, String> users = new LinkedHashMap<>();

    public OptionalInt getUidForUserIfPresent(String name) {
        var found = users.entrySet().stream()
                .filter(e -> e.getValue().equals(name))
                .findFirst()
                .map(e -> e.getKey())
                .orElse(null);
        return found != null ? OptionalInt.of(found) : OptionalInt.empty();
    }

    public int getUidForUser(String name) {
        return getUidForUserIfPresent(name).orElse(0);
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
