package io.xpipe.ext.proc.util;

import io.xpipe.extension.event.TrackEvent;
import lombok.Value;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

public class ShellReader {

    public static String readLine(InputStream inputStream, Charset charset) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        while (true) {
            int c = inputStream.read();

            if (c == -1 && byteArrayOutputStream.size() == 0) {
                return null;
            }

            if (c != '\n' && c != -1) {
                byteArrayOutputStream.write(c);
            } else {
                break;
            }
        }

        var string = byteArrayOutputStream.toString(charset);
        if (string.endsWith("\r")) {
            string = string.substring(0, string.length() - 1);
        }

        TrackEvent.withTrace("proc", "Read: " + string).handle();
        return string;
    }

    public static String readAllUntilTimeout(Charset charset, InputStream is, int timeout) throws IOException {
        var b = new byte[1024];

        int bufferOffset = 0;
        long maxTimeMillis = System.currentTimeMillis() + timeout;
        while (System.currentTimeMillis() < maxTimeMillis && bufferOffset < b.length) {
            int readLength = Math.min(is.available(), b.length - bufferOffset);
            // can alternatively use bufferedReader, guarded by isReady():
            int readResult = is.read(b, bufferOffset, readLength);
            if (readResult == -1) break;
            bufferOffset += readResult;
        }
        var string = new String(b, 0, bufferOffset, charset);
        TrackEvent.withTrace("proc", "Read: " + string).handle();
        return string;
    }

    public static String readErrorUntilOccurrenceOrTimeout(
            InputStream inputStream, Charset charset, String s, int timeout)
            throws IOException {
        StringBuilder text = new StringBuilder();
        while (true) {
            var newLine = readLineUntilTimeout(charset, inputStream, timeout);
            if (newLine.isEmpty()) {
                return text.toString();
            }

            if (newLine.getContent().equals(s)) {
                return null;
            }

            text.append(newLine.getContent()).append("\n");
        }
    }

    public static String readUntilOccurrence(
            InputStream inputStream, Charset charset, String s, String ignore, boolean allowedPreliminaryExit)
            throws IOException {
        StringBuilder text = new StringBuilder();
        while (true) {
            var newLine = readLine(inputStream, charset);

            if (newLine == null) {
                if (allowedPreliminaryExit) {
                    break;
                }

                throw new IOException("Input was closed before end was read");
            }

            var shouldIgnoreLine = ignore != null && newLine.contains(ignore);
            if (shouldIgnoreLine) {
                continue;
            }

            if (newLine.contains(s)) {
                break;
            }

            text.append(newLine).append("\n");
        }
        return text.toString();
    }

    public static ReadResult readLineUntilTimeout(Charset charset, InputStream is, int timeout) throws IOException {
        var b = new byte[1024];
        var hasSeenNewLine = false;

        int bufferOffset = 0;
        long maxTimeMillis = System.currentTimeMillis() + timeout;
        while (System.currentTimeMillis() < maxTimeMillis && bufferOffset < b.length) {
            int readLength = java.lang.Math.min(is.available(), b.length - bufferOffset);
            // can alternatively use bufferedReader, guarded by isReady():
            int readResult = is.read(b, bufferOffset, readLength > 0 ? 1 : 0);
            if (readResult == -1) break;
            if (readResult > 0 && b[bufferOffset] == '\n') {
                hasSeenNewLine = true;
                break;
            }
            bufferOffset += readResult;
        }

        var string = new String(b, 0, bufferOffset, charset);
        if (string.endsWith("\r")) {
            string = string.substring(0, string.length() - 1);
        }

        TrackEvent.withTrace("proc", "Read: " + string + (!hasSeenNewLine ? " (no LF)" : ""))
                .handle();
        return new ReadResult(string, hasSeenNewLine);
    }

    @Value
    public static class ReadResult {
        String content;
        boolean hasSeenNewLine;

        public boolean isEmpty() {
            return content.isEmpty() && !hasSeenNewLine;
        }
    }
}
