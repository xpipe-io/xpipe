package io.xpipe.app.browser.icon;

import io.xpipe.app.core.AppResources;
import io.xpipe.core.store.FileKind;
import io.xpipe.core.store.FileNames;
import io.xpipe.core.store.FileSystem;
import lombok.Getter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public interface DirectoryType {

    List<DirectoryType> ALL = new ArrayList<>();

    static DirectoryType byId(String id) {
        return ALL.stream().filter(fileType -> fileType.getId().equals(id)).findAny().orElseThrow();
    }

    static void loadDefinitions() {
        ALL.add(new DirectoryType() {

            @Override
            public String getId() {
                return "root";
            }

            @Override
            public boolean matches(FileSystem.FileEntry entry) {
                return entry.getPath().equals("/") || entry.getPath().matches("\\w:\\\\");
            }

            @Override
            public String getIcon(FileSystem.FileEntry entry, boolean open) {
                return open ? "default_root_folder_opened.svg" : "default_root_folder.svg";
            }
        });

        AppResources.with(AppResources.XPIPE_MODULE, "folder_list.txt", path -> {
            try (var reader = new BufferedReader(new InputStreamReader(Files.newInputStream(path), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    var split = line.split("\\|");
                    var id = split[0].trim();
                    var filter = Arrays.stream(split[1].split(",")).map(s -> {
                        var r = s.trim();
                        if (r.startsWith(".")) {
                            return r;
                        }

                        if (r.contains(".")) {
                            return r;
                        }

                        return "." + r;
                    }).toList();

                    var closedIcon = split[2].trim();
                    var openIcon = split[3].trim();

                    var lightClosedIcon = split.length > 4 ? split[4].trim() : closedIcon;
                    var lightOpenIcon = split.length > 4 ? split[5].trim() : openIcon;

                    ALL.add(new Simple(id, new IconVariant(lightClosedIcon, closedIcon), new IconVariant(lightOpenIcon, openIcon),
                            filter.toArray(String[]::new)));
                }
            }
        });
    }

    String getId();

    boolean matches(FileSystem.FileEntry entry);

    String getIcon(FileSystem.FileEntry entry, boolean open);

    class Simple implements DirectoryType {

        @Getter
        private final String id;

        private final IconVariant closed;
        private final IconVariant open;
        private final String[] names;

        public Simple(String id, IconVariant closed, IconVariant open, String... names) {
            this.id = id;
            this.closed = closed;
            this.open = open;
            this.names = names;
        }

        @Override
        public boolean matches(FileSystem.FileEntry entry) {
            if (entry.getKind() != FileKind.DIRECTORY) {
                return false;
            }

            return Arrays.stream(names).anyMatch(name -> FileNames.getFileName(entry.getPath()).equalsIgnoreCase(name));
        }

        @Override
        public String getIcon(FileSystem.FileEntry entry, boolean open) {
            return open ? this.open.getIcon() : this.closed.getIcon();
        }
    }
}
