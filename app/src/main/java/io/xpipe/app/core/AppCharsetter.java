package io.xpipe.app.core;

import io.xpipe.core.charsetter.Charsetter;
import io.xpipe.core.charsetter.StreamCharset;
import org.apache.commons.io.ByteOrderMark;
import org.apache.commons.io.input.BOMInputStream;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

public class AppCharsetter extends Charsetter {

    private static final int MAX_BYTES = 8192;

    public static void init() {
        Charsetter.INSTANCE = new AppCharsetter();
    }

    public Result read(FailableSupplier<InputStream, Exception> in, FailableConsumer<InputStreamReader, Exception> con) throws Exception {
        checkInit();

        try (var is = in.get(); var bin = new BOMInputStream(is)) {
            ByteOrderMark bom = bin.getBOM();
            String charsetName = bom == null ? null : bom.getCharsetName();
            var charset = charsetName != null ? StreamCharset.get(Charset.forName(charsetName), bom.getCharsetName() != null) : null;

            bin.mark(MAX_BYTES);
            var bytes = bin.readNBytes(MAX_BYTES);
            bin.reset();
            if (charset == null) {
                charset = StreamCharset.get(inferCharset(bytes), false);
            }
            var nl = inferNewLine(bytes);

            if (con != null) {
                con.accept(new InputStreamReader(bin, charset.getCharset()));
            }
            return new Result(charset, nl);
        }
    }
}
