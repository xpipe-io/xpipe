package io.xpipe.extension;

import io.xpipe.core.charsetter.NewLine;
import io.xpipe.core.dialog.Dialog;
import io.xpipe.core.dialog.QueryConverter;
import io.xpipe.core.source.DataSource;
import io.xpipe.core.source.DataSourceType;
import io.xpipe.core.store.DataStore;
import javafx.beans.property.Property;
import javafx.scene.layout.Region;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

public interface DataSourceProvider<T extends DataSource<?>> {

    static enum Category {
        FILE,
        DATABASE;
    }

    default void validate() throws Exception {
        getGeneralType();
        getSourceClass();
    }

    default Category getGeneralType() {
        if (getFileProvider() != null) {
            return Category.FILE;
        }

        if (getDatabaseProvider() != null) {
            return Category.DATABASE;
        }

        throw new ExtensionException("Provider has no general type");
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

    default Region createConfigGui(Property<T> source, Property<T> appliedSource) {
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

    public static Dialog charset(Charset c, boolean all)  {
        return Dialog.query("charset", false, false, c != null &&!all, c, QueryConverter.CHARSET);
    }

    public static Dialog newLine(NewLine l, boolean all)  {
        return Dialog.query("newline", false, false, l != null &&!all, l, NEW_LINE_CONVERTER);
    }

    static <T> Dialog query(String desc, T value, boolean required, QueryConverter<T> c, boolean all)  {
        return Dialog.query(desc, false, required, value != null && !all, value, c);
    }

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

    Dialog configDialog(T source, boolean all);

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

    default String getId() {
        return getPossibleNames().get(0);
    }

    default String getModuleName() {
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

    Class<T> getSourceClass();


    List<String> getPossibleNames();
}
