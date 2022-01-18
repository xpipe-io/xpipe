package io.xpipe.extension;

import io.xpipe.core.source.DataSourceConfig;
import io.xpipe.core.source.DataSourceDescriptor;
import io.xpipe.core.source.DataSourceInfo;
import io.xpipe.core.source.TableReadConnection;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.store.StreamDataStore;
import javafx.beans.property.Property;
import javafx.scene.layout.Region;

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

    interface CliProvider {

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

        DataSourceConfig getConfig();

        DataSourceDescriptor<?> toDescriptor(Map<String, String> values);

        Map<String, String> toConfigOptions(DataSourceDescriptor<?> desc);

        Map<DataSourceConfig.Option, Function<String, ?>> getConverters();
    }

    boolean supportsStore(DataStore store);

    FileProvider getFileProvider();

    GuiProvider getGuiProvider();

    CliProvider getCliProvider();

    String getId();

    /**
     * Attempt to create a useful data source descriptor from a data store.
     * The result does not need to be always right, it should only reflect the best effort.
     */
    DataSourceDescriptor<?> createDefaultDescriptor(DataStore input) throws Exception;

    DataSourceDescriptor<?> createDefaultWriteDescriptor(DataStore input, DataSourceInfo info) throws Exception;

    Class<? extends DataSourceDescriptor<?>> getDescriptorClass();
}
