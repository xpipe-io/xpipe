package io.xpipe.extension;

import io.xpipe.core.source.*;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.store.StreamDataStore;
import javafx.beans.property.Property;
import javafx.scene.layout.Region;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

public interface DataSourceProvider {

    interface FileProvider {

        void write(StreamDataStore store, DataSourceDescriptor<StreamDataStore> desc, TableReadConnection con) throws Exception;

        String getFileName();

        Map<Supplier<String>, String> getFileExtensions();
    }

    interface GuiProvider {

        Region createConfigOptions(DataStore input, Property<? extends DataSourceDescriptor<?>> source);

        String getDisplayName();

        String getDisplayImage();

        Supplier<String> getDescription(DataSourceDescriptor<?> source);
    }

    interface ConfigProvider {

        static String booleanName(String name) {
            return name + " (y/n)";
        }

        static Function<String, Boolean> booleanConverter() {
            return s -> {
                if (s.equalsIgnoreCase("y") || s.equalsIgnoreCase("yes")) {
                    return true;
                }

                if (s.equalsIgnoreCase("n") || s.equalsIgnoreCase("no")) {
                    return false;
                }

              throw new IllegalArgumentException("Invalid boolean: " + s);
            };
        }

        static Function<String, Character> characterConverter() {
            return s -> {
                if (s.length() != 1) {
                    throw new IllegalArgumentException("Invalid character: " + s);
                }

                return s.toCharArray()[0];
            };
        }

        DataSourceConfigOptions getConfig();

        DataSourceDescriptor<?> toDescriptor(Map<String, String> values);

        Map<String, String> toConfigOptions(DataSourceDescriptor<?> desc);

        Map<DataSourceConfigOptions.Option, Function<String, ?>> getConverters();

        List<String> getPossibleNames();
    }

    DataSourceType getType();

    boolean supportsStore(DataStore store);

    FileProvider getFileProvider();

    GuiProvider getGuiProvider();

    ConfigProvider getConfigProvider();

    String getId();

    DataSourceDescriptor<?> createDefaultDescriptor();

    /**
     * Attempt to create a useful data source descriptor from a data store.
     * The result does not need to be always right, it should only reflect the best effort.
     */
    DataSourceDescriptor<?> createDefaultDescriptor(DataStore input) throws Exception;

    DataSourceDescriptor<?> createDefaultWriteDescriptor(DataStore input, DataSourceInfo info) throws Exception;

    Class<? extends DataSourceDescriptor<?>> getDescriptorClass();
}
