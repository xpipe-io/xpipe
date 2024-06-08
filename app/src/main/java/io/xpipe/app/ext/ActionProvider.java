package io.xpipe.app.ext;

import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.util.ModuleLayerLoader;

import javafx.beans.value.ObservableValue;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ServiceLoader;

public interface ActionProvider {

    List<ActionProvider> ALL = new ArrayList<>();

    static void initProviders() {
        for (ActionProvider actionProvider : ALL) {
            try {
                actionProvider.init();
            } catch (Throwable t) {
                ErrorEvent.fromThrowable(t).handle();
            }
        }
    }

    default void init() throws Exception {}

    default String getId() {
        return null;
    }

    default String getProFeatureId() {
        return null;
    }

    default LauncherCallSite getLauncherCallSite() {
        return null;
    }

    default LeafDataStoreCallSite<?> getLeafDataStoreCallSite() {
        return null;
    }


    default BranchDataStoreCallSite<?> getBranchDataStoreCallSite() {
        return null;
    }

    default DefaultDataStoreCallSite<?> getDefaultDataStoreCallSite() {
        return null;
    }

    interface Action {

        void execute() throws Exception;
    }

    interface LauncherCallSite {

        String getId();

        Action createAction(URI uri) throws Exception;
    }

    interface XPipeLauncherCallSite extends LauncherCallSite {

        String getId();

        default Action createAction(URI uri) throws Exception {
            var args = new ArrayList<>(Arrays.asList(uri.getPath().substring(1).split("/")));
            args.addFirst(uri.getHost());
            return createAction(args);
        }

        Action createAction(List<String> args) throws Exception;
    }

    interface DefaultDataStoreCallSite<T extends DataStore> {

        Action createAction(DataStoreEntryRef<T> store);

        Class<T> getApplicableClass();

        default boolean isApplicable(DataStoreEntryRef<T> o) {
            return true;
        }
    }

    interface DataStoreCallSite<T extends DataStore> {

        default boolean isSystemAction() {
            return false;
        }

        default boolean isMajor(DataStoreEntryRef<T> o) {
            return false;
        }

        default boolean isApplicable(DataStoreEntryRef<T> o) {
            return true;
        }

        ObservableValue<String> getName(DataStoreEntryRef<T> store);

        String getIcon(DataStoreEntryRef<T> store);
    }

    interface BranchDataStoreCallSite<T extends DataStore> extends DataStoreCallSite<T> {

        default List<ActionProvider> getChildren() {
            return List.of();
        }
    }

    interface LeafDataStoreCallSite<T extends DataStore> extends DataStoreCallSite<T> {

        default boolean canLinkTo() {
            return false;
        }

        Action createAction(DataStoreEntryRef<T> store);

        Class<T> getApplicableClass();

        default boolean requiresValidStore() {
            return true;
        }
    }

    class Loader implements ModuleLayerLoader {

        @Override
        public void init(ModuleLayer layer) {
            ALL.addAll(ServiceLoader.load(layer, ActionProvider.class).stream()
                    .map(actionProviderProvider -> actionProviderProvider.get())
                    .toList());
        }
    }
}
