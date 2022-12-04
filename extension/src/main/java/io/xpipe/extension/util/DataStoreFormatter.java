package io.xpipe.extension.util;

import io.xpipe.core.store.DataStore;
import io.xpipe.core.store.ShellStore;
import io.xpipe.extension.DataStoreProviders;

import java.util.function.IntFunction;

public class DataStoreFormatter {

    public static String formatAtHost(IntFunction<String> func, DataStore at, int length) {
        var atString = at instanceof ShellStore shellStore && !ShellStore.isLocal(shellStore)
                ? DataStoreProviders.byStore(at).toSummaryString(at, length)
                : null;
        if (atString == null) {
            return func.apply(length);
        }

        var fileString = func.apply(length - atString.length() - 3);
        return String.format("%s @ %s", fileString, atString);
    }

    public static String format(DataStore input, int length) {
        var named = XPipeDaemon.getInstance().getStoreName(input);
        if (named.isPresent()) {
            return cut(named.get(), length);
        }

        return DataStoreProviders.byStore(input).toSummaryString(input, length);
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
                    "%s @ %s",
                    DataStoreFormatter.cut(name, lengthShare), DataStoreFormatter.cut(region, length - lengthShare));
        }

        return cut(input, length);
    }
}
