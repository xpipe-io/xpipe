package io.xpipe.extension.util;

import io.xpipe.core.store.DataStore;
import io.xpipe.extension.event.ErrorEvent;
import javafx.beans.value.ObservableValue;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

public interface ActionProvider {

    static List<ActionProvider> ALL = new ArrayList<>();

    public static void init(ModuleLayer layer) {
        if (ALL.size() == 0) {
            ALL.addAll(ServiceLoader.load(layer, ActionProvider.class).stream()
                               .map(p -> (ActionProvider) p.get())
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

    public static interface DataStoreCallSite<T extends DataStore> {

        Action createAction(T store);

        Class<T> getApplicableClass();

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
}
