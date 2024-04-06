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

    List<BrowserAction> ALL = new ArrayList<>();

    static List<LeafAction> getFlattened(OpenFileSystemModel model, List<BrowserEntry> entries) {
        return ALL.stream()
                .map(browserAction -> browserAction instanceof LeafAction
                        ? List.of((LeafAction) browserAction)
                        : ((BranchAction) browserAction).getBranchingActions(model, entries))
                .flatMap(List::stream)
                .toList();
    }

    static LeafAction byId(String id, OpenFileSystemModel model, List<BrowserEntry> entries) {
        return getFlattened(model, entries).stream()
                .filter(browserAction -> id.equals(browserAction.getId()))
                .findAny()
                .orElseThrow();
    }

    default void init(OpenFileSystemModel model) throws Exception {}

    default String getProFeatureId() {
        return null;
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

    String getName(OpenFileSystemModel model, List<BrowserEntry> entries);

    default boolean isApplicable(OpenFileSystemModel model, List<BrowserEntry> entries) {
        return true;
    }

    default boolean automaticallyResolveLinks() {
        return true;
    }

    default boolean isActive(OpenFileSystemModel model, List<BrowserEntry> entries) {
        return true;
    }

    enum Category {
        CUSTOM,
        OPEN,
        NATIVE,
        COPY_PASTE,
        MUTATION
    }

    class Loader implements ModuleLayerLoader {

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
        public boolean prioritizeLoading() {
            return false;
        }
    }
}
