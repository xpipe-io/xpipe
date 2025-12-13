package io.xpipe.app.storage;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.base.ContextualFileReferenceChoiceComp;
import io.xpipe.app.comp.base.SecretFieldComp;
import io.xpipe.app.comp.base.TextAreaComp;
import io.xpipe.app.comp.base.TextFieldComp;
import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.ext.ValidationException;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.process.CountDown;
import io.xpipe.app.process.ShellScript;
import io.xpipe.app.secret.*;
import io.xpipe.app.util.HttpHelper;
import io.xpipe.app.util.Validators;
import io.xpipe.core.FilePath;
import io.xpipe.core.InPlaceSecretValue;
import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import lombok.Builder;
import lombok.Value;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public interface DataStorageGroupStrategy {

    static List<Class<?>> getClasses() {
        var l = new ArrayList<Class<?>>();
        l.add(None.class);
        l.add(File.class);
        l.add(Command.class);
        l.add(HttpRequest.class);
        return l;
    }

    default boolean requiresUnlock() {
        return true;
    }

    default void checkComplete() throws ValidationException {}

    byte[] queryEncryptionSecret() throws Exception;

    @JsonTypeName("none")
    @Value
    public class None implements DataStorageGroupStrategy {

        @Override
        public boolean requiresUnlock() {
            return false;
        }

        @Override
        public byte[] queryEncryptionSecret() throws Exception {
            throw  new UnsupportedOperationException();
        }
    }

    @JsonTypeName("file")
    @Builder
    @Value
    public class File implements DataStorageGroupStrategy {

        @SuppressWarnings("unused")
        public static String getOptionsNameKey() {
            return "fileSecret";
        }

        @SuppressWarnings("unused")
        public static OptionsBuilder createOptions(Property<File> p) {
            var file = new SimpleObjectProperty<>(p.getValue().getFile() != null ? p.getValue().getFile().toLocalAbsoluteFilePath() : null);
            return new OptionsBuilder()
                    .nameAndDescription("fileSecretChoice")
                    .addComp(new ContextualFileReferenceChoiceComp(
                            new ReadOnlyObjectWrapper<>(DataStorage.get().local().ref()),
                            file, null, List.of(), e -> e.equals(DataStorage.get().local())), file)
                    .nonNull()
                    .bind(
                            () -> {
                                return File.builder().file(ContextualFileReference.of(file.get())).build();
                            },
                            p);
        }

        ContextualFileReference file;

        @Override
        public void checkComplete() throws ValidationException {
            Validators.nonNull(file);
        }

        @Override
        public byte[] queryEncryptionSecret() throws Exception {
            var abs = file.toLocalAbsoluteFilePath().asLocalPath();
            if (!Files.exists(abs)) {
                throw ErrorEventFactory.expected(new IllegalArgumentException("Group key file " + file + " does not exist"));
            }

            var read = Files.readAllBytes(abs);
            return read;
        }
    }

    @JsonTypeName("command")
    @Builder
    @Value
    public class Command implements DataStorageGroupStrategy {

        @SuppressWarnings("unused")
        public static String getOptionsNameKey() {
            return "commandSecret";
        }

        @SuppressWarnings("unused")
        public static OptionsBuilder createOptions(Property<Command> p) {
            var command = new SimpleStringProperty(p.getValue().getCommand() != null ?
                    p.getValue().getCommand().getValue() : null);
            return new OptionsBuilder()
                    .nameAndDescription("commandSecretField")
                    .addComp(new TextAreaComp(command), command)
                    .nonNull()
                    .bind(
                            () -> {
                                return Command.builder().command(command.get() != null ? new ShellScript(
                                        command.get()) : null).build();
                            },
                            p);
        }

        ShellScript command;

        @Override
        public void checkComplete() throws ValidationException {
            Validators.nonNull(command);
        }

        @Override
        public byte[] queryEncryptionSecret() throws Exception {
            try (var sc = ProcessControlProvider.get().createLocalProcessControl(true)) {
                try (var cc = sc.command(command).start()) {
                    cc.killOnTimeout(CountDown.of().start(30_000));
                    var out = cc.readRawBytesOrThrow();
                    return out;
                }
            }
        }
    }


    @JsonTypeName("httpRequest")
    @Builder
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
        public byte[] queryEncryptionSecret() throws Exception {
            var uri = URI.create(getUri());
            var request = java.net.http.HttpRequest.newBuilder()
                    .uri(uri)
                    .POST(java.net.http.HttpRequest.BodyPublishers.noBody())
                    .build();
            var result = HttpHelper.client().send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (result.statusCode() >= 400) {
                throw ErrorEventFactory.expected(new IOException(new String(result.body(), StandardCharsets.UTF_8)));
            }
            return result.body();
        }
    }
}
