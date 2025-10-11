package io.xpipe.app.util;

import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.LocalShell;
import io.xpipe.app.process.PropertiesFormatsParser;
import lombok.Builder;
import lombok.Value;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class FlatpakCache {

    @Value
    @Builder
    public static class App {

        String id;
        String name;
    }

    private static final Map<String, App> apps = new LinkedHashMap<>();

    public static synchronized Optional<App> getApp(String id) throws Exception {
        if (apps.containsKey(id)) {
            return Optional.ofNullable(apps.get(id));
        }

        var info = LocalShell.getShell().command(CommandBuilder.of().add("flatpak", "info").addQuoted(id)).readStdoutIfPossible();
        if (info.isEmpty()) {
            apps.put(id, null);
            return Optional.empty();
        }

        var props = PropertiesFormatsParser.parse(info.get() , ":");
        var name = props.get("Name");
        var app = App.builder().id(id).name(name).build();
        apps.put(id, app);
        return Optional.ofNullable(app);
    }
}
