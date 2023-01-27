package io.xpipe.ext.pdx.parser;

public class ParseException extends Exception {

    public ParseException(String message) {
        super(message);
    }

    public ParseException(Throwable t) {
        super("Parser failed because: " + t.getMessage(), t);
    }

    public ParseException(String message, Throwable cause) {
        super(message, cause);
    }

    private static int getUsedOffset(int offset, byte[] data) {
        boolean isEndOfLine = data.length - 2 > offset && data[offset + 1] == '\n';
        if (isEndOfLine) {
            return offset + 2;
        } else {
            return offset;
        }
    }

    private static int getLineNumber(int offset, byte[] data) {
        offset = getUsedOffset(offset, data);

        int line = 1;
        for (int i = 0; i < offset; i++) {
            if (data[i] == '\n') {
                line++;
            }
        }
        return line;
    }

    private static int getDataStart(int offset, byte[] data) {
        offset = getUsedOffset(offset, data);

        int i;
        boolean skippedLine = false;
        for (i = offset; i >= Math.max(offset - 100, 0); i--) {
            if (data[i] == '\n') {
                if (!skippedLine) {
                    skippedLine = true;
                } else {
                    return i + 1;
                }
            }
        }
        return i + 1;
    }

    private static int getDataEnd(int offset, byte[] data) {
        offset = getUsedOffset(offset, data);

        int i;
        boolean skippedLine = false;
        for (i = offset; i < Math.min(offset + 100, data.length); i++) {
            if (data[i] == '\n') {
                if (!skippedLine) {
                    skippedLine = true;
                } else {
                    return i - 1;
                }
            }
        }
        return i - 1;
    }

    public static ParseException createFromOffset(String fileName, String s, int offset, byte[] data) {
        // Clamp range
        offset = Math.max(0, Math.min(offset, data.length - 1));

        var start = getDataStart(offset, data);
        var end = getDataEnd(offset, data);
        var length = Math.max(end - start + 1, 0);
        var snippet = new String(data, start, length);
        var msg = "Parser failed for " + fileName + " at line " + getLineNumber(offset, data) + " / offset " + offset
                + ": " + s + "\n\n" + snippet;
        return new ParseException(msg);
    }

    public static ParseException createFromLiteralIndex(String fileName, String s, int lIndex, NodeContext ctx) {
        var offset = ctx.getLiteralsBegin()[Math.max(0, Math.min(ctx.getLiteralsCount() - 1, lIndex))];
        return createFromOffset(fileName, s, offset, ctx.getData());
    }
}
