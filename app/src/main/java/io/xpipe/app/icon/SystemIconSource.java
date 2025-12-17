package io.xpipe.app.icon;

import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.ext.ValidationException;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.util.DesktopHelper;
import io.xpipe.app.util.Hyperlinks;
import io.xpipe.app.util.Validators;
import io.xpipe.core.FilePath;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
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

    void checkComplete() throws ValidationException;

    void refresh() throws Exception;

    String getId();

    Path getPath();

    String getIcon();

    String getDisplayName();

    String getDescription();

    void open();

    @Value
    @Builder
    @Jacksonized
    @JsonTypeName("directory")
    class Directory implements SystemIconSource {

        Path path;
        String id;

        @Override
        public void checkComplete() throws ValidationException {
            Validators.nonNull(path);
            if (path.getParent() == null) {
                throw new ValidationException("Directory is a root");
            }
            Validators.notEmpty(id);
        }

        @Override
        public void refresh() {}

        @Override
        public Path getPath() {
            return path;
        }

        @Override
        public String getIcon() {
            return "mdi2f-folder";
        }

        @Override
        public String getDisplayName() {
            var name = path.getFileName();
            return name != null ? name.toString() : path.toString();
        }

        @Override
        public String getDescription() {
            return path.toString();
        }

        @Override
        public void open() {
            if (Files.exists(path)) {
                DesktopHelper.browseFile(path);
            }
        }
    }

    @Value
    @Builder
    @Jacksonized
    @JsonTypeName("git")
    class GitRepository implements SystemIconSource {

        String remote;
        String id;

        @Override
        public void checkComplete() throws ValidationException {
            Validators.notEmpty(remote);
            Validators.notEmpty(id);
        }

        @Override
        public void refresh() throws Exception {
            try (var sc =
                    ProcessControlProvider.get().createLocalProcessControl(true).start()) {
                var present = sc.view().findProgram("git").isPresent();
                if (!present) {
                    var msg =
                            "Git command-line tools are not available in the PATH but are required to use icons from a git repository. For more "
                                    + "details, see https://git-scm.com/downloads.";
                    ErrorEventFactory.fromMessage(msg).expected().handle();
                    return;
                }

                var dir = SystemIconManager.getPoolPath().resolve(id);
                if (!Files.exists(dir)) {
                    sc.command(CommandBuilder.of()
                                    .add("git", "clone")
                                    .addQuoted(remote)
                                    .addFile(dir.toString()))
                            .execute();
                } else {
                    sc.command(CommandBuilder.of().add("git", "pull"))
                            .withWorkingDirectory(FilePath.of(dir))
                            .execute();
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
        public String getDisplayName() {
            return FilePath.of(remote).getFileName();
        }

        @Override
        public String getDescription() {
            return "Git repository " + remote;
        }

        @Override
        public void open() {
            Hyperlinks.open(remote);
        }
    }
}
