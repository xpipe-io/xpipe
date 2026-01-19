package io.xpipe.ext.base.script;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.app.comp.base.ContextualFileReferenceChoiceComp;
import io.xpipe.app.core.AppCache;
import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.ShellDialects;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.core.FilePath;
import io.xpipe.core.SecretValue;
import io.xpipe.core.UuidHelper;
import javafx.beans.property.*;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public interface ScriptSource {

    @JsonTypeName("directory")
    @Value
    @Jacksonized
    @Builder
    class Directory implements ScriptSource {

        Path path;

        @SuppressWarnings("unused")
        static OptionsBuilder createOptions(Property<Directory> property) {
            var path = new SimpleObjectProperty<>(property.getValue().getPath() != null ? FilePath.of(property.getValue().getPath()) : null);
            return new OptionsBuilder()
                    .nameAndDescription("scriptDirectory")
                    .addComp(new ContextualFileReferenceChoiceComp(new ReadOnlyObjectWrapper<>(DataStorage.get().local().ref()), path, null, List.of(), null, true))
                    .nonNull()
                    .bind(
                            () -> Directory.builder()
                                    .path(path.get() != null ? path.get().asLocalPath() : null)
                                    .build(),
                            property);
        }

        @Override
        public void prepare() {

        }

        @Override
        public Path getLocalPath() {
            return path;
        }
    }

    @JsonTypeName("gitRepository")
    @Value
    @Jacksonized
    @Builder
    class GitRepository implements ScriptSource {

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
        public void prepare() {

        }

        @Override
        public Path getLocalPath() {
            return AppCache.getBasePath().resolve("scripts").resolve(getName());
        }
    }

    void prepare();

    Path getLocalPath();

    static List<Class<?>> getClasses() {
        var l = new ArrayList<Class<?>>();
        l.add(Directory.class);
        l.add(GitRepository.class);
        return l;
    }
}
