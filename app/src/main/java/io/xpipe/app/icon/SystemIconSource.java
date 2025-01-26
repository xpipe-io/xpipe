package io.xpipe.app.icon;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.util.DesktopHelper;
import io.xpipe.app.util.Hyperlinks;
import io.xpipe.core.process.CommandBuilder;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.nio.file.Files;
import java.nio.file.Path;


@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = SystemIconSource.Directory.class),
        @JsonSubTypes.Type(value = SystemIconSource.GitRepository.class)
})
public interface SystemIconSource {

    @Value
    @Builder
    @Jacksonized
    @JsonTypeName("directory")
    static class Directory implements SystemIconSource{

        Path path;
        String id;

        @Override
        public void refresh() throws Exception {}

        @Override
        public Path getPath() {
            return path;
        }

        @Override
        public String getIcon() {
            return "mdi2f-folder";
        }

        @Override
        public String getDescription() {
            return path.toString();
        }

        @Override
        public void open() throws Exception {
            DesktopHelper.browsePathLocal(path);
        }
    }

    @Value
    @Builder
    @Jacksonized
    @JsonTypeName("git")
    static class GitRepository implements SystemIconSource{

        String remote;
        String id;

        @Override
        public void refresh() throws Exception {
            try (var sc = ProcessControlProvider.get().createLocalProcessControl(true).start()) {
                var dir = SystemIconManager.getPoolPath().resolve(id);
                if (!Files.exists(dir)) {
                    sc.command(CommandBuilder.of().add("git", "clone").addQuoted(remote).addFile(dir.toString())).execute();
                } else {
                    sc.command(CommandBuilder.of().add("git", "pull")).withWorkingDirectory(dir.toString()).execute();
                }
            }
        }

        @Override
        public Path getPath() {
            return SystemIconManager.getPoolPath().resolve(id);
        }

        @Override
        public String getIcon() {
            return "mdi2g-git";
        }

        @Override
        public String getDescription() {
            return "Git repository " + remote;
        }

        @Override
        public void open() throws Exception {
            Hyperlinks.open(remote);
        }
    }

    void refresh() throws Exception;

    String getId();

    Path getPath();

    String getIcon();

    String getDescription();

    void open() throws Exception;
}