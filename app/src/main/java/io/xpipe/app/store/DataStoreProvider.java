package io.xpipe.app.store;

import io.xpipe.app.browser.BrowserFullSessionModel;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppImages;
import io.xpipe.app.ext.ExtensionException;
import io.xpipe.app.ext.ModuleLayerLoader;
import io.xpipe.app.hub.creation.StoreCreationModel;
import io.xpipe.app.hub.entry.StoreEntryComp;
import io.xpipe.app.hub.entry.StoreEntryInformation;
import io.xpipe.app.hub.entry.StoreEntryWrapper;
import io.xpipe.app.hub.section.StoreSection;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.storage.DataStoreCategory;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.DocumentationLink;
import io.xpipe.app.util.FailableRunnable;
import io.xpipe.app.util.GuiDialog;
import io.xpipe.app.webtop.WebtopApp;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableValue;

import java.util.*;

public interface DataStoreProvider {

    List<DataStoreProvider> ALL = new ArrayList<>();

    static Optional<DataStoreProvider> byId(String id) {
        if (ALL.isEmpty()) {
            throw new IllegalStateException("Not initialized");
        }

        return ALL.stream().filter(d -> d.getId().equalsIgnoreCase(id)).findAny();
    }

    @SuppressWarnings("unchecked")
    static <T extends DataStoreProvider> Optional<T> byStoreIfPresent(DataStore store) {
        if (ALL.isEmpty()) {
            throw new IllegalStateException("Not initialized");
        }

        return (Optional<T>) ALL.stream()
                .filter(d -> d.getStoreClasses().contains(store.getClass()))
                .findAny();
    }

    static <T extends DataStoreProvider> T byStore(DataStore store) {
        return DataStoreProvider.<T>byStoreIfPresent(store)
                .orElseThrow(() -> new IllegalArgumentException("Unknown store class"));
    }

    static List<DataStoreProvider> getAll() {
        return ALL;
    }

    default WebtopApp getRequiredWebtopApp(DataStoreEntry entry) {
        return null;
    }

    default boolean allowCreation() {
        return true;
    }

    default boolean showIncompleteInfo() {
        return false;
    }

    default boolean includeInConnectionCount() {
        return getUsageCategory() != DataStoreUsageCategory.GROUP;
    }

    default boolean canConfigure() {
        var m = getClass().getDeclaredMethods();
        return Arrays.stream(m).anyMatch(method -> method.getName().equals("guiDialog"));
    }

    default DocumentationLink getHelpLink() {
        return null;
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

    default Comparator<StoreSection> getComparator() {
        return null;
    }

    default void onParentRefresh(DataStoreEntry entry) {}

    default void onChildrenRefresh(DataStoreEntry entry) {}

    default ObservableBooleanValue busy(StoreEntryWrapper wrapper) {
        return new SimpleBooleanProperty(false);
    }

    default void validate() {
        if (getUsageCategory() == null) {
            throw ExtensionException.corrupt("Provider %s does not have the usage category".formatted(getId()));
        }
    }

    default FailableRunnable<Exception> activateAction(DataStoreEntry store) {
        return null;
    }

    default FailableRunnable<Exception> launch(DataStoreEntry store) {
        return null;
    }

    default FailableRunnable<Exception> launchBrowser(
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

    default boolean shouldShowScan() {
        return true;
    }

    default boolean canConnectDuringCreation() {
        return false;
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

        if (cc == DataStoreCreationCategory.SCRIPT || cc == DataStoreCreationCategory.SCRIPT_SOURCE) {
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

    default DataStoreEntryRef<?> getDisplayParent(DataStoreEntry store) {
        return getSyntheticParent(store);
    }

    default DataStoreEntryRef<?> getSyntheticParent(DataStoreEntry store) {
        return null;
    }

    default GuiDialog guiDialog(StoreCreationModel model, Property<DataStore> store) {
        return null;
    }

    default boolean isSyncableFromLocalMachine() {
        return false;
    }

    default boolean isSyncable(DataStoreEntry entry) {
        return true;
    }

    default String summaryString(StoreEntryWrapper wrapper) {
        return null;
    }

    default StoreEntryInformation buildInformation(StoreSection section) {
        return null;
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
        if (AppImages.hasImage(png)) {
            return png;
        }

        return getModuleName() + ":" + getId() + "_icon.svg";
    }

    default DataStore defaultStore(DataStoreCategory category) {
        return null;
    }

    String getId();

    List<Class<?>> getStoreClasses();

    class Loader implements ModuleLayerLoader {

        @Override
        public void init(ModuleLayer layer) {
            TrackEvent.info("Loading extension providers ...");
            ALL.addAll(ServiceLoader.load(layer, DataStoreProvider.class).stream()
                    .map(ServiceLoader.Provider::get)
                    .toList());
            ALL.removeIf(p -> {
                try {
                    p.validate();
                    return false;
                } catch (Throwable e) {
                    ErrorEventFactory.fromThrowable(e).handle();
                    return true;
                }
            });
        }

        @Override
        public boolean initForCli() {
            return false;
        }
    }
}
