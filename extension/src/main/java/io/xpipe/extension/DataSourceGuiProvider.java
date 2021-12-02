package io.xpipe.extension;

import io.xpipe.core.source.DataSource;
import io.xpipe.core.store.DataStore;
import javafx.beans.property.Property;
import javafx.scene.image.Image;
import javafx.scene.layout.Region;

import java.nio.file.Path;
import java.util.Map;
import java.util.function.Supplier;

public interface DataSourceGuiProvider {

    default boolean isHidden() {
        return false;
    }

    boolean supportsFile(Path file);

    Region createConfigOptions(DataStore input, Property<? extends DataSource<?>> source);

    DataSource<?> createDefaultDataSource(DataStore input);

    String getDisplayName();

    Image getImage();

    Supplier<String> getFileName();

    Map<Supplier<String>, String> getFileExtensions();

    String getDataSourceDescription(DataSource<?> source);

    Class<? extends DataSource<?>> getType();
}
