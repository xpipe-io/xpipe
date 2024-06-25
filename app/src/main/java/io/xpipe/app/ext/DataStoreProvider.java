package io.xpipe.app.ext;

import io.xpipe.app.browser.session.BrowserSessionModel;
import io.xpipe.app.comp.base.MarkdownComp;
import io.xpipe.app.comp.store.StoreEntryComp;
import io.xpipe.app.comp.store.StoreEntryWrapper;
import io.xpipe.app.comp.store.StoreSection;
import io.xpipe.app.comp.store.StoreSectionComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppImages;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.util.JacksonizedValue;

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableValue;

import java.util.List;

public interface DataStoreProvider {

    default boolean shouldShow(StoreEntryWrapper w) {
        return true;
    }

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
        for (Class<?> storeClass : getStoreClasses()) {
            if (!JacksonizedValue.class.isAssignableFrom(storeClass)) {
                throw new ExtensionException(
                        String.format("Store class %s is not a Jacksonized value", storeClass.getSimpleName()));
            }

            if (getUsageCategory() == null) {
                throw new ExtensionException("Provider %s does not have the usage category".formatted(getId()));
            }
        }
    }

    default ActionProvider.Action activateAction(DataStoreEntry store) {
        return null;
    }

    default ActionProvider.Action launchAction(DataStoreEntry store) {
        return null;
    }

    default ActionProvider.Action browserAction(
            BrowserSessionModel sessionModel, DataStoreEntry store, BooleanProperty busy) {
        return null;
    }

    default String browserDisplayName(DataStore store) {
        var e = DataStorage.get().getStoreDisplayName(store);
        return e.orElse("?");
    }

    default List<String> getSearchableTerms(DataStore store) {
        return List.of();
    }

    default boolean shouldEdit() {
        return false;
    }

    default StoreEntryComp customEntryComp(StoreSection s, boolean preferLarge) {
        return StoreEntryComp.create(s.getWrapper(), null, preferLarge);
    }

    default StoreSectionComp customSectionComp(StoreSection section, boolean topLevel) {
        return new StoreSectionComp(section, topLevel);
    }

    default boolean canHaveSubShells() {
        return true;
    }

    default boolean shouldHaveChildren() {
        return canHaveSubShells();
    }

    default Comp<?> stateDisplay(StoreEntryWrapper w) {
        return Comp.empty();
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
        return new MarkdownComp(content, s -> s)
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

        if (cc == DataStoreCreationCategory.DATABASE) {
            return DataStoreUsageCategory.DATABASE;
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

    default ObservableValue<String> informationString(StoreEntryWrapper wrapper) {
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

    List<String> getPossibleNames();

    default String getId() {
        return getPossibleNames().getFirst();
    }

    List<Class<?>> getStoreClasses();
}
