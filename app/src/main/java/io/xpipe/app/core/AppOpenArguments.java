package io.xpipe.app.core;

import io.xpipe.app.browser.BrowserFullSessionModel;
import io.xpipe.app.core.mode.OperationMode;
import io.xpipe.app.ext.ActionProvider;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.storage.DataStorage;

import io.xpipe.core.store.FilePath;
import lombok.Value;

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

        var all = new ArrayList<ActionProvider.Action>();
        arguments.forEach(s -> {
            try {
                all.addAll(parseActions(s));
            } catch (Exception e) {
                ErrorEvent.fromThrowable(e).omit().handle();
            }
        });

        //        var requiresPlatform = all.stream().anyMatch(launcherInput -> launcherInput.requiresJavaFXPlatform());
        //        if (requiresPlatform) {
        //            OperationMode.switchToSyncIfPossible(OperationMode.GUI);
        //        }
        //        var hasGui = OperationMode.get() == OperationMode.GUI;

        all.forEach(launcherInput -> {
            try {
                launcherInput.execute();
            } catch (Exception e) {
                ErrorEvent.fromThrowable(e).omit().handle();
            }
        });
    }

    public static List<ActionProvider.Action> parseActions(String input) {
        if (input.startsWith("\"") && input.endsWith("\"")) {
            input = input.substring(1, input.length() - 1);
        }

        try {
            var uri = URI.create(input);
            var scheme = uri.getScheme();
            if (scheme != null) {
                if (scheme.equalsIgnoreCase("file")) {
                    var path = Path.of(uri);
                    return List.of(new BrowseFileAction(path));
                }

                var action = uri.getScheme();
                var found = ActionProvider.ALL.stream()
                        .filter(actionProvider -> actionProvider.getLauncherCallSite() != null
                                && actionProvider.getLauncherCallSite().getId().equalsIgnoreCase(action))
                        .findFirst();
                if (found.isPresent()) {
                    ActionProvider.Action a;
                    try {
                        a = found.get().getLauncherCallSite().createAction(uri);
                    } catch (Exception e) {
                        ErrorEvent.fromThrowable(e).omit().expected().handle();
                        return List.of();
                    }
                    return a != null ? List.of(a) : List.of();
                }
            }
        } catch (IllegalArgumentException ignored) {
        }

        try {
            var path = Path.of(input);
            if (Files.exists(path)) {
                return List.of(new BrowseFileAction(path));
            }
        } catch (InvalidPathException ignored) {
        }

        return List.of();
    }

    @Value
    public static class BrowseFileAction implements ActionProvider.Action {

        Path file;

        @Override
        public void execute() {
            if (!Files.exists(file)) {
                return;
            }

            if (!file.isAbsolute()) {
                return;
            }

            var dir = Files.isDirectory(file) ? file : file.getParent();
            AppLayoutModel.get().selectBrowser();
            BrowserFullSessionModel.DEFAULT.openFileSystemAsync(
                    DataStorage.get().local().ref(), model -> FilePath.of(dir.toString()), null);
        }
    }
}
