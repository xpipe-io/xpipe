package io.xpipe.app.ext;

import io.xpipe.app.browser.BrowserFullSessionModel;
import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.base.MarkdownComp;
import io.xpipe.app.comp.store.StoreEntryComp;
import io.xpipe.app.comp.store.StoreEntryWrapper;
import io.xpipe.app.comp.store.StoreSection;
import io.xpipe.app.comp.store.StoreSectionComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.resources.AppImages;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.core.store.DataStore;

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableValue;

import java.util.List;
import java.util.UUID;

public interface DataStoreProvider {

    default boolean canMoveCategories() {
        return true;
    }

    default UUID getTargetCategory(DataStore store, UUID target) {
        return target;
    }

    default int getOrderPriority() {
        return 0;
    }

    default boolean showProviderChoice() {
        return true;
    }

    default boolean shouldShow(StoreEntryWrapper w) {
        return true;
    }

    default void onParentRefresh(DataStoreEntry entry) {}

    default void onChildrenRefresh(DataStoreEntry entry) {}

    default ObservableBooleanValue busy(StoreEntryWrapper wrapper) {
        return new SimpleBooleanProperty(false);
    }

    default boolean editByDefault() {
        return false;
    }

    default boolean alwaysShowSummary() {
        return false;
    }

    default void validate() {
        if (getUsageCategory() == null) {
            throw ExtensionException.corrupt("Provider %s does not have the usage category".formatted(getId()));
        }
    }

    default ActionProvider.Action activateAction(DataStoreEntry store) {
        return null;
    }

    default ActionProvider.Action launchAction(DataStoreEntry store) {
        return null;
    }

    default ActionProvider.Action browserAction(
            BrowserFullSessionModel sessionModel, DataStoreEntry store, BooleanProperty busy) {
        return null;
    }

    default String displayName(DataStoreEntry entry) {
        return entry.getName();
    }

    default List<String> getSearchableTerms(DataStore store) {
        return List.of();
    }

    default StoreEntryComp customEntryComp(StoreSection s, boolean preferLarge) {
        return StoreEntryComp.create(s, null, preferLarge);
    }

    default StoreSectionComp customSectionComp(StoreSection section, boolean topLevel) {
        return new StoreSectionComp(section, topLevel);
    }

    default boolean shouldShowScan() {
        return true;
    }

    default Comp<?> stateDisplay(StoreEntryWrapper w) {
        return Comp.empty();
    }

    default boolean canConnectDuringCreation() {
        return false;
    }

    default Comp<?> createInsightsComp(ObservableValue<DataStore> store) {
        var content = Bindings.createStringBinding(
                () -> {
                    if (store.getValue() == null
                            || !store.getValue().isComplete()
                            || !getStoreClasses().contains(store.getValue().getClass())) {
                        return null;
                    }

                    try {
                        return "## Insights\n\n" + createInsightsMarkdown(store.getValue());
                    } catch (Exception ex) {
                        ErrorEvent.fromThrowable(ex).handle();
                        return "?";
                    }
                },
                store);
        return new MarkdownComp(content, s -> s, true)
                .apply(struc -> struc.get().setPrefWidth(450))
                .apply(struc -> struc.get().setPrefHeight(250));
    }

    default String createInsightsMarkdown(DataStore store) {
        return null;
    }

    default DataStoreCreationCategory getCreationCategory() {
        return null;
    }

    default DataStoreUsageCategory getUsageCategory() {
        var cc = getCreationCategory();
        if (cc == DataStoreCreationCategory.SHELL || cc == DataStoreCreationCategory.HOST) {
            return DataStoreUsageCategory.SHELL;
        }

        if (cc == DataStoreCreationCategory.COMMAND) {
            return DataStoreUsageCategory.COMMAND;
        }

        if (cc == DataStoreCreationCategory.SCRIPT) {
            return DataStoreUsageCategory.SCRIPT;
        }

        if (cc == DataStoreCreationCategory.SERIAL) {
            return DataStoreUsageCategory.SERIAL;
        }

        return null;
    }

    default boolean canClone() {
        return getCreationCategory() != null;
    }

    default DataStoreEntry getDisplayParent(DataStoreEntry store) {
        return getSyntheticParent(store);
    }

    default DataStoreEntry getSyntheticParent(DataStoreEntry store) {
        return null;
    }

    default GuiDialog guiDialog(DataStoreEntry entry, Property<DataStore> store) {
        return null;
    }

    default boolean preInit() {
        return true;
    }

    default void init() {}

    default void reset() {}

    default boolean isShareableFromLocalMachine() {
        return false;
    }

    default boolean isShareable(DataStoreEntry entry) {
        return true;
    }

    default String summaryString(StoreEntryWrapper wrapper) {
        return null;
    }

    default ObservableValue<String> informationString(StoreSection section) {
        return new SimpleStringProperty(null);
    }

    default ObservableValue<String> i18n(String key) {
        return AppI18n.observable(getId() + "." + key);
    }

    default ObservableValue<String> displayName() {
        return i18n("displayName");
    }

    default ObservableValue<String> displayDescription() {
        return i18n("displayDescription");
    }

    default String getModuleName() {
        var n = getClass().getModule().getName();
        var i = n.lastIndexOf('.');
        return i != -1 ? n.substring(i + 1) : n;
    }

    default String getDisplayIconFileName(DataStore store) {
        var png = getModuleName() + ":" + getId() + "_icon.png";
        if (AppImages.hasNormalImage(png)) {
            return png;
        }

        return getModuleName() + ":" + getId() + "_icon.svg";
    }

    default DataStore defaultStore() {
        return null;
    }

    String getId();

    List<Class<?>> getStoreClasses();
}
