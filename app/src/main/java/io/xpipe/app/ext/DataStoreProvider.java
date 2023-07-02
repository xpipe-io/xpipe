package io.xpipe.app.ext;

import io.xpipe.app.comp.base.MarkdownComp;
import io.xpipe.app.comp.base.SystemStateComp;
import io.xpipe.app.comp.storage.store.StandardStoreEntryComp;
import io.xpipe.app.comp.storage.store.StoreSectionComp;
import io.xpipe.app.comp.storage.store.StoreEntryWrapper;
import io.xpipe.app.comp.storage.store.StoreSection;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.core.dialog.Dialog;
import io.xpipe.core.store.*;
import io.xpipe.core.util.JacksonizedValue;
import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.beans.value.ObservableValue;

import java.util.List;

public interface DataStoreProvider {

    default ModuleInstall getRequiredAdditionalInstallation() {
        return null;
    }

    default void validate() {
        getCategory();
        for (Class<?> storeClass : getStoreClasses()) {
            if (!JacksonizedValue.class.isAssignableFrom(storeClass)) {
                throw new ExtensionException(
                        String.format("Store class %s is not a Jacksonized value", storeClass.getSimpleName()));
            }
        }
    }

    default Comp<?> customDisplay(StoreSection s) {
        return new StandardStoreEntryComp(s.getWrapper(), null);
    }

    default Comp<?> customContainer(StoreSection section) {
        return new StoreSectionComp(section);
    }

    default String failureInfo() {
        return null;
    }

    default Comp<?> stateDisplay(StoreEntryWrapper w) {
        var state = Bindings.createObjectBinding(
                () -> {
                    return w.getState().getValue() == DataStoreEntry.State.COMPLETE_BUT_INVALID
                            ? SystemStateComp.State.FAILURE
                            : w.getState().getValue() == DataStoreEntry.State.COMPLETE_AND_VALID
                                    ? SystemStateComp.State.SUCCESS
                                    : SystemStateComp.State.OTHER;
                },
                w.getState());
        var name = Bindings.createStringBinding(
                () -> {
                    return w.getState().getValue() == DataStoreEntry.State.COMPLETE_BUT_INVALID ? "stop" : "start";
                },
                w.getState());
        return new SystemStateComp(name, state);
    }

    default Comp<?> createInsightsComp(ObservableValue<DataStore> store) {
        var content = Bindings.createStringBinding(
                () -> {
                    if (store.getValue() == null
                            || !store.getValue().isComplete()
                            || !getStoreClasses().contains(store.getValue().getClass())) {
                        return null;
                    }

                    return createInsightsMarkdown(store.getValue());
                },
                store);
        var markdown = new MarkdownComp(content, s -> s)
                .apply(struc -> struc.get().setPrefWidth(450))
                .apply(struc -> struc.get().setPrefHeight(200));
        return markdown;
    }

    default String createInsightsMarkdown(DataStore store) {
        return null;
    }

    default DataCategory getCategory() {
        var c = getStoreClasses().get(0);
        if (StreamDataStore.class.isAssignableFrom(c)) {
            return DataCategory.STREAM;
        }

        if (FileSystem.class.isAssignableFrom(c) || ShellStore.class.isAssignableFrom(c)) {
            return DataCategory.SHELL;
        }

        throw new ExtensionException("Provider " + getId() + " has no set category");
    }

    default DisplayCategory getDisplayCategory() {
        var category = getCategory();
        if (category.equals(DataCategory.SHELL)) {
            return DisplayCategory.HOST;
        }
        if (category.equals(DataCategory.DATABASE)) {
            return DisplayCategory.DATABASE;
        }
        return DisplayCategory.OTHER;
    }

    default DataStore getLogicalParent(DataStore store) {
        return null;
    }

    default DataStore getDisplayParent(DataStore store) {
        return getLogicalParent(store);
    }

    default GuiDialog guiDialog(Property<DataStore> store) {
        return null;
    }

    default boolean init() throws Exception {
        return true;
    }

    default void storageInit() throws Exception {}

    default boolean isShareable() {
        return false;
    }

    String queryInformationString(DataStore store, int length) throws Exception;

    default String queryInvalidInformationString(DataStore store, int length) throws Exception {
        return null;
    }

    String toSummaryString(DataStore store, int length);

    default String i18n(String key) {
        return AppI18n.get(getId() + "." + key);
    }

    default String i18nKey(String key) {
        return getId() + "." + key;
    }

    default String getDisplayName() {
        return i18n("displayName");
    }

    default String getDisplayDescription() {
        return i18n("displayDescription");
    }

    default String getModuleName() {
        var n = getClass().getModule().getName();
        var i = n.lastIndexOf('.');
        return i != -1 ? n.substring(i + 1) : n;
    }

    default String getDisplayIconFileName(DataStore store) {
        return getModuleName() + ":" + getId() + "_icon.png";
    }

    default Dialog dialogForStore(DataStore store) {
        return null;
    }

    default boolean requiresFrequentRefresh() {
        return getStoreClasses().stream().anyMatch(aClass -> FixedHierarchyStore.class.isAssignableFrom(aClass));
    }

    default DataStore defaultStore() {
        return null;
    }

    List<String> getPossibleNames();

    default String getId() {
        return getPossibleNames().get(0);
    }

    List<Class<?>> getStoreClasses();

    default boolean canManuallyCreate() {
        return true;
    }

    enum DataCategory {
        STREAM,
        SHELL,
        DATABASE
    }

    enum DisplayCategory {
        HOST,
        DATABASE,
        SHELL,
        COMMAND,
        OTHER
    }
}
