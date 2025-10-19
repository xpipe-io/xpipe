package io.xpipe.app.util;

import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.ShellControl;
import io.xpipe.core.OsType;
import lombok.Getter;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.OptionalInt;

@Getter
public class GroupFile {

    private final Map<Integer, String> groups = new LinkedHashMap<>();

    public static GroupFile parse(ShellControl sc) throws Exception {
        var f = new GroupFile();
        f.loadGroups(sc);
        return f;
    }

    public OptionalInt getGidForGroupIfPresent(String name) {
        var found = groups.entrySet().stream()
                .filter(e -> e.getValue().equals(name))
                .findFirst()
                .map(e -> e.getKey())
                .orElse(null);
        return found != null ? OptionalInt.of(found) : OptionalInt.empty();
    }

    public int getGidForGroup(String name) {
        return getGidForGroupIfPresent(name).orElse(0);
    }

    private void loadGroups(ShellControl sc) throws Exception {
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
}
