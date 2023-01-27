package io.xpipe.app.launcher;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.xpipe.app.comp.source.GuiDsCreatorMultiStep;
import io.xpipe.app.comp.source.store.GuiDsStoreCreator;
import io.xpipe.app.core.mode.OperationMode;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.core.impl.FileStore;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.util.JacksonMapper;
import io.xpipe.core.util.SecretValue;
import io.xpipe.extension.DataSourceProvider;
import io.xpipe.extension.event.ErrorEvent;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Value;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public abstract class LauncherInput {

    public static void handle(List<String> arguments) {
        var all = new ArrayList<LauncherInput>();
        arguments.forEach(s -> {
            try {
                all.addAll(of(s));
            } catch (Exception e) {
                ErrorEvent.fromThrowable(e).omit().handle();
            }
        });

        var requiresPlatform = all.stream().anyMatch(launcherInput -> launcherInput.requiresPlatform());
        if (requiresPlatform) {
            OperationMode.switchTo(OperationMode.GUI);
        }
        var hasGui = OperationMode.get() == OperationMode.GUI;

        all.forEach(launcherInput -> {
            if (!hasGui && launcherInput.requiresPlatform()) {
                return;
            }

            try {
                launcherInput.execute();
            } catch (Exception e) {
                ErrorEvent.fromThrowable(e).omit().handle();
            }
        });
    }

    public abstract void execute() throws Exception;

    public abstract boolean requiresPlatform();

    public static List<LauncherInput> of(String input) {
        if (input.startsWith("\"") && input.endsWith("\"")) {
            input = input.substring(1, input.length() - 1);
        }

        try {
            var uri = URI.create(input);
            var scheme = uri.getScheme();
            if (scheme != null) {
                if (scheme.equalsIgnoreCase("file")) {
                    var path = Path.of(uri);
                    return List.of(new LocalFileInput(path));
                }

                if (scheme.equalsIgnoreCase("xpipe")) {
                    var action = uri.getAuthority();
                    var args = Arrays.asList(uri.getPath().split("/"));

                    var a = switch (action.toLowerCase()) {
                        case "add" -> new AddActionInput(args);
                        default -> throw new IllegalStateException("Unexpected value: " + action);
                    };

                    return List.of(a);
                }
            }
        } catch (IllegalArgumentException ignored) {
        }

        try {
            var path = Path.of(input);
            if (Files.exists(path)) {
                return List.of(new LocalFileInput(path));
            }
        } catch (InvalidPathException ignored) {
        }


        return List.of();
    }

    @Value
    @EqualsAndHashCode(callSuper = true)
    public static class LocalFileInput extends LauncherInput {

        Path file;

        @Override
        public void execute() {
            if (!Files.exists(file)) {
                return;
            }

            if (!file.isAbsolute()) {
                return;
            }

            GuiDsCreatorMultiStep.showForStore(DataSourceProvider.Category.STREAM, FileStore.local(file), null);
        }

        @Override
        public boolean requiresPlatform() {
            return true;
        }
    }

    public static abstract class ActionInput extends LauncherInput {

        @Getter
        private final List<String> args;

        protected ActionInput(List<String> args) {
            this.args = args;
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = true)
    public static class AddActionInput extends ActionInput {

        public AddActionInput(List<String> args) {
            super(args);
        }

        @Override
        public void execute() throws JsonProcessingException {
            var type = getArgs().get(1);
            var storeString = SecretValue.ofSecret(getArgs().get(2));
            var store = JacksonMapper.parse(storeString.getSecretValue(), DataStore.class);
            if (store == null) {
                return;
            }

            var entry = DataStoreEntry.createNew(UUID.randomUUID(),"", store);
            GuiDsStoreCreator.showEdit(entry);
        }

        @Override
        public boolean requiresPlatform() {
            return true;
        }
    }
}
