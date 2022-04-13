package io.xpipe.extension;

import io.xpipe.core.config.ConfigOption;
import io.xpipe.core.config.ConfigOptionSet;
import io.xpipe.core.source.DataSourceDescriptor;
import io.xpipe.core.source.DataSourceInfo;
import io.xpipe.core.source.DataSourceType;
import io.xpipe.core.store.DataStore;
import javafx.beans.property.Property;
import javafx.scene.layout.Region;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

public interface DataSourceProvider {

    interface FileProvider {

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

        ConfigOption
                CHARSET_OPTION = new ConfigOption("Charset", "charset");
        Function<String, Charset>
                CHARSET_CONVERTER = ConfigProvider.charsetConverter();
        Function<Charset, String>
                CHARSET_STRING = Charset::name;

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

        static Function<String, Charset> charsetConverter() {
            return Charset::forName;
        }

        ConfigOptionSet getConfig();

        DataSourceDescriptor<?> toDescriptor(Map<String, String> values);

        Map<String, String> toConfigOptions(DataSourceDescriptor<?> desc);

        Map<ConfigOption, Function<String, ?>> getConverters();

        List<String> getPossibleNames();
    }

    DataSourceType getType();

    boolean prefersStore(DataStore store);

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
