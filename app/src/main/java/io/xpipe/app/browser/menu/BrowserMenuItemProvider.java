package io.xpipe.app.browser.menu;

import io.xpipe.app.action.ActionProvider;
import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.file.BrowserFileSystemTabModel;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCombination;

import java.util.List;

public interface BrowserMenuItemProvider extends ActionProvider {

    MenuItem toMenuItem(BrowserFileSystemTabModel model, List<BrowserEntry> selected);

    default void init(BrowserFileSystemTabModel model) throws Exception {}

    default boolean automaticallyResolveLinks() {
        return true;
    }

    default List<BrowserEntry> resolveFilesIfNeeded(List<BrowserEntry> selected) {
        return automaticallyResolveLinks()
                ? selected.stream()
                .map(browserEntry ->
                        new BrowserEntry(browserEntry.getRawFileEntry().resolved(), browserEntry.getModel()))
                .toList()
                : selected;
    }

    default Node getIcon(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return null;
    }

    default Category getCategory() {
        return null;
    }

    default KeyCombination getShortcut() {
        return null;
    }

    ObservableValue<String> getName(BrowserFileSystemTabModel model, List<BrowserEntry> entries);

    default boolean acceptsEmptySelection() {
        return false;
    }

    default boolean isApplicable(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return true;
    }

    default boolean isActive(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return true;
    }

    enum Category {
        CUSTOM,
        OPEN,
        NATIVE,
        COPY_PASTE,
        MUTATION
    }
}
