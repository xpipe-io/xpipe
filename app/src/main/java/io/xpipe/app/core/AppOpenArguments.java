package io.xpipe.app.core;

import io.xpipe.app.action.AbstractAction;
import io.xpipe.app.action.ActionProvider;
import io.xpipe.app.action.LauncherUrlProvider;
import io.xpipe.app.browser.action.impl.OpenDirectoryActionProvider;
import io.xpipe.app.core.mode.OperationMode;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.core.FilePath;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class AppOpenArguments {

    private static final List<String> bufferedArguments = new ArrayList<>();

    public static synchronized void init() {
        handleImpl(AppProperties.get().getArguments().getOpenArgs());
        handleImpl(bufferedArguments);
        bufferedArguments.clear();
    }

    public static synchronized void handle(List<String> arguments) {
        if (OperationMode.isInShutdown()) {
            return;
        }

        if (OperationMode.isInStartup()) {
            TrackEvent.withDebug("Buffering open arguments").elements(arguments).handle();
            bufferedArguments.addAll(arguments);
            return;
        }

        handleImpl(arguments);
    }

    private static synchronized void handleImpl(List<String> arguments) {
        if (arguments.size() == 0) {
            return;
        }

        TrackEvent.withDebug("Handling arguments").elements(arguments).handle();

        var all = new ArrayList<AbstractAction>();
        arguments.forEach(s -> {
            try {
                all.addAll(parseActions(s));
            } catch (Exception e) {
                ErrorEventFactory.fromThrowable(e).omit().handle();
            }
        });

        //        var requiresPlatform = all.stream().anyMatch(launcherInput -> launcherInput.requiresJavaFXPlatform());
        //        if (requiresPlatform) {
        //            OperationMode.switchToSyncIfPossible(OperationMode.GUI);
        //        }
        //        var hasGui = OperationMode.get() == OperationMode.GUI;

        all.forEach(launcherInput -> {
            launcherInput.executeAsync();
        });
    }

    public static List<? extends AbstractAction> parseActions(String input) {
        if (input == null || input.isBlank()) {
            return List.of();
        }

        if (input.startsWith("\"") && input.endsWith("\"")) {
            input = input.substring(1, input.length() - 1);
        }

        try {
            var uri = URI.create(input);
            var scheme = uri.getScheme();
            if (scheme != null) {
                var action = uri.getScheme();
                var found = ActionProvider.ALL.stream()
                        .filter(actionProvider -> actionProvider instanceof LauncherUrlProvider lcs
                                && lcs.getScheme().equalsIgnoreCase(action))
                        .findFirst();
                if (found.isPresent()) {
                    AbstractAction a;
                    try {
                        a = ((LauncherUrlProvider) found.get()).createAction(uri);
                    } catch (Exception e) {
                        ErrorEventFactory.fromThrowable(e).omit().expected().handle();
                        return List.of();
                    }
                    return a != null ? List.of(a) : List.of();
                }
            }
        } catch (IllegalArgumentException ignored) {
        }

        try {
            var path = Path.of(input);
            if (Files.isRegularFile(path)) {
                path = path.getParent();
            }

            if (Files.exists(path)) {
                return List.of(OpenDirectoryActionProvider.Action.builder()
                        .ref(DataStorage.get().local().ref())
                        .files(List.of(FilePath.of(path)))
                        .build());
            }
        } catch (InvalidPathException ignored) {
        }

        return List.of();
    }
}
