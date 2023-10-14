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

    class Loader implements ModuleLayerLoader {

        @Override
        public void init(ModuleLayer layer) {
            ALL.addAll(ServiceLoader.load(layer, ActionProvider.class).stream()
                    .map(actionProviderProvider -> actionProviderProvider.get())
                    .filter(provider -> {
                        try {
                            return provider.isActive();
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

    interface Action {

        boolean requiresJavaFXPlatform();

        void execute() throws Exception;
    }

    default String getId() {
        return null;
    }

    default boolean isActive() {
        return true;
    }

    interface LauncherCallSite {

        String getId();

        Action createAction(URI uri) throws Exception;
    }

    interface XPipeLauncherCallSite extends LauncherCallSite {

        String getId();

        default Action createAction(URI uri) throws Exception {
            var args = new ArrayList<>(Arrays.asList(uri.getPath().substring(1).split("/")));
            args.add(0, uri.getHost());
            return createAction(args);
        }

        Action createAction(List<String> args) throws Exception;
    }

    default LauncherCallSite getLauncherCallSite() {
        return null;
    }

    default DataStoreCallSite<?> getDataStoreCallSite() {
        return null;
    }

    default DefaultDataStoreCallSite<?> getDefaultDataStoreCallSite() {
        return null;
    }

    interface DefaultDataStoreCallSite<T extends DataStore> {

        Action createAction(DataStoreEntryRef<T> store);

        Class<T> getApplicableClass();

        default boolean isApplicable(DataStoreEntryRef<T> o) {
            return true;
        }
    }

    interface DataStoreCallSite<T extends DataStore> {

        enum ActiveType {
            ONLY_SHOW_IF_ENABLED,
            ALWAYS_SHOW,
            ALWAYS_ENABLE
        }

        default boolean isSystemAction() {
            return false;
        }

        default boolean canLinkTo() {
            return false;
        }

        Action createAction(DataStoreEntryRef<T> store);

        Class<T> getApplicableClass();

        default boolean isMajor(DataStoreEntryRef<T> o) {
            return false;
        }

        default boolean isApplicable(DataStoreEntryRef<T> o) {
            return true;
        }

        ObservableValue<String> getName(DataStoreEntryRef<T> store);

        String getIcon(DataStoreEntryRef<T> store);

        default ActiveType activeType() {
            return ActiveType.ONLY_SHOW_IF_ENABLED;
        }
    }
}
