package io.xpipe.app.browser.action;

import io.xpipe.app.browser.BrowserEntry;
import io.xpipe.app.browser.OpenFileSystemModel;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.core.util.ModuleLayerLoader;
import javafx.scene.Node;
import javafx.scene.input.KeyCombination;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

public interface BrowserAction {

    static enum Category {
        CUSTOM,
        OPEN,
        NATIVE,
        COPY_PASTE,
        MUTATION
    }

    static List<BrowserAction> ALL = new ArrayList<>();

    public static List<LeafAction> getFlattened() {
        return ALL.stream()
                .map(browserAction -> browserAction instanceof LeafAction
                        ? List.of((LeafAction) browserAction)
                        : ((BranchAction) browserAction).getBranchingActions())
                .flatMap(List::stream)
                .toList();
    }

    default Node getIcon(OpenFileSystemModel model, List<BrowserEntry> entries) {
        return null;
    }

    default Category getCategory() {
        return null;
    }

    default KeyCombination getShortcut() {
        return null;
    }

    default boolean acceptsEmptySelection() {
        return false;
    }

    public abstract String getName(OpenFileSystemModel model, List<BrowserEntry> entries);

    public default boolean isApplicable(OpenFileSystemModel model, List<BrowserEntry> entries) {
        return true;
    }

    public default boolean isActive(OpenFileSystemModel model, List<BrowserEntry> entries) {
        return true;
    }

    public static class Loader implements ModuleLayerLoader {

        @Override
        public void init(ModuleLayer layer) {
            ALL.addAll(ServiceLoader.load(layer, BrowserAction.class).stream()
                    .map(actionProviderProvider -> actionProviderProvider.get())
                    .filter(provider -> {
                        try {
                            return true;
                        } catch (Throwable e) {
                            ErrorEvent.fromThrowable(e).handle();
                            return false;
                        }
                    })
                    .toList());
        }

        @Override
        public boolean requiresFullDaemon() {
            return true;
        }

        @Override
        public boolean prioritizeLoading() {
            return false;
        }
    }
}
