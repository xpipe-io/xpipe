package io.xpipe.app.storage;

import io.xpipe.app.comp.base.*;
import io.xpipe.app.core.App;
import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.ext.ValidationException;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.process.CountDown;
import io.xpipe.app.process.ShellScript;
import io.xpipe.app.secret.SecretPasswordManagerStrategy;
import io.xpipe.app.secret.SecretQueryState;
import io.xpipe.app.util.HttpHelper;
import io.xpipe.app.util.Validators;

import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public interface DataStorageGroupStrategy {

    static List<Class<?>> getClasses() {
        var l = new ArrayList<Class<?>>();
        l.add(PasswordManager.class);
        l.add(File.class);
        l.add(Command.class);
        l.add(HttpRequest.class);
        return l;
    }

    default void checkComplete() throws ValidationException {}

    String queryEncryptionSecret() throws Exception;

    @JsonTypeName("passwordManager")
    @Builder
    @Jacksonized
    @Value
    public class PasswordManager implements DataStorageGroupStrategy {

        @SuppressWarnings("unused")
        public static String getOptionsNameKey() {
            return "passwordManager";
        }

        @SuppressWarnings("unused")
        public static OptionsBuilder createOptions(Property<PasswordManager> p) {
            var key = new SimpleStringProperty(p.getValue().getKey());

            var prefs = AppPrefs.get();
            var field = new TextFieldComp(key).apply(struc -> struc.get()
                    .promptTextProperty()
                    .bind(Bindings.createStringBinding(
                            () -> {
                                return prefs.passwordManager().getValue() != null
                                        ? prefs.passwordManager().getValue().getKeyPlaceholder()
                                        : "?";
                            },
                            prefs.passwordManager())));
            var button = new ButtonComp(null, new FontIcon("mdomz-settings"), () -> {
                AppPrefs.get().selectCategory("passwordManager");
                App.getApp().getStage().requestFocus();
            });
            var content = new InputGroupComp(List.of(field, button));
            content.setMainReference(field);

            return new OptionsBuilder()
                    .nameAndDescription("passwordManagerKey")
                    .addComp(content, key)
                    .nonNull()
                    .bind(
                            () -> {
                                return PasswordManager.builder().key(key.get()).build();
                            },
                            p);
        }

        String key;

        @Override
        public void checkComplete() throws ValidationException {
            Validators.nonNull(key);
        }

        @Override
        public String queryEncryptionSecret() {
            var r = SecretPasswordManagerStrategy.builder()
                    .key(key)
                    .build()
                    .query()
                    .query("Group secret");
            return r.getState() == SecretQueryState.NORMAL ? r.getSecret().getSecretValue() : null;
        }
    }

    @JsonTypeName("file")
    @Builder
    @Jacksonized
    @Value
    public class File implements DataStorageGroupStrategy {

        @SuppressWarnings("unused")
        public static String getOptionsNameKey() {
            return "fileSecret";
        }

        @SuppressWarnings("unused")
        public static OptionsBuilder createOptions(Property<File> p) {
            var file = new SimpleObjectProperty<>(
                    p.getValue().getFile() != null ? p.getValue().getFile().toLocalAbsoluteFilePath() : null);
            return new OptionsBuilder()
                    .nameAndDescription("fileSecretChoice")
                    .addComp(
                            new ContextualFileReferenceChoiceComp(
                                    new ReadOnlyObjectWrapper<>(
                                            DataStorage.get().local().ref()),
                                    file,
                                    null,
                                    List.of(),
                                    e -> e.equals(DataStorage.get().local()),
                                    false),
                            file)
                    .nonNull()
                    .bind(
                            () -> {
                                return File.builder()
                                        .file(ContextualFileReference.of(file.get()))
                                        .build();
                            },
                            p);
        }

        ContextualFileReference file;

        @Override
        public void checkComplete() throws ValidationException {
            Validators.nonNull(file);
        }

        @Override
        public String queryEncryptionSecret() throws Exception {
            var abs = file.toLocalAbsoluteFilePath().asLocalPath();
            if (!Files.exists(abs)) {
                throw ErrorEventFactory.expected(
                        new IllegalArgumentException("Group key file " + file + " does not exist"));
            }

            var read = Files.readString(abs);
            if (read.length() == 0) {
                throw ErrorEventFactory.expected(new IllegalArgumentException("Group key file " + file + " is empty"));
            }

            return read;
        }
    }

    @JsonTypeName("command")
    @Builder
    @Jacksonized
    @Value
    public class Command implements DataStorageGroupStrategy {

        @SuppressWarnings("unused")
        public static String getOptionsNameKey() {
            return "commandSecret";
        }

        @SuppressWarnings("unused")
        public static OptionsBuilder createOptions(Property<Command> p) {
            var command = new SimpleStringProperty(
                    p.getValue().getCommand() != null
                            ? p.getValue().getCommand().getValue()
                            : null);
            return new OptionsBuilder()
                    .nameAndDescription("commandSecretField")
                    .addComp(new TextAreaComp(command), command)
                    .nonNull()
                    .bind(
                            () -> {
                                return Command.builder()
                                        .command(command.get() != null ? new ShellScript(command.get()) : null)
                                        .build();
                            },
                            p);
        }

        ShellScript command;

        @Override
        public void checkComplete() throws ValidationException {
            Validators.nonNull(command);
        }

        @Override
        public String queryEncryptionSecret() throws Exception {
            try (var sc = ProcessControlProvider.get().createLocalProcessControl(true)) {
                try (var cc = sc.command(command).start()) {
                    cc.killOnTimeout(CountDown.of().start(30_000));
                    var out = cc.readStdoutOrThrow();
                    if (out.length() == 0) {
                        throw ErrorEventFactory.expected(
                                new IllegalArgumentException("Command did not return any output"));
                    }
                    return out;
                }
            }
        }
    }

    @JsonTypeName("httpRequest")
    @Builder
    @Jacksonized
    @Value
    public class HttpRequest implements DataStorageGroupStrategy {

        @SuppressWarnings("unused")
        public static String getOptionsNameKey() {
            return "httpRequestSecret";
        }

        @SuppressWarnings("unused")
        public static OptionsBuilder createOptions(Property<HttpRequest> p) {
            var uri = new SimpleStringProperty(p.getValue().getUri());
            return new OptionsBuilder()
                    .nameAndDescription("httpRequestSecretField")
                    .addString(uri)
                    .nonNull()
                    .bind(
                            () -> {
                                return HttpRequest.builder().uri(uri.get()).build();
                            },
                            p);
        }

        String uri;

        @Override
        public void checkComplete() throws ValidationException {
            Validators.nonNull(uri);
        }

        @Override
        public String queryEncryptionSecret() throws Exception {
            var uri = URI.create(getUri());
            var request = java.net.http.HttpRequest.newBuilder()
                    .uri(uri)
                    .POST(java.net.http.HttpRequest.BodyPublishers.noBody())
                    .build();
            var result = HttpHelper.client().send(request, HttpResponse.BodyHandlers.ofString());
            if (result.statusCode() >= 400) {
                throw ErrorEventFactory.expected(new IOException(result.body()));
            }
            var body = result.body();
            if (body.length() == 0) {
                throw ErrorEventFactory.expected(new IllegalArgumentException("Http response body is empty"));
            }
            return body;
        }
    }
}
