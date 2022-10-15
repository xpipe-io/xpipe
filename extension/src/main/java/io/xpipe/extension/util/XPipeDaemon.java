package io.xpipe.extension.util;

import io.xpipe.core.source.DataSource;
import io.xpipe.core.source.DataSourceType;
import io.xpipe.core.store.DataStore;
import io.xpipe.extension.DataSourceProvider;
import io.xpipe.extension.DataStoreProvider;
import io.xpipe.fxcomps.Comp;
import javafx.beans.property.Property;
import javafx.beans.value.ObservableValue;
import javafx.scene.image.Image;

import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.Predicate;

public interface XPipeDaemon {

    static XPipeDaemon getInstance() {
        return ServiceLoader.load(XPipeDaemon.class).findFirst().orElseThrow();
    }

    List<DataStore> getNamedStores();

    public Image image(String file);

    <T extends Comp<?> & Validatable> T streamStoreChooser(
            Property<DataStore> storeProperty,
            Property<DataSourceProvider<?>> provider,
            boolean showAnonymous,
            boolean showSaved);

    <T extends Comp<?> & Validatable> T namedStoreChooser(
            ObservableValue<Predicate<DataStore>> filter,
            Property<? extends DataStore> selected,
            DataStoreProvider.Category category);

    Comp<?> namedSourceChooser(
            ObservableValue<Predicate<DataSource<?>>> filter,
            Property<? extends DataSource<?>> selected,
            DataSourceProvider.Category category);

    <T extends Comp<?> & Validatable> T sourceProviderChooser(
            Property<DataSourceProvider<?>> provider, DataSourceProvider.Category category, DataSourceType filter);

    Optional<DataStore> getNamedStore(String name);

    Optional<DataSource<?>> getSource(String id);

    Optional<String> getStoreName(DataStore store);

    Optional<String> getSourceId(DataSource<?> source);
}
