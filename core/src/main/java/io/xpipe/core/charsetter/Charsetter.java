package io.xpipe.core.charsetter;

import io.xpipe.core.store.StreamDataStore;
import io.xpipe.core.util.FailableConsumer;
import io.xpipe.core.util.FailableSupplier;
import lombok.Value;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public abstract class Charsetter {

    private static final int MAX_BYTES = 8192;
    public static Charsetter INSTANCE;
    private static CharsetterUniverse universe;

    protected Charsetter() {}

    protected static void checkInit() {
        if (universe == null) {
            throw new IllegalStateException("Charsetter not initialized");
        }
    }

    public static void init(CharsetterContext ctx) {
        universe = CharsetterUniverse.create(ctx);
    }

    public static Charsetter get() {
        return INSTANCE;
    }

    private static int count(byte[] outerArray, byte[] smallerArray) {
        int count = 0;
        for (int i = 0; i < outerArray.length - smallerArray.length + 1; ++i) {
            boolean found = true;
            for (int j = 0; j < smallerArray.length; ++j) {
                if (outerArray[i + j] != smallerArray[j]) {
                    found = false;
                    break;
                }
            }
            if (found) {
                count++;
            }
        }
        return count;
    }

    public BufferedReader reader(StreamDataStore store, StreamCharset charset) throws Exception {
        return reader(store.openBufferedInput(), charset);
    }

    public OutputStreamWriter writer(StreamDataStore store, StreamCharset charset) throws Exception {
        return new OutputStreamWriter(store.openOutput(), charset.getCharset());
    }

    public BufferedReader reader(InputStream stream, StreamCharset charset) throws Exception {
        if (charset.hasByteOrderMark()) {
            var bom = stream.readNBytes(charset.getByteOrderMark().length);
            if (bom.length != 0 && !Arrays.equals(bom, charset.getByteOrderMark())) {
                throw new IllegalStateException("Charset does not match: " + charset);
            }
        }

        return new BufferedReader(new InputStreamReader(stream, charset.getCharset()));
    }

    public abstract Result read(
            FailableSupplier<InputStream> in, FailableConsumer<InputStreamReader, Exception> con);

    public Result detect(StreamDataStore store) throws Exception {
        Result result = new Result(null, null);

        if (store.canOpen()) {

            try (InputStream inputStream = store.openBufferedInput()) {
                StreamCharset detected = null;
                for (var charset : StreamCharset.COMMON) {
                    if (charset.hasByteOrderMark()) {
                        inputStream.mark(charset.getByteOrderMark().length);
                        var bom = inputStream.readNBytes(charset.getByteOrderMark().length);
                        inputStream.reset();
                        if (Arrays.equals(bom, charset.getByteOrderMark())) {
                            detected = charset;
                            break;
                        }
                    }
                }

                var bytes = inputStream.readNBytes(MAX_BYTES);
                if (detected == null) {
                    detected = StreamCharset.get(inferCharset(bytes), false);
                }
                var nl = inferNewLine(bytes);
                result = new Result(detected, nl);
            }
        }

//        if (store instanceof FileStore fileStore && fileStore.getFileSystem() instanceof ShellStore m) {
//            if (result.getNewLine() == null) {
//                result = new Result(
//                        result.getCharset(),
//                        m.getShellType() != null ? m.getShellType().getNewLine() : null);
//            }
//        }

        if (result.getCharset() == null) {
            result = new Result(StreamCharset.UTF8, result.getNewLine());
        }

        if (result.getNewLine() == null) {
            result = new Result(result.getCharset(), NewLine.platform());
        }

        return result;
    }

    public NewLine inferNewLine(byte[] content) {
        Map<NewLine, Integer> count = new HashMap<>();
        for (var nl : NewLine.values()) {
            var nlBytes = nl.getNewLineString().getBytes(StandardCharsets.UTF_8);
            count.put(nl, count(content, nlBytes));
        }

        if (count.values().stream().allMatch(v -> v == 0)) {
            return null;
        }

        return count.entrySet().stream()
                .min(Comparator.comparingInt(Map.Entry::getValue))
                .orElseThrow()
                .getKey();
    }

    public Charset inferCharset(byte[] content) {
        checkInit();

        for (Charset c : universe.getCharsets()) {
            CharsetDecoder decoder = c.newDecoder();
            decoder.onMalformedInput(CodingErrorAction.REPORT);
            decoder.onUnmappableCharacter(CodingErrorAction.REPORT);

            ByteBuffer byteBuf = ByteBuffer.wrap(content);
            CharBuffer charBuf = CharBuffer.allocate(byteBuf.capacity() * 2);

            CoderResult coderResult = decoder.decode(byteBuf, charBuf, false);
            if (coderResult != null) {
                if (coderResult.isError()) {
                    continue;
                }
            }

            return c;
        }

        return StandardCharsets.UTF_8;
    }

    @Value
    public static class Result {
        StreamCharset charset;
        NewLine newLine;
    }
}
