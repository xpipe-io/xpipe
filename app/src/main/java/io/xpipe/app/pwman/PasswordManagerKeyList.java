package io.xpipe.app.pwman;

import io.xpipe.app.prefs.AppPrefs;

import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableBooleanValue;

import java.util.List;

public class PasswordManagerKeyList {

    public static ObservableBooleanValue isSupported() {
        return Bindings.createBooleanBinding(
                () -> {
                    var pwman = AppPrefs.get().passwordManager().getValue();
                    return pwman != null && pwman.supportsList();
                },
                AppPrefs.get().passwordManager());
    }

    private static Class<?> cachedPasswordManagerClass;
    private static List<PasswordManager.ListEntry> cached;

    public static synchronized List<PasswordManager.ListEntry> queryList(boolean refresh) {
        if (!isSupported().get()) {
            return List.of();
        }

        var pwman = AppPrefs.get().passwordManager().getValue();

        if (cached != null && !cached.isEmpty() && !refresh && pwman.getClass() == cachedPasswordManagerClass) {
            return cached;
        }

        var l = pwman.listKeys();
        cached = l;
        cachedPasswordManagerClass = pwman.getClass();
        return l;
    }
}
