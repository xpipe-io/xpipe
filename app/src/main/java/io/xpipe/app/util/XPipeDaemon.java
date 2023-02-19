package io.xpipe.app.util;

import io.xpipe.app.ext.DataSourceProvider;
import io.xpipe.app.ext.DataStoreProvider;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.core.charsetter.Charsetter;
import io.xpipe.core.source.DataSource;
import io.xpipe.core.source.DataSourceType;
import io.xpipe.core.store.DataStore;
import javafx.beans.property.Property;
import javafx.beans.value.ObservableValue;
import javafx.scene.image.Image;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.Predicate;

public interface XPipeDaemon {

    static XPipeDaemon getInstance() {
        return ServiceLoader.load(XPipeDaemon.class).findFirst().orElseThrow();
    }

    static Optional<XPipeDaemon> getInstanceIfPresent() {
        return ServiceLoader.load(XPipeDaemon.class).findFirst();
    }

    void withResource(String module, String file, Charsetter.FailableConsumer<Path, IOException> con);

    List<DataStore> getNamedStores();

    String getVersion();

    Image image(String file);

    String svgImage(String file);

    <T extends Comp<?> & Validatable> T streamStoreChooser(
            Property<DataStore> storeProperty,
            Property<DataSourceProvider<?>> provider,
            boolean showAnonymous,
            boolean showSaved);

    <T extends Comp<?> & Validatable> T namedStoreChooser(
            ObservableValue<Predicate<DataStore>> filter,
            Property<? extends DataStore> selected,
            DataStoreProvider.DataCategory category);

    <T extends Comp<?> & Validatable> T namedSourceChooser(
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
