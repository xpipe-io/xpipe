package io.xpipe.ext.pdx.parser;

import java.util.regex.Pattern;

public class StringValues {

    private static final byte DOUBLE_QUOTE_CHAR = 34;

    private static final Pattern UNESCAPE_PATTERN = Pattern.compile("\\\\([\"\\\\])");

    public static String unescapeScalarValue(NodeContext context, int index) {
        var b = context.getLiteralsBegin()[index];
        var l = context.getLiteralsLength()[index];
        var s = new String(context.getData(), b, l, context.getCharset());
        if (l < 2) {
            return s;
        }

        boolean quoted = context.getData()[b] == DOUBLE_QUOTE_CHAR && context.getData()[b + l - 1] == DOUBLE_QUOTE_CHAR;
        if (!quoted) {
            return s;
        }

        var matcher = UNESCAPE_PATTERN.matcher(s);
        matcher.region(1, s.length() - 1);
        return matcher.replaceAll(r -> "$1");
    }

    public static String escapeStringContent(String val) {
        return val.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
