package io.xpipe.app.util;

import io.xpipe.app.comp.store.StoreEntryWrapper;
import io.xpipe.app.fxcomps.util.BindingsHelper;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.core.process.ShellStoreState;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.store.ShellStore;
import javafx.beans.value.ObservableValue;

import java.util.function.IntFunction;

public class DataStoreFormatter {

    public static ObservableValue<String> shellInformation(StoreEntryWrapper w) {
        return BindingsHelper.map(w.getPersistentState(), o -> {
            if (o instanceof ShellStoreState s) {
                if (!s.isInitialized()) {
                    return null;
                }

                if (s.getShellDialect() != null
                        && !s.getShellDialect().getDumbMode().supportsAnyPossibleInteraction()) {
                    return s.getOsName() != null
                            ? s.getOsName()
                            : s.getShellDialect().getDisplayName();
                }

                return s.isRunning() ? s.getOsName() : "Connection failed";
            }

            return "?";
        });
    }

    public static String capitalize(String name) {
        if (name == null) {
            return null;
        }

        return name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
    }

    public static String formatSubHost(IntFunction<String> func, DataStore at, int length) {
        var atString = at instanceof ShellStore shellStore && !ShellStore.isLocal(shellStore)
                ? DataStorage.get().getStoreDisplayName(at).orElse(null)
                : null;
        if (atString == null) {
            return func.apply(length);
        }

        var fileString = func.apply(length - atString.length() - 1);
        return String.format("%s/%s", atString, fileString);
    }

    public static String formatAtHost(IntFunction<String> func, DataStore at, int length) {
        var atString = at instanceof ShellStore shellStore && !ShellStore.isLocal(shellStore)
                ? DataStorage.get().getStoreDisplayName(at).orElse(null)
                : null;
        if (atString == null) {
            return func.apply(length);
        }

        var fileString = func.apply(length - atString.length() - 3);
        return String.format("%s @ %s", fileString, atString);
    }

    public static String formatViaProxy(IntFunction<String> func, DataStoreEntry at, int length) {
        var atString =
                at.getStore() instanceof ShellStore shellStore && !ShellStore.isLocal(shellStore) ? at.getName() : null;
        if (atString == null) {
            return func.apply(length);
        }

        var fileString = func.apply(length - atString.length() - 3);
        return String.format("%s > %s", atString, fileString);
    }

    public static String toApostropheName(DataStoreEntry input) {
        return toName(input, Integer.MAX_VALUE) + "'s";
    }

    public static String toName(DataStoreEntry input) {
        return toName(input, Integer.MAX_VALUE);
    }

    public static String toName(DataStoreEntry input, int length) {
        if (input == null) {
            return "?";
        }

        return cut(input.getName(), length);
    }

    public static String split(String left, String separator, String right, int length) {
        var half = (length / 2) - separator.length();
        return cut(left, half) + separator + cut(right, length - half);
    }

    public static String cut(String input, int length) {
        if (input == null) {
            return "";
        }

        var end = Math.min(input.length(), length);
        if (end < input.length()) {
            return input.substring(0, end) + "...";
        }
        return input;
    }

    public static String formatHostName(String input, int length) {
        // Remove port
        if (input.contains(":")) {
            input = input.split(":")[0];
        }

        // Check for amazon web services
        if (input.endsWith(".rds.amazonaws.com")) {
            var split = input.split("\\.");
            var name = split[0];
            var region = split[2];
            var lengthShare = (length - 3) / 2;
            return String.format(
                    "%s.%s",
                    DataStoreFormatter.cut(name, lengthShare), DataStoreFormatter.cut(region, length - lengthShare));
        }

        if (input.endsWith(".compute.amazonaws.com")) {
            var split = input.split("\\.");
            var name = split[0];
            var region = split[1];
            var lengthShare = (length - 3) / 2;
            return String.format(
                    "%s.%s",
                    DataStoreFormatter.cut(name, lengthShare), DataStoreFormatter.cut(region, length - lengthShare));
        }

        return cut(input, length);
    }
}
