package io.xpipe.charsetter;

import org.apache.commons.io.ByteOrderMark;
import org.apache.commons.io.input.BOMInputStream;
import org.apache.commons.lang3.function.FailableBiConsumer;
import org.apache.commons.lang3.function.FailableSupplier;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.*;

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

    public static Charset read(FailableSupplier<InputStream, Exception> in, FailableBiConsumer<InputStream, Charset, Exception> con) throws Exception {
        checkInit();

        try (var is = in.get();
             var bin = new BOMInputStream(is)) {
            ByteOrderMark bom = bin.getBOM();
            String charsetName = bom == null ? null : bom.getCharsetName();
            var charset = charsetName != null ? Charset.forName(charsetName) : null;

            if (charset == null) {
                bin.mark(MAX_BYTES);
                var bytes = bin.readNBytes(MAX_BYTES);
                bin.reset();
                charset = inferCharset(bytes);
            }

            if (con != null) {
                con.accept(bin, charset);
            }
            return charset;
        }
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
