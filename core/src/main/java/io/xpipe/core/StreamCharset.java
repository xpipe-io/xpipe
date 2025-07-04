package io.xpipe.core;

import lombok.Value;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Value
public class StreamCharset {

    public static final StreamCharset UTF8 = new StreamCharset(StandardCharsets.UTF_8, null);

    public static final StreamCharset UTF8_BOM =
            new StreamCharset(StandardCharsets.UTF_8, new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});

    // ======
    // UTF-16
    // ======

    public static final StreamCharset UTF16_BE = new StreamCharset(StandardCharsets.UTF_16BE, null);

    public static final StreamCharset UTF16_BE_BOM =
            new StreamCharset(StandardCharsets.UTF_16BE, new byte[] {(byte) 0xFE, (byte) 0xFF});

    public static final StreamCharset UTF16_LE = new StreamCharset(StandardCharsets.UTF_16LE, null);

    public static final StreamCharset UTF16_LE_BOM =
            new StreamCharset(StandardCharsets.UTF_16LE, new byte[] {(byte) 0xFF, (byte) 0xFE});

    public static final StreamCharset UTF16 = UTF16_LE;

    public static final StreamCharset UTF16_BOM = UTF16_LE_BOM;

    public static final List<StreamCharset> COMMON = List.of(
            UTF8,
            UTF8_BOM,
            UTF16,
            UTF16_BOM,
            new StreamCharset(StandardCharsets.US_ASCII, null),
            new StreamCharset(StandardCharsets.ISO_8859_1, null),
            new StreamCharset(Charset.forName("Windows-1251"), null),
            new StreamCharset(Charset.forName("Windows-1252"), null));

    // ======
    // UTF-32
    // ======
    public static final StreamCharset UTF32_LE = new StreamCharset(Charset.forName("utf-32le"), null);
    public static final StreamCharset UTF32_LE_BOM =
            new StreamCharset(Charset.forName("utf-32le"), new byte[] {0x00, 0x00, (byte) 0xFE, (byte) 0xFF});
    public static final StreamCharset UTF32_BE = new StreamCharset(Charset.forName("utf-32be"), null);
    public static final StreamCharset UTF32_BE_BOM = new StreamCharset(Charset.forName("utf-32be"), new byte[] {
        (byte) 0xFF, (byte) 0xFE, 0x00, 0x00,
    });
    private static final List<StreamCharset> RARE =
            List.of(UTF16_LE, UTF16_LE_BOM, UTF16_BE, UTF16_BE_BOM, UTF32_LE, UTF32_LE_BOM, UTF32_BE, UTF32_BE_BOM);

    public static final List<StreamCharset> ALL =
            Stream.concat(COMMON.stream(), RARE.stream()).toList();

    Charset charset;
    byte[] byteOrderMark;

    public static StreamCharset get(Charset charset, boolean byteOrderMark) {
        return ALL.stream()
                .filter(streamCharset ->
                        streamCharset.getCharset().equals(charset) && streamCharset.hasByteOrderMark() == byteOrderMark)
                .findFirst()
                .orElseThrow();
    }

    public static InputStreamReader detectedReader(InputStream inputStream) throws Exception {
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

        if (detected == null) {
            detected = StreamCharset.UTF8;
        }

        return detected.reader(inputStream);
    }

    public String read(byte[] b) throws Exception {
        return read(new ByteArrayInputStream(b));
    }

    public String read(InputStream inputStream) throws Exception {
        if (hasByteOrderMark()) {
            var bom = inputStream.readNBytes(getByteOrderMark().length);
            if (bom.length != 0 && !Arrays.equals(bom, getByteOrderMark())) {
                throw new IllegalStateException("Charset does not match: " + charset.toString());
            }
        }
        return new String(inputStream.readAllBytes(), charset);
    }

    public InputStreamReader reader(InputStream stream) throws Exception {
        if (hasByteOrderMark()) {
            var bom = stream.readNBytes(getByteOrderMark().length);
            if (bom.length != 0 && !Arrays.equals(bom, getByteOrderMark())) {
                throw new IllegalStateException("Charset does not match: " + charset.toString());
            }
        }

        return new InputStreamReader(stream, charset);
    }

    public byte[] toBytes(String s) {
        var raw = s.getBytes(charset);
        if (hasByteOrderMark()) {
            var bom = getByteOrderMark();
            var r = new byte[raw.length + bom.length];
            System.arraycopy(bom, 0, r, 0, bom.length);
            System.arraycopy(raw, 0, r, bom.length, raw.length);
            return r;
        } else {
            return raw;
        }
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(charset);
        result = 31 * result + Arrays.hashCode(byteOrderMark);
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof StreamCharset that)) {
            return false;
        }
        return charset.equals(that.charset) && Arrays.equals(byteOrderMark, that.byteOrderMark);
    }

    public boolean hasByteOrderMark() {
        return byteOrderMark != null;
    }
}
