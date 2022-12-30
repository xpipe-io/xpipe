package io.xpipe.extension;

import io.xpipe.core.dialog.Dialog;
import io.xpipe.core.source.DataSource;
import io.xpipe.core.source.DataSourceType;
import io.xpipe.core.store.DataStore;
import javafx.beans.property.Property;
import javafx.scene.layout.Region;

import java.util.List;
import java.util.Map;

public interface DataSourceProvider<T extends DataSource<?>> {

    default void validate() throws Exception {
        getCategory();
        getSourceClass();
    }

    default Category getCategory() {
        if (getFileProvider() != null) {
            return Category.STREAM;
        }

        throw new ExtensionException("Provider has no set general type");
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
        return I18n.get(i18nKey(key));
    }

    default String i18nKey(String key) {
        return getId() + "." + key;
    }

    default Region configGui(Property<T> source, boolean preferQuiet) throws Exception {
        return null;
    }

    default String getDisplayName() {
        return i18n("displayName");
    }

    default String getDisplayDescription() {
        return i18n("displayDescription");
    }

    default String getModuleName() {
        var n = getClass().getModule().getName();
        var i = n.lastIndexOf('.');
        return i != -1 ? n.substring(i + 1) : n;
    }

    default String queryInformationString(DataStore store, int length) throws Exception {
        return getDisplayName();
    }

    default String getDisplayIconFileName() {
        return getModuleName() + ":" + getId() + "_icon.png";
    }

    Dialog configDialog(T source, boolean all);

    default boolean shouldShow(DataSourceType type) {
        return type == null || type == getPrimaryType();
    }

    DataSourceType getPrimaryType();

    /**
     * Checks whether this provider prefers a certain kind of store.
     * This is important for the correct autodetection of a store.
     */
    boolean prefersStore(DataStore store, DataSourceType type);

    /**
     * Checks whether this provider supports the store in principle.
     * This method should not perform any further checks,
     * just check whether it may be possible that the store is supported.
     * <p>
     * This method will be called for validation purposes.
     */
    boolean couldSupportStore(DataStore store);

    /**
     * Performs a deep inspection to check whether this provider supports a given store.
     * <p>
     * This functionality will be used in case no preferred provider has been found.
     */
    default boolean supportsStore(DataStore store) {
        return false;
    }

    default FileProvider getFileProvider() {
        return null;
    }

    default String getId() {
        return getPossibleNames().get(0);
    }

    /**
     * Attempt to create a useful data source descriptor from a data store.
     * The result does not need to be always right, it should only reflect the best effort.
     */
    T createDefaultSource(DataStore input) throws Exception;

    Class<T> getSourceClass();

    List<String> getPossibleNames();

    static enum Category {
        STREAM,
        DATABASE;
    }

    interface FileProvider {

        String getFileName();

        Map<String, List<String>> getFileExtensions();
    }
}
