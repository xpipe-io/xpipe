package io.xpipe.app.pwman;

import io.xpipe.app.prefs.AppPrefs;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableBooleanValue;

import java.util.ArrayList;
import java.util.List;

public class PasswordManagerKeyList {

    public static ObservableBooleanValue isSupported() {
        return Bindings.createBooleanBinding(() -> {
            var pwman = AppPrefs.get().passwordManager().getValue();
            return pwman != null && pwman.supportsList();
        }, AppPrefs.get().passwordManager());
    }

    private static List<PasswordManager.ListEntry> cached;

    public static synchronized List<PasswordManager.ListEntry> queryList(boolean refresh) {
        if (!isSupported().get()) {
            return List.of();
        }

        if (cached != null && !refresh) {
            return cached;
        }

        var pwman = AppPrefs.get().passwordManager().getValue();
        var l = pwman.listKeys();
        cached = l;
        return l;
    }
}
