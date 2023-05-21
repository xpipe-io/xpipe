package io.xpipe.app.browser.action;

import io.xpipe.app.browser.BrowserEntry;

import java.util.List;

public class BrowserActionFormatter {

    public static String filesArgument(List<BrowserEntry> entries) {
        return entries.size() == 1 ? entries.get(0).getOptionallyQuotedFileName() : "(" + entries.size() + ")";
    }

    public static String centerEllipsis(String input, int length) {
        if (input == null) {
            return "";
        }

        if (input.length() <= length) {
            return input;
        }

        var half = (length / 2) - 5;
        return input.substring(0, half) + " ... " + input.substring(input.length() - half);
    }
}
