package io.xpipe.app.browser.icon;

import io.xpipe.app.core.AppResources;
import io.xpipe.app.ext.FileEntry;
import io.xpipe.core.FileKind;

import lombok.Getter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class BrowserIconDirectoryType {

    private static final List<BrowserIconDirectoryType> ALL = new ArrayList<>();

    public static synchronized void loadDefinitions() {
        ALL.add(new BrowserIconDirectoryType() {

            @Override
            public String getId() {
                return "root";
            }

            @Override
            public boolean matches(FileEntry entry) {
                return entry.getPath().toString().equals("/")
                        || entry.getPath().toString().matches("\\w:\\\\");
            }

            @Override
            public String getIcon(FileEntry entry) {
                return "browser/default_root_folder.svg";
            }
        });

        AppResources.with(AppResources.MAIN_MODULE, "folder_list.txt", path -> {
            try (var reader =
                    new BufferedReader(new InputStreamReader(Files.newInputStream(path), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    var split = line.split("\\|");
                    var id = split[0].strip();
                    var filter = Arrays.stream(split[1].split(","))
                            .map(s -> {
                                return s.strip();
                            })
                            .collect(Collectors.toSet());

                    var closedIcon = "browser/" + split[2].strip();
                    var lightClosedIcon = split.length > 4 ? "browser/" + split[4].strip() : closedIcon;

                    ALL.add(new Simple(id, new BrowserIconVariant(lightClosedIcon, closedIcon), filter));
                }
            }
        });
    }

    public static synchronized List<BrowserIconDirectoryType> getAll() {
        return ALL;
    }

    public abstract String getId();

    public abstract boolean matches(FileEntry entry);

    public abstract String getIcon(FileEntry entry);

    public static class Simple extends BrowserIconDirectoryType {

        @Getter
        private final String id;

        private final BrowserIconVariant closed;
        private final Set<String> names;

        public Simple(String id, BrowserIconVariant closed, Set<String> names) {
            this.id = id;
            this.closed = closed;
            this.names = names;
        }

        @Override
        public boolean matches(FileEntry entry) {
            if (entry.getKind() != FileKind.DIRECTORY) {
                return false;
            }

            var name = entry.getPath().getFileName();
            return names.contains(name);
        }

        @Override
        public String getIcon(FileEntry entry) {
            return this.closed.getIcon();
        }
    }
}
