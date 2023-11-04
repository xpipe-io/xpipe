package io.xpipe.app.browser.icon;

import io.xpipe.app.core.AppResources;
import io.xpipe.core.store.FileKind;
import io.xpipe.core.store.FileSystem;
import lombok.Getter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public interface FileType {

    List<FileType> ALL = new ArrayList<>();

    static FileType byId(String id) {
        return ALL.stream().filter(fileType -> fileType.getId().equals(id)).findAny().orElseThrow();
    }

    static void loadDefinitions() {
        AppResources.with(AppResources.XPIPE_MODULE, "file_list.txt", path -> {
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
                    var darkIcon = split[2].trim();
                    var lightIcon = split.length > 3 ? split[3].trim() : darkIcon;
                    ALL.add(new FileType.Simple(id, lightIcon, darkIcon, filter.toArray(String[]::new)));
                }
            }
        });
    }

    String getId();

    boolean matches(FileSystem.FileEntry entry);

    String getIcon();

    @Getter
    class Simple implements FileType {

        private final String id;
        private final IconVariant icon;
        private final String[] endings;

        public Simple(String id, String lightIcon, String darkIcon, String... endings) {
            this.icon = new IconVariant(lightIcon, darkIcon);
            this.id = id;
            this.endings = endings;
        }

        @Override
        public boolean matches(FileSystem.FileEntry entry) {
            if (entry.getKind() == FileKind.DIRECTORY) {
                return false;
            }

            return Arrays.stream(endings).anyMatch(ending -> entry.getPath().toLowerCase().endsWith(ending.toLowerCase()));
        }

        @Override
        public String getIcon() {
            return icon.getIcon();
        }
    }
}
