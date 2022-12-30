package io.xpipe.extension;

import io.xpipe.core.source.DataSource;
import io.xpipe.extension.event.ErrorEvent;
import javafx.beans.value.ObservableValue;
import javafx.scene.layout.Region;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

public interface DataSourceActionProvider<T extends DataSource<?>> {

    static List<DataSourceActionProvider<?>> ALL = new ArrayList<>();

    public static void init(ModuleLayer layer) {
        if (ALL.size() == 0) {
            ALL.addAll(ServiceLoader.load(layer, DataSourceActionProvider.class).stream()
                               .map(p -> (DataSourceActionProvider<?>) p.get())
                               .filter(provider -> {
                                   try {
                                       return provider.isActive();
                                   } catch (Exception e) {
                                       ErrorEvent.fromThrowable(e).handle();
                                       return false;
                                   }
                               })
                               .toList());
        }
    }

    Class<T> getApplicableClass();

    default boolean isActive() throws Exception {
        return true;
    }

    default boolean isApplicable(T o) throws Exception {
        return true;
    }

    default void applyToRegion(T store, Region region) {
    }

    ObservableValue<String> getName(T store);

    String getIcon(T store);

    default void execute(T store) throws Exception {
    }
}
