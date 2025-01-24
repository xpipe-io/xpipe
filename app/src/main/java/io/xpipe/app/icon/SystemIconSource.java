package io.xpipe.app.icon;

import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.core.process.CommandBuilder;
import lombok.Value;

import java.nio.file.Files;
import java.nio.file.Path;

public interface SystemIconSource {

    @Value
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
    }

    void refresh() throws Exception;

    String getId();

    Path getPath();
}
