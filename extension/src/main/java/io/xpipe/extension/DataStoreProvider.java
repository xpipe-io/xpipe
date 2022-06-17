package io.xpipe.extension;

import io.xpipe.core.dialog.Dialog;
import io.xpipe.core.store.DataStore;

import java.net.URL;
import java.util.List;

public interface DataStoreProvider {

    default void init() throws Exception {
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

    default String getDisplayIconFileName() {
        return getId() + ":icon.png";
    }

    default Dialog dialogForURL(URL url) {
        return null;
    }

    default Dialog dialogForString(String s) {
        return null;
    }

    Dialog defaultDialog();

    default String display(DataStore store) {
        return store.toDisplay();
    }

    List<String> getPossibleNames();

    default String getId() {
        return getPossibleNames().get(0);
    }
}
