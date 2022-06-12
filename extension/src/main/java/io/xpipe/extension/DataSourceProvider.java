package io.xpipe.extension;

import io.xpipe.charsetter.NewLine;
import io.xpipe.core.config.QueryConverter;
import io.xpipe.core.config.ConfigParameter;
import io.xpipe.core.source.DataSource;
import io.xpipe.core.source.DataSourceType;
import io.xpipe.core.store.DataStore;
import javafx.beans.property.Property;
import javafx.scene.layout.Region;
import lombok.SneakyThrows;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public interface DataSourceProvider<T extends DataSource<?>> {

    static enum GeneralType {
        FILE,
        DATABASE;
    }

    default GeneralType getGeneralType() {
        if (getFileProvider() != null) {
            return GeneralType.FILE;
        }

        if (getDatabaseProvider() != null) {
            return GeneralType.DATABASE;
        }

        throw new ExtensionException("Provider has no general type");
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    default T createDefault() {
        return (T) getSourceClass().getDeclaredConstructors()[0].newInstance();
    }

    default boolean supportsConversion(T in, DataSourceType t) {
        return false;
    }

    default DataSource<?> convert(T in, DataSourceType t) throws Exception {
        throw new ExtensionException();
    }

    default void init() throws Exception {
    }

    default String i18n(String key) {
        return I18n.get(getId() + "." + key);
    }

    default String i18nKey(String key) {
        return getId() + "." + key;
    }

    default Region createConfigGui(Property<T> source) {
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

    interface DatabaseProvider {

    }

    interface ConfigProvider<T extends DataSource<?>> {

        static <T extends DataSource<?>> ConfigProvider<T> empty(List<String> names, Function<DataStore, T> func) {
            return new ConfigProvider<>() {
                @Override
                public void applyConfig(T source, Map<ConfigParameter, Object> values) {
                }

                @Override
                public Map<ConfigParameter, Function<T, Object>> toCompleteConfig() {
                    return Map.of();
                }

                @Override
                public Map<ConfigParameter, Object> toRequiredReadConfig(T desc) {
                    return Map.of();
                }

                @Override
                public List<String> getPossibleNames() {
                    return names;
                }
            };
        }

        ConfigParameter CHARSET = new ConfigParameter(
                "charset", QueryConverter.CHARSET);

        public static final QueryConverter<NewLine> NEW_LINE_CONVERTER = new QueryConverter<NewLine>() {
            @Override
            protected NewLine fromString(String s) {
                return NewLine.id(s);
            }

            @Override
            protected String toString(NewLine value) {
                return value.getId();
            }
        };

        ConfigParameter NEWLINE = new ConfigParameter(
                "newline", NEW_LINE_CONVERTER);

        void applyConfig(T source, Map<ConfigParameter, Object> values);

        Map<ConfigParameter, Function<T, Object>> toCompleteConfig();

        Map<ConfigParameter, Object> toRequiredReadConfig(T desc);

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

    default DatabaseProvider getDatabaseProvider() {
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
    T createDefaultSource(DataStore input) throws Exception;

    default T createDefaultWriteSource(DataStore input) throws Exception {
        return createDefaultSource(input);
    }

    @SuppressWarnings("unchecked")
    default Class<T> getSourceClass() {
        return (Class<T>) Arrays.stream(getClass().getDeclaredClasses())
                .filter(c -> c.getName().endsWith("Source")).findFirst()
                .orElseThrow(() -> new ExtensionException("Descriptor class not found for " + getId()));
    }
}
