package io.xpipe.ext.base.script;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

import io.xpipe.app.comp.base.ContextualFileReferenceChoiceComp;
import io.xpipe.app.comp.base.IntegratedTextAreaComp;
import io.xpipe.app.core.AppCache;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.ext.ShellDialectChoiceComp;
import io.xpipe.app.ext.ValidationException;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.process.ShellDialect;
import io.xpipe.app.process.ShellScript;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.util.DocumentationLink;
import io.xpipe.app.util.HttpHelper;
import io.xpipe.app.util.Validators;
import io.xpipe.core.FilePath;
import io.xpipe.core.UuidHelper;
import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import lombok.Builder;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = ScriptTextSource.InPlace.class),
        @JsonSubTypes.Type(value = ScriptTextSource.Url.class),
        @JsonSubTypes.Type(value = ScriptTextSource.SourceReference.class)
})
public interface ScriptTextSource {

    @JsonTypeName("inPlace")
    @Value
    @Jacksonized
    @Builder
    class InPlace implements ScriptTextSource {

        ShellDialect dialect;
        ShellScript text;

        @SuppressWarnings("unused")
        public static String getOptionsNameKey() {
            return "scriptSourceTypeInPlace";
        }

        @SuppressWarnings("unused")
        static OptionsBuilder createOptions(Property<InPlace> property) {
            var dialect = new SimpleObjectProperty<>(property.getValue().getDialect());
            var text = new SimpleObjectProperty<>(property.getValue().getText());

            var availableDialects = ScriptDialects.getSupported();
            var choice = new ShellDialectChoiceComp(availableDialects, dialect, ShellDialectChoiceComp.NullHandling.NULL_IS_ALL);

            return new OptionsBuilder()
                    .name("minimumShellDialect")
                    .description("minimumShellDialectDescription")
                    .documentationLink(DocumentationLink.SCRIPTING_COMPATIBILITY)
                    .addComp(choice, dialect)
                    .name("scriptContents")
                    .description("scriptContentsDescription")
                    .documentationLink(DocumentationLink.SCRIPTING_EDITING)
                    .addComp(IntegratedTextAreaComp.script(text, Bindings.createStringBinding(() -> {
                        return dialect.getValue() != null
                                ? dialect.getValue().getScriptFileEnding()
                                : "sh";
                    }, dialect)))
                    .bind(() -> InPlace.builder().dialect(dialect.get()).text(text.get()).build(),
                    property);
        }

        @Override
        public void checkComplete() throws ValidationException {
            Validators.nonNull(text);
        }

        @Override
        public String toSummary() {
            return dialect.getDisplayName();
        }
    }

    @JsonTypeName("url")
    @Value
    @Jacksonized
    @Builder
    class Url implements ScriptTextSource {

        ShellDialect dialect;
        String url;
        ShellScript lastText;

        @SuppressWarnings("unused")
        public static String getOptionsNameKey() {
            return "scriptSourceTypeUrl";
        }

        @SuppressWarnings("unused")
        static OptionsBuilder createOptions(Property<Url> property) {
            var dialect = new SimpleObjectProperty<>(property.getValue().getDialect());
            var url = new SimpleStringProperty(property.getValue().getUrl());

            var availableDialects = ScriptDialects.getSupported();
            var choice = new ShellDialectChoiceComp(availableDialects, dialect, ShellDialectChoiceComp.NullHandling.NULL_IS_ALL);

            return new OptionsBuilder()
                    .name("minimumShellDialect")
                    .description("minimumShellDialectDescription")
                    .documentationLink(DocumentationLink.SCRIPTING_COMPATIBILITY)
                    .addComp(choice, dialect)
                    .nameAndDescription("scriptTextSourceUrl").addString(url).nonNull().bind(
                    () -> Url.builder().dialect(dialect.get()).url(url.get()).build(), property);
        }

        private void prepare() throws Exception {
            var path = getLocalPath();
            if (Files.exists(path)) {
                return;
            }

            var req = HttpRequest.newBuilder().GET().uri(URI.create(url)).build();
            var r = HttpHelper.client().send(req, HttpResponse.BodyHandlers.ofString());
            if (r.statusCode() >= 400) {
                throw ErrorEventFactory.expected(new IOException(r.body()));
            }

            Files.writeString(path, r.body());
        }

        private Path getLocalPath() {
            return AppCache.getBasePath().resolve("scripts").resolve(getName());
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
            Validators.nonNull(lastText);
        }

        @Override
        public String toSummary() {
            return url;
        }

        @Override
        @SneakyThrows
        public ShellScript getText() {
            return lastText;
        }
    }

    @JsonTypeName("source")
    @Value
    @Jacksonized
    @Builder
    class SourceReference implements ScriptTextSource {

        ScriptCollectionSourceEntry entry;

        @SuppressWarnings("unused")
        public static String getOptionsNameKey() {
            return "scriptSourceTypeSource";
        }

        @SuppressWarnings("unused")
        static OptionsBuilder createOptions(Property<SourceReference> property) {
            var entry = new SimpleObjectProperty<>(property.getValue().getEntry());

            return new OptionsBuilder().bind(
                            () -> SourceReference.builder().entry(entry.get()).build(), property);
        }

        @Override
        public void checkComplete() throws ValidationException {
            Validators.nonNull(entry);
            entry.getSource().checkComplete();
        }

        @Override
        public String toSummary() {
            return entry.getName();
        }

        @Override
        public ShellDialect getDialect() {
            return entry.getDialect();
        }

        @Override
        @SneakyThrows
        public ShellScript getText() {
            var r = Files.readString(entry.getLocalFile());
            return ShellScript.of(r);
        }
    }

    void checkComplete() throws ValidationException;

    String toSummary();

    ShellDialect getDialect();

    ShellScript getText();

    static List<Class<?>> getClasses() {
        var l = new ArrayList<Class<?>>();
        l.add(InPlace.class);
        l.add(Url.class);
        l.add(SourceReference.class);
        return l;
    }
}
