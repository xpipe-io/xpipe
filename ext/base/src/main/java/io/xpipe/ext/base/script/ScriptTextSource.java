package io.xpipe.ext.base.script;

import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.comp.base.InputGroupComp;
import io.xpipe.app.comp.base.IntegratedTextAreaComp;
import io.xpipe.app.core.AppCache;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.window.AppDialog;
import io.xpipe.app.ext.ShellDialectChoiceComp;
import io.xpipe.app.ext.ValidationException;
import io.xpipe.app.hub.comp.StoreChoiceComp;
import io.xpipe.app.hub.comp.StoreViewState;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.platform.LabelGraphic;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.process.ShellDialect;
import io.xpipe.app.process.ShellScript;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.DocumentationLink;
import io.xpipe.app.util.HttpHelper;
import io.xpipe.app.util.Validators;
import io.xpipe.core.FilePath;
import io.xpipe.core.UuidHelper;

import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = ScriptTextSource.InPlace.class),
    @JsonSubTypes.Type(value = ScriptTextSource.SourceReference.class),
    @JsonSubTypes.Type(value = ScriptTextSource.Url.class)
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
            var choice = new ShellDialectChoiceComp(
                    availableDialects, dialect, ShellDialectChoiceComp.NullHandling.NULL_IS_ALL);

            return new OptionsBuilder()
                    .name("minimumShellDialect")
                    .description("minimumShellDialectDescription")
                    .documentationLink(DocumentationLink.SCRIPTING_COMPATIBILITY)
                    .addComp(choice, dialect)
                    .name("scriptContents")
                    .description("scriptContentsDescription")
                    .documentationLink(DocumentationLink.SCRIPTING_EDITING)
                    .addComp(
                            IntegratedTextAreaComp.script(
                                    text,
                                    Bindings.createStringBinding(
                                            () -> {
                                                return dialect.getValue() != null
                                                        ? dialect.getValue().getScriptFileEnding()
                                                        : "sh";
                                            },
                                            dialect)),
                            text)
                    .bind(
                            () -> InPlace.builder()
                                    .dialect(dialect.get())
                                    .text(text.get())
                                    .build(),
                            property);
        }

        @Override
        public void checkComplete() {}

        @Override
        public void checkAvailable() {}

        @Override
        public void validate() {
            checkAvailable();
        }

        @Override
        public String toSummary() {
            return AppI18n.get("inPlaceScript");
        }

        @Override
        public ShellScript getText() {
            return text != null ? text : ShellScript.empty();
        }
    }

    @JsonTypeName("url")
    @Value
    @Jacksonized
    @Builder
    class Url implements ScriptTextSource {

        ShellDialect dialect;
        String url;

        @SuppressWarnings("unused")
        public static String getOptionsNameKey() {
            return "scriptSourceTypeUrl";
        }

        @SuppressWarnings("unused")
        static OptionsBuilder createOptions(Property<Url> property) {
            var dialect = new SimpleObjectProperty<>(property.getValue().getDialect());
            var url = new SimpleStringProperty(property.getValue().getUrl());

            var availableDialects = ScriptDialects.getSupported();
            var choice = new ShellDialectChoiceComp(
                    availableDialects, dialect, ShellDialectChoiceComp.NullHandling.NULL_IS_ALL);

            return new OptionsBuilder()
                    .name("minimumShellDialect")
                    .description("minimumShellDialectDescription")
                    .documentationLink(DocumentationLink.SCRIPTING_COMPATIBILITY)
                    .addComp(choice, dialect)
                    .nameAndDescription("scriptTextSourceUrl")
                    .addString(url)
                    .nonNull()
                    .bind(
                            () -> Url.builder()
                                    .dialect(dialect.get())
                                    .url(url.get())
                                    .build(),
                            property);
        }

        public void refresh() throws Exception {
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
        }

        @Override
        public void checkAvailable() {
            if (!Files.exists(getLocalPath())) {
                throw ErrorEventFactory.expected(
                        new IllegalStateException("Script URL " + url + " has not been initialized"));
            }
        }

        @Override
        public void validate() throws Exception {
            refresh();
            checkAvailable();
        }

        @Override
        public String toSummary() {
            return AppI18n.get(
                    "sourcedFrom",
                    url.replace("http://", "")
                            .replace("https://", "")
                            .replace("file://", "")
                            .replace("ssh://", ""));
        }

        @Override
        @SneakyThrows
        public ShellScript getText() {
            var path = getLocalPath();

            if (!Files.exists(path)) {
                return ShellScript.empty();
            }

            try {
                var r = Files.readString(path);
                return ShellScript.of(r);
            } catch (IOException e) {
                ErrorEventFactory.fromThrowable(e).expected().handle();
                return ShellScript.empty();
            }
        }
    }

    @JsonTypeName("source")
    @Value
    @Jacksonized
    @Builder
    class SourceReference implements ScriptTextSource {

        DataStoreEntryRef<ScriptCollectionSourceStore> ref;
        String name;

        @SuppressWarnings("unused")
        public static String getOptionsNameKey() {
            return "scriptSourceTypeSource";
        }

        @SuppressWarnings("unused")
        static OptionsBuilder createOptions(Property<SourceReference> property) {
            var ref = new SimpleObjectProperty<>(property.getValue().getRef());
            var name = new SimpleStringProperty(property.getValue().getName());

            var sourceChoice = new StoreChoiceComp<>(
                    null,
                    ref,
                    ScriptCollectionSourceStore.class,
                    ignored -> true,
                    StoreViewState.get().getAllScriptsCategory(),
                    StoreViewState.get().getScriptSourcesCategory(),
                    true);

            var importButton = new ButtonComp(null, new LabelGraphic.IconGraphic("mdi2i-import"), () -> {
                var current = AppDialog.getCurrentModalOverlay();
                current.ifPresent(modalOverlay -> modalOverlay.close());

                var dialog = new ScriptCollectionSourceImportDialog(ref.get());
                dialog.show();
            });
            importButton.disable(ref.isNull());

            return new OptionsBuilder()
                    .nameAndDescription("scriptCollectionSourceEntry")
                    .addComp(new InputGroupComp(List.of(sourceChoice, importButton)).setMainReference(0), ref)
                    .nonNull()
                    .nameAndDescription("scriptSourceName")
                    .addString(name)
                    .nonNull()
                    .bind(
                            () -> SourceReference.builder()
                                    .ref(ref.getValue())
                                    .name(name.getValue())
                                    .build(),
                            property);
        }

        @Override
        @SneakyThrows
        public void checkComplete() {
            Validators.nonNull(ref);
            ref.checkComplete();
        }

        @Override
        public void checkAvailable() {
            var cached = ref.getStore().getState().getEntries();
            if (cached == null) {
                throw ErrorEventFactory.expected(
                        new IllegalStateException("Source " + ref.get().getName() + " has not been initialized"));
            }

            var found = cached.stream()
                    .filter(e -> e.getName().equals(name))
                    .findFirst()
                    .orElse(null);
            if (found == null) {
                throw ErrorEventFactory.expected(new IllegalStateException("Script file " + name
                        + " not found in local cache for source " + ref.get().getName()));
            }

            if (!Files.exists(found.getLocalFile())) {
                throw ErrorEventFactory.expected(
                        new IllegalStateException("Referenced script file " + found.getLocalFile() + " does not exist"));
            }
        }

        @Override
        public void validate() {
            checkAvailable();
        }

        @Override
        public String toSummary() {
            return AppI18n.get("sourcedFrom", ref.get().getName());
        }

        @Override
        public ShellDialect getDialect() {
            var found = findSourceEntryIfPossible();
            return found != null ? found.getDialect() : null;
        }

        @Override
        @SneakyThrows
        public ShellScript getText() {
            var found = findSourceEntryIfPossible();
            if (found == null) {
                return ShellScript.empty();
            }

            if (!Files.exists(found.getLocalFile())) {
                return ShellScript.empty();
            }

            try {
                var r = Files.readString(found.getLocalFile());
                return ShellScript.of(r);
            } catch (IOException e) {
                ErrorEventFactory.fromThrowable(e).expected().handle();
                return ShellScript.empty();
            }
        }

        private ScriptCollectionSourceEntry findSourceEntryIfPossible() {
            if (ref == null) {
                return null;
            }

            var cached = ref.getStore().getState().getEntries();
            if (cached == null) {
                return null;
            }

            var found = cached.stream()
                    .filter(e -> e.getName().equals(name))
                    .findFirst()
                    .orElse(null);
            if (found == null) {
                return null;
            }

            return found;
        }
    }

    void checkComplete() throws ValidationException;

    void checkAvailable();

    void validate() throws Exception;

    String toSummary();

    ShellDialect getDialect();

    ShellScript getText();

    static List<Class<?>> getClasses() {
        var l = new ArrayList<Class<?>>();
        l.add(InPlace.class);
        l.add(SourceReference.class);
        l.add(Url.class);
        return l;
    }
}
