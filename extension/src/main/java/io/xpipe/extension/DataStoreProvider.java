package io.xpipe.extension;

import io.xpipe.core.dialog.Dialog;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.store.MachineFileStore;
import io.xpipe.core.store.ShellStore;
import io.xpipe.core.store.StreamDataStore;
import javafx.beans.property.Property;

import java.net.URI;
import java.util.List;

public interface DataStoreProvider {

    enum Category {
        STREAM,
        MACHINE,
        DATABASE;
    }

    default Category getCategory() {
        var c = getStoreClasses().get(0);
        if (StreamDataStore.class.isAssignableFrom(c)) {
            return Category.STREAM;
        }

        if (MachineFileStore.class.isAssignableFrom(c) || ShellStore.class.isAssignableFrom(c)) {
            return Category.MACHINE;
        }

        throw new ExtensionException("Provider " + getId() + " has no set category");
    }

    default GuiDialog guiDialog(Property<DataStore> store) {
        throw new ExtensionException("Gui Dialog is not implemented by provider " + getId());
    }

    default void init() throws Exception {
    }

    default boolean isHidden() {
        return false;
    }

    default String i18n(String key) {
        return I18n.get(getId() + "." + key);
    }

    default String i18nKey(String key) {
        return getId() + "." + key;
    }

    default String getDisplayName() {
        return i18n("displayName");
    }

    default String getDisplayDescription() {
        return i18n("displayDescription");
    }

    default String getModuleName() {
        var n = getClass().getPackageName();
        var i = n.lastIndexOf('.');
        return i != -1 ? n.substring(i + 1) : n;
    }

    default String getDisplayIconFileName() {
        return getModuleName() + ":" + getId() + "_icon.png";
    }

    default Dialog dialogForString(String s) {
        return null;
    }

    default Dialog dialogForURI(URI uri) {
        return null;
    }

    default Dialog defaultDialog() {
        throw new ExtensionException("CLI Dialog not implemented by provider");
    }

    default String display(DataStore store) {
        return store.toDisplay();
    }

    List<String> getPossibleNames();

    default String getId() {
        return getPossibleNames().get(0);
    }

    List<Class<?>> getStoreClasses();
}
