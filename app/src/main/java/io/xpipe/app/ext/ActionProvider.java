package io.xpipe.app.ext;

import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.core.source.DataSource;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.util.ModuleLayerLoader;
import javafx.beans.value.ObservableValue;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

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
                    .collect(Collectors.toSet()));
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

    default boolean isActive() {
        return true;
    }

    interface LauncherCallSite {

        String getId();

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

    default DataSourceCallSite<?> getDataSourceCallSite() {
        return null;
    }

    interface DefaultDataStoreCallSite<T extends DataStore> {

        Action createAction(T store);

        Class<T> getApplicableClass();

        default boolean isApplicable(T o) {
            return true;
        }
    }

    interface DataStoreCallSite<T extends DataStore> {

        enum ActiveType {
            ONLY_SHOW_IF_ENABLED,
            ALWAYS_SHOW,
            ALWAYS_ENABLE
        }

        Action createAction(T store);

        Class<T> getApplicableClass();

        default boolean isMajor() {
            return false;
        }

        default boolean isApplicable(T o) {
            return true;
        }

        ObservableValue<String> getName(T store);

        String getIcon(T store);

        default ActiveType activeType() {
            return ActiveType.ONLY_SHOW_IF_ENABLED;
        }
    }

    interface DataSourceCallSite<T extends DataSource<?>> {

        Action createAction(T source);

        Class<T> getApplicableClass();

        default boolean isMajor() {
            return false;
        }

        default boolean isApplicable(T o) {
            return true;
        }

        ObservableValue<String> getName(T source);

        String getIcon(T source);

        default boolean showIfDisabled() {
            return true;
        }
    }
}
