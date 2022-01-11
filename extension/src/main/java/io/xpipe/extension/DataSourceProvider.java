package io.xpipe.extension;

import io.xpipe.core.source.DataSourceDescriptor;
import io.xpipe.core.store.DataStore;
import javafx.beans.property.Property;
import javafx.scene.layout.Region;

import java.nio.file.Path;
import java.util.Map;
import java.util.function.Supplier;

public interface DataSourceProvider {

    interface FileProvider {

        boolean supportsFile(Path file);

        String getFileName();

        Map<Supplier<String>, String> getFileExtensions();
    }

    interface GuiProvider {

        Region createConfigOptions(DataStore input, Property<? extends DataSourceDescriptor<?>> source);

        String getDisplayName();

        String getDisplayImage();

        Supplier<String> getDescription(DataSourceDescriptor<?> source);
    }

    interface CliProvider {

    }

    FileProvider getFileProvider();

    GuiProvider getGuiProvider();

    CliProvider getCliProvider();

    String getId();

    /**
     * Attempt to create a useful data source descriptor from a data store.
     * The result does not need to be always right, it should only reflect the best effort.
     */
    DataSourceDescriptor<?> createDefaultDescriptor(DataStore input) throws Exception;

    Class<? extends DataSourceDescriptor<?>> getDescriptorClass();
}
