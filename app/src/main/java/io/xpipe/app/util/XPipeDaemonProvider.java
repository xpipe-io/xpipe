package io.xpipe.app.util;

import io.xpipe.app.comp.source.DsProviderChoiceComp;
import io.xpipe.app.comp.source.NamedSourceChoiceComp;
import io.xpipe.app.comp.source.store.DsStreamStoreChoiceComp;
import io.xpipe.app.comp.source.store.NamedStoreChoiceComp;
import io.xpipe.app.core.AppImages;
import io.xpipe.app.core.AppProperties;
import io.xpipe.app.core.AppResources;
import io.xpipe.app.ext.DataSourceProvider;
import io.xpipe.app.ext.DataStoreProvider;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.update.AppDownloads;
import io.xpipe.core.charsetter.Charsetter;
import io.xpipe.core.source.DataSource;
import io.xpipe.core.source.DataSourceId;
import io.xpipe.core.source.DataSourceReference;
import io.xpipe.core.source.DataSourceType;
import io.xpipe.core.store.DataStore;
import javafx.beans.property.Property;
import javafx.beans.value.ObservableValue;
import javafx.scene.image.Image;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class XPipeDaemonProvider implements XPipeDaemon {

    @Override
    public void withResource(String module, String file, Charsetter.FailableConsumer<Path, IOException> con) {
        AppResources.with(module, file, con);
    }

    @Override
    public List<DataStore> getNamedStores() {
        return DataStorage.get().getStores().stream()
                .filter(entry -> !entry.isDisabled())
                .map(DataStoreEntry::getStore)
                .toList();
    }

    @Override
    public String getVersion() {
        var version = AppProperties.get().getVersion() != null
                ? AppProperties.get().getVersion()
                : AppDownloads.getLatestVersion();
        return version;
    }

    @Override
    public Image image(String file) {
        return AppImages.image(file);
    }

    @Override
    public String svgImage(String file) {
        return AppImages.svgImage(file);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Comp<?> & Validatable> T streamStoreChooser(
            Property<DataStore> storeProperty,
            Property<DataSourceProvider<?>> provider,
            boolean showAnonymous,
            boolean showSaved) {
        return (T) new DsStreamStoreChoiceComp(
                storeProperty, provider, showAnonymous, showSaved, DsStreamStoreChoiceComp.Mode.WRITE);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Comp<?> & Validatable> T namedStoreChooser(
            ObservableValue<Predicate<DataStore>> filter,
            Property<? extends DataStore> selected,
            DataStoreProvider.DataCategory category) {
        return (T) new NamedStoreChoiceComp(filter, selected, category);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Comp<?> & Validatable> T namedSourceChooser(
            ObservableValue<Predicate<DataSource<?>>> filter,
            Property<? extends DataSource<?>> selected,
            DataSourceProvider.Category category) {
        return (T) new NamedSourceChoiceComp(filter, selected, category);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Comp<?> & Validatable> T sourceProviderChooser(
            Property<DataSourceProvider<?>> provider, DataSourceProvider.Category category, DataSourceType filter) {
        return (T) new DsProviderChoiceComp(category, provider, filter);
    }

    @Override
    public Optional<DataStore> getNamedStore(String name) {
        if (name == null) {
            return Optional.empty();
        }
        return DataStorage.get().getStoreIfPresent(name).map(DataStoreEntry::getStore);
    }

    @Override
    public Optional<DataSource<?>> getSource(String id) {
        var sourceId = DataSourceId.fromString(id);
        return DataStorage.get()
                .getDataSource(DataSourceReference.id(sourceId))
                .map(dataSourceEntry -> dataSourceEntry.getSource());
    }

    @Override
    public Optional<String> getStoreName(DataStore store) {
        if (store == null) {
            return Optional.empty();
        }

        return DataStorage.get().getStores().stream()
                .filter(entry -> !entry.isDisabled() && entry.getStore().equals(store))
                .findFirst()
                .map(entry -> entry.getName());
    }

    @Override
    public Optional<String> getSourceId(DataSource<?> source) {
        var entry = DataStorage.get().getEntryBySource(source);
        return entry.map(
                dataSourceEntry -> DataStorage.get().getId(dataSourceEntry).toString());
    }
}
