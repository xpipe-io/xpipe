package io.xpipe.charsetter;

import lombok.Value;
import org.apache.commons.io.ByteOrderMark;
import org.apache.commons.io.input.BOMInputStream;
import org.apache.commons.lang3.function.FailableConsumer;
import org.apache.commons.lang3.function.FailableSupplier;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.*;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class Charsetter {

    private static CharsetterUniverse universe;
    private static final int MAX_BYTES = 8192;

    public static void init(CharsetterContext ctx) {
        universe = CharsetterUniverse.create(ctx);
    }

    private static void checkInit() {
        if (universe == null) {
            throw new IllegalStateException("Charsetter not initialized");
        }
    }

    @Value
    public static class Result {
        Charset charset;
        NewLine newLine;
    }

    public static Result read(FailableSupplier<InputStream, Exception> in, FailableConsumer<InputStreamReader, Exception> con) throws Exception {
        checkInit();

        try (var is = in.get();
             var bin = new BOMInputStream(is)) {
            ByteOrderMark bom = bin.getBOM();
            String charsetName = bom == null ? null : bom.getCharsetName();
            var charset = charsetName != null ? Charset.forName(charsetName) : null;

            bin.mark(MAX_BYTES);
            var bytes = bin.readNBytes(MAX_BYTES);
            bin.reset();
            if (charset == null) {
                charset = inferCharset(bytes);
            }
            var nl = inferNewLine(bytes);

            if (con != null) {
                con.accept(new InputStreamReader(bin, charset));
            }
            return new Result(charset, nl);
        }
    }

    public static NewLine inferNewLine(byte[] content) {
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

    public static int count(byte[] outerArray, byte[] smallerArray) {
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

    public static Charset inferCharset(byte[] content) {
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
