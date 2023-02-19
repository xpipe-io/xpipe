package io.xpipe.app.ext;

import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.util.ModuleLayerLoader;
import io.xpipe.core.source.DataSource;
import io.xpipe.core.store.DataStore;
import javafx.beans.value.ObservableValue;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

public interface ActionProvider {

    static List<ActionProvider> ALL = new ArrayList<>();

    public static class Loader implements ModuleLayerLoader {

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

        boolean requiresPlatform();

        void execute() throws Exception;
    }

    default boolean isActive() throws Exception {
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

    default DataSourceCallSite<?> getDataSourceCallSite() {
        return null;
    }

    public static interface DataStoreCallSite<T extends DataStore> {

        Action createAction(T store);

        Class<T> getApplicableClass();

        default boolean isDefault() {
            return false;
        }

        default boolean isMajor() {
            return false;
        }

        default boolean isApplicable(T o) throws Exception {
            return true;
        }

        ObservableValue<String> getName(T store);

        String getIcon(T store);

        default boolean showIfDisabled() {
            return true;
        }
    }

    public static interface DataSourceCallSite<T extends DataSource<?>> {

        Action createAction(T source);

        Class<T> getApplicableClass();

        default boolean isMajor() {
            return false;
        }

        default boolean isApplicable(T o) throws Exception {
            return true;
        }

        ObservableValue<String> getName(T source);

        String getIcon(T source);

        default boolean showIfDisabled() {
            return true;
        }
    }
}
