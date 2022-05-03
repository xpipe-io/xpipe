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
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

public interface DataSourceProvider {

    default String i18n(String key) {
        return I18n.get(getId() + "." + key);
    }

    default String i18nKey(String key) {
        return getId() + "." + key;
    }

    default Region createConfigOptions(DataStore input, Property<? extends DataSourceDescriptor<?>> source) {
        return null;
    }

    default String getDisplayName() {
        return i18n("displayName");
    }

    default String getDisplayImageFile() {
        return "logo.png";
    }

    default String getDescription(DataSourceDescriptor<?> source) {
        return i18n("description");
    }

    interface FileProvider {

        String getFileName();

        Map<String, List<String>> getFileExtensions();
    }

    interface ConfigProvider {

        static ConfigProvider empty(List<String> names, Supplier<DataSourceDescriptor<?>> supplier) {
            return new ConfigProvider() {
                @Override
                public ConfigOptionSet getConfig() {
                    return ConfigOptionSet.empty();
                }

                @Override
                public DataSourceDescriptor<?> toDescriptor(Map<String, String> values) {
                    return supplier.get();
                }

                @Override
                public Map<String, String> toConfigOptions(DataSourceDescriptor<?> desc) {
                    return Map.of();
                }

                @Override
                public Map<ConfigOption, Function<String, ?>> getConverters() {
                    return Map.of();
                }

                @Override
                public List<String> getPossibleNames() {
                    return names;
                }
            };
        }

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

    default boolean isHidden() {
        return false;
    }

    DataSourceType getPrimaryType();

    /**
     * Checks whether this provider prefers a certain kind of store.
     * This is important for the correct autodetection of a store.
     */
    boolean prefersStore(DataStore store);

    /**
     * Checks whether this provider supports the store in principle.
     * This method should not perform any further checks,
     * just check whether it may be possible that the store is supported.
     *
     * This method will be called for validation purposes.
     */
    boolean couldSupportStore(DataStore store);

    /**
     * Performs a deep inspection to check whether this provider supports a given store.
     *
     * This functionality will be used in case no preferred provider has been found.
     */
    default boolean supportsStore(DataStore store) {
        return false;
    }

    FileProvider getFileProvider();

    ConfigProvider getConfigProvider();

    default String getId() {
        var n = getClass().getPackageName();
        var i = n.lastIndexOf('.');
        return i != -1 ? n.substring(i + 1) : n;
    }

    DataSourceDescriptor<?> createDefaultDescriptor();

    /**
     * Attempt to create a useful data source descriptor from a data store.
     * The result does not need to be always right, it should only reflect the best effort.
     */
    DataSourceDescriptor<?> createDefaultDescriptor(DataStore input) throws Exception;

    DataSourceDescriptor<?> createDefaultWriteDescriptor(DataStore input, DataSourceInfo info) throws Exception;

    Class<? extends DataSourceDescriptor<?>> getDescriptorClass();

    Optional<String> determineDefaultName(DataStore store);
}
