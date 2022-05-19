package io.xpipe.extension;

import io.xpipe.core.config.ConfigOption;
import io.xpipe.core.config.ConfigOptionSet;
import io.xpipe.core.source.DataSourceDescriptor;
import io.xpipe.core.source.DataSourceType;
import io.xpipe.core.store.DataStore;
import javafx.beans.property.Property;
import javafx.scene.layout.Region;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public interface DataSourceProvider<T extends DataSourceDescriptor<?>> {

    static enum GeneralType {
        FILE,
        DATABASE;
    }

    default GeneralType getGeneralType() {
        if (getFileProvider() != null) {
            return GeneralType.FILE;
        }

        throw new ExtensionException("Provider has no general type");
    }

    default boolean supportsConversion(T in, DataSourceType t) {
        return false;
    }

    default DataSourceDescriptor<?> convert(T in, DataSourceType t) throws Exception {
        throw new ExtensionException();
    }

    default void init() {
    }

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

    default String getDisplayDescription() {
        return i18n("displayDescription");
    }

    default String getDisplayIconFileName() {
        return getId() + ":icon.png";
    }

    default String getSourceDescription(T source) {
        return getDisplayName();
    }

    interface FileProvider {

        String getFileName();

        Map<String, List<String>> getFileExtensions();
    }

    interface ConfigProvider<T extends DataSourceDescriptor<?>> {

        static <T extends DataSourceDescriptor<?>> ConfigProvider<T> empty(List<String> names, Function<DataStore, T> func) {
            return new ConfigProvider<>() {
                @Override
                public ConfigOptionSet getConfig() {
                    return ConfigOptionSet.empty();
                }

                @Override
                public T toDescriptor(DataStore store, Map<String, String> values) {
                    return func.apply(store);
                }

                @Override
                public Map<String, String> toConfigOptions(T source) {
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

        T toDescriptor(DataStore store, Map<String, String> values);

        Map<String, String> toConfigOptions(T desc);

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

    default FileProvider getFileProvider() {
        return null;
    }

    default boolean hasDirectoryProvider() {
        return false;
    }

    ConfigProvider<T> getConfigProvider();

    default String getId() {
        var n = getClass().getPackageName();
        var i = n.lastIndexOf('.');
        return i != -1 ? n.substring(i + 1) : n;
    }

    /**
     * Attempt to create a useful data source descriptor from a data store.
     * The result does not need to be always right, it should only reflect the best effort.
     */
    T createDefaultDescriptor(DataStore input) throws Exception;

    default T createDefaultWriteDescriptor(DataStore input) throws Exception {
        return createDefaultDescriptor(input);
    }

    @SuppressWarnings("unchecked")
    default Class<T> getDescriptorClass() {
        return (Class<T>) Arrays.stream(getClass().getDeclaredClasses())
                .filter(c -> c.getName().endsWith("Descriptor")).findFirst()
                .orElseThrow(() -> new AssertionError("Descriptor class not found"));
    }
}
