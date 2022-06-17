package io.xpipe.core.charsetter;

import lombok.Value;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.*;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public abstract class Charsetter {

    private static CharsetterUniverse universe;

    protected static void checkInit() {
        if (universe == null) {
            throw new IllegalStateException("Charsetter not initialized");
        }
    }

    public static void init(CharsetterContext ctx) {
        universe = CharsetterUniverse.create(ctx);
    }

    @Value
    public static class Result {
        Charset charset;
        NewLine newLine;
    }

    protected Charsetter() {}

    public static Charsetter INSTANCE;

    public static Charsetter get() {
        return INSTANCE;
    }

    @FunctionalInterface
    public interface FailableSupplier<R, E extends Throwable> {
        R get() throws E;
    }

    @FunctionalInterface
    public interface FailableConsumer<T, E extends Throwable> {

        void accept(T var1) throws E;
    }

    public abstract Result read(FailableSupplier<InputStream, Exception> in, FailableConsumer<InputStreamReader, Exception> con) throws Exception;

    public NewLine inferNewLine(byte[] content) {
        Map<NewLine, Integer> count = new HashMap<>();
        for (var nl : NewLine.values()) {
            var nlBytes = nl.getNewLine().getBytes(StandardCharsets.UTF_8);
            count.put(nl, count(content, nlBytes));
        }

        if (count.values().stream().allMatch(v -> v == 0)) {
            return null;
        }

        return count.entrySet().stream().min(Comparator.comparingInt(Map.Entry::getValue))
                .orElseThrow().getKey();
    }

    private static int count(byte[] outerArray, byte[] smallerArray) {
        int count = 0;
        for(int i = 0; i < outerArray.length - smallerArray.length+1; ++i) {
            boolean found = true;
            for(int j = 0; j < smallerArray.length; ++j) {
                if (outerArray[i+j] != smallerArray[j]) {
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
}
