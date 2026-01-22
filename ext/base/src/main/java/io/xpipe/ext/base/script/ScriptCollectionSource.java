package io.xpipe.ext.base.script;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.app.comp.base.ContextualFileReferenceChoiceComp;
import io.xpipe.app.core.AppCache;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.ext.ValidationException;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.util.Validators;
import io.xpipe.core.FilePath;
import io.xpipe.core.UuidHelper;
import javafx.beans.property.*;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = ScriptCollectionSource.Directory.class),
        @JsonSubTypes.Type(value = ScriptCollectionSource.GitRepository.class)
})
public interface ScriptCollectionSource {

    @JsonTypeName("directory")
    @Value
    @Jacksonized
    @Builder
    class Directory implements ScriptCollectionSource {

        Path path;

        @SuppressWarnings("unused")
        static OptionsBuilder createOptions(Property<Directory> property) {
            var path = new SimpleObjectProperty<>(property.getValue().getPath() != null ? FilePath.of(property.getValue().getPath()) : null);
            return new OptionsBuilder()
                    .nameAndDescription("scriptDirectory")
                    .addComp(new ContextualFileReferenceChoiceComp(new ReadOnlyObjectWrapper<>(DataStorage.get().local().ref()),
                            path, null, List.of(), null, true), path)
                    .nonNull()
                    .bind(
                            () -> Directory.builder()
                                    .path(path.get() != null ? path.get().asLocalPath() : null)
                                    .build(),
                            property);
        }

        @Override
        public void checkComplete() throws ValidationException {
            Validators.nonNull(path);
        }

        @Override
        public void prepare() {
            if (!Files.isDirectory(path)) {
                throw ErrorEventFactory.expected(new IllegalStateException("Source directory " + path + " does not exist"));
            }
        }

        @Override
        public Path getLocalPath() {
            return path;
        }

        @Override
        public String toSummary() {
            return path.toString();
        }

        @Override
        public String toName() {
            return AppI18n.get("directorySource");
        }
    }

    @JsonTypeName("gitRepository")
    @Value
    @Jacksonized
    @Builder
    class GitRepository implements ScriptCollectionSource {

        String url;

        @SuppressWarnings("unused")
        static OptionsBuilder createOptions(Property<GitRepository> property) {
            var url = new SimpleStringProperty(property.getValue().getUrl());
            return new OptionsBuilder()
                    .nameAndDescription("scriptSourceUrl")
                    .addString(url)
                    .nonNull()
                    .bind(
                            () -> GitRepository.builder().url(url.get()).build(),
                            property);
        }

        private String getName() {
            var name = FilePath.of(url).getFileName();
            if (!name.isEmpty()) {
                return name;
            }

            return UuidHelper.generateFromObject(url).toString();
        }

        @Override
        public void checkComplete() throws ValidationException {
            Validators.nonNull(url);
        }

        @Override
        public void prepare() throws Exception {
            if (Files.exists(getLocalPath())) {
                ProcessControlProvider.get().pullRepository(getLocalPath());
            } else {
                ProcessControlProvider.get().cloneRepository(url, getLocalPath());
            }
        }

        @Override
        public Path getLocalPath() {
            return AppCache.getBasePath().resolve("scripts").resolve(getName());
        }

        @Override
        public String toSummary() {
            return url.replace("http://", "").replace("https://", "")
                    .replace("file://", "").replace("ssh://", "");
        }

        @Override
        public String toName() {
            return AppI18n.get("gitRepositorySource");
        }
    }

    void checkComplete() throws ValidationException;

    void prepare() throws Exception;

    Path getLocalPath();

    String toSummary();

    String toName();

    default List<ScriptCollectionSourceEntry> listScripts() {
        var availableDialects = ScriptDialects.getSupported();
        var l = new ArrayList<ScriptCollectionSourceEntry>();

        if (!Files.exists(getLocalPath())) {
            return l;
        }

        try {
            Files.walkFileTree(getLocalPath(), new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    var name = file.getFileName().toString();
                    var dialect = availableDialects.stream().filter(shellDialect -> {
                        return name.endsWith("." + shellDialect.getScriptFileEnding());
                    }).findFirst();
                    if (dialect.isEmpty()) {
                        return FileVisitResult.CONTINUE;
                    }

                    var entry = ScriptCollectionSourceEntry.builder()
                            .name(name)
                            .source(ScriptCollectionSource.this)
                            .dialect(dialect.get())
                            .localFile(file)
                            .build();
                    l.add(entry);

                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            ErrorEventFactory.fromThrowable(e).expected().handle();
        }
        return l;
    }

    static List<Class<?>> getClasses() {
        var l = new ArrayList<Class<?>>();
        l.add(Directory.class);
        l.add(GitRepository.class);
        return l;
    }
}
