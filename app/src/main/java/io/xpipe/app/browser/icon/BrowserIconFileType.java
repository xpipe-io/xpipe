package io.xpipe.app.browser.icon;

import io.xpipe.app.core.AppResources;
import io.xpipe.core.store.FileEntry;
import io.xpipe.core.store.FileKind;

import lombok.Getter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

public abstract class BrowserIconFileType {

    private static final List<BrowserIconFileType> ALL = new ArrayList<>();

    public static synchronized BrowserIconFileType byId(String id) {
        return ALL.stream()
                .filter(fileType -> fileType.getId().equals(id))
                .findAny()
                .orElseThrow();
    }

    public static synchronized void loadDefinitions() {
        AppResources.with(AppResources.XPIPE_MODULE, "file_list.txt", path -> {
            try (var reader =
                    new BufferedReader(new InputStreamReader(Files.newInputStream(path), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    var split = line.split("\\|");
                    var id = split[0].strip();
                    var filter = Arrays.stream(split[1].split(","))
                            .map(s -> {
                                var r = s.strip();
                                if (r.startsWith(".")) {
                                    return r;
                                }

                                if (r.contains(".")) {
                                    return r;
                                }

                                return "." + r;
                            })
                            .collect(Collectors.toSet());
                    var darkIcon = "browser/" + split[2].strip();
                    var lightIcon = (split.length > 3 ? "browser/" + split[3].strip() : darkIcon);
                    ALL.add(new BrowserIconFileType.Simple(id, lightIcon, darkIcon, filter));
                }
            }
        });
    }

    public static synchronized List<BrowserIconFileType> getAll() {
        return ALL;
    }

    public abstract String getId();

    public abstract boolean matches(FileEntry entry);

    public abstract String getIcon();

    @Getter
    public static class Simple extends BrowserIconFileType {

        private final String id;
        private final BrowserIconVariant icon;
        private final Set<String> endings;

        public Simple(String id, String lightIcon, String darkIcon, Set<String> endings) {
            this.icon = new BrowserIconVariant(lightIcon, darkIcon);
            this.id = id;
            this.endings = endings;
        }

        @Override
        public boolean matches(FileEntry entry) {
            if (entry.getKind() == FileKind.DIRECTORY) {
                return false;
            }

            var name = entry.getPath().getFileName();
            var ext = entry.getPath().getExtension();
            return (ext.isPresent() && endings.contains("." + ext.get().toLowerCase(Locale.ROOT))) || endings.contains(name);
        }

        @Override
        public String getIcon() {
            return icon.getIcon();
        }
    }
}
