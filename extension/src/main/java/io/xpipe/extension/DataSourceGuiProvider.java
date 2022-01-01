package io.xpipe.extension;

import io.xpipe.core.source.DataSourceDescriptor;
import io.xpipe.core.store.DataStore;
import javafx.beans.property.Property;
import javafx.scene.layout.Region;

import java.nio.file.Path;
import java.util.Map;
import java.util.function.Supplier;

public interface DataSourceGuiProvider {

    default boolean isHidden() {
        return false;
    }

    boolean supportsFile(Path file);

    Region createConfigOptions(DataStore input, Property<? extends DataSourceDescriptor<?>> source);

    DataSourceDescriptor<?> createDefaultDataSource(DataStore input) throws Exception;

    String getDisplayName();

    String getDisplayImage();

    String getFileName();

    Map<Supplier<String>, String> getFileExtensions();

    String getDataSourceShortDescription(DataSourceDescriptor<?> source);

    String getDataSourceLongDescription(DataSourceDescriptor<?> source);

    Class<? extends DataSourceDescriptor<?>> getType();
}
