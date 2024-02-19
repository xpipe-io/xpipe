package io.xpipe.core.charsetter;

import io.xpipe.core.util.Identifiers;
import lombok.Value;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Stream;

@Value
public class StreamCharset {

    public static final StreamCharset UTF8 =
            new StreamCharset(StandardCharsets.UTF_8, null, Identifiers.get("utf", "8"));

    public static final StreamCharset UTF8_BOM = new StreamCharset(
            StandardCharsets.UTF_8,
            new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF},
            Identifiers.get("utf", "8", "bom"));

    // ======
    // UTF-16
    // ======

    public static final StreamCharset UTF16_BE =
            new StreamCharset(StandardCharsets.UTF_16BE, null, Identifiers.get("utf", "16", "be"));

    public static final StreamCharset UTF16_BE_BOM = new StreamCharset(
            StandardCharsets.UTF_16BE,
            new byte[] {(byte) 0xFE, (byte) 0xFF},
            Identifiers.get("utf", "16", "be", "bom"));

    public static final StreamCharset UTF16_LE =
            new StreamCharset(StandardCharsets.UTF_16LE, null, Identifiers.get("utf", "16", "le"));

    public static final StreamCharset UTF16_LE_BOM = new StreamCharset(
            StandardCharsets.UTF_16LE,
            new byte[] {(byte) 0xFF, (byte) 0xFE},
            Identifiers.get("utf", "16", "le", "bom"));

    public static final StreamCharset UTF16 = UTF16_LE;

    public static final StreamCharset UTF16_BOM = UTF16_LE_BOM;

    public static final List<StreamCharset> COMMON = List.of(
            UTF8,
            UTF8_BOM,
            UTF16,
            UTF16_BOM,
            new StreamCharset(
                    StandardCharsets.US_ASCII,
                    null,
                    Identifiers.join(Identifiers.get("ascii"), Identifiers.get("us", "ascii"))),
            new StreamCharset(
                    StandardCharsets.ISO_8859_1,
                    null,
                    Identifiers.join(
                            Identifiers.get("iso", "8859"),
                            Identifiers.get("iso", "8859", "1"),
                            Identifiers.get("8859"),
                            Identifiers.get("8859", "1"))),
            new StreamCharset(
                    Charset.forName("Windows-1251"),
                    null,
                    Identifiers.join(Identifiers.get("windows", "1251"), Identifiers.get("1251"))),
            new StreamCharset(
                    Charset.forName("Windows-1252"),
                    null,
                    Identifiers.join(Identifiers.get("windows", "1252"), Identifiers.get("1252"))));

    // ======
    // UTF-32
    // ======
    public static final StreamCharset UTF32_LE =
            new StreamCharset(Charset.forName("utf-32le"), null, Identifiers.get("utf", "32", "le"));
    public static final StreamCharset UTF32_LE_BOM = new StreamCharset(
            Charset.forName("utf-32le"),
            new byte[] {0x00, 0x00, (byte) 0xFE, (byte) 0xFF},
            Identifiers.get("utf", "32", "le", "bom"));
    public static final StreamCharset UTF32_BE =
            new StreamCharset(Charset.forName("utf-32be"), null, Identifiers.get("utf", "32", "be"));
    public static final StreamCharset UTF32_BE_BOM = new StreamCharset(
            Charset.forName("utf-32be"),
            new byte[] {
                (byte) 0xFF, (byte) 0xFE, 0x00, 0x00,
            },
            Identifiers.get("utf", "32", "be", "bom"));
    private static final List<StreamCharset> RARE_NAMED =
            List.of(UTF16_LE, UTF16_LE_BOM, UTF16_BE, UTF16_BE_BOM, UTF32_LE, UTF32_LE_BOM, UTF32_BE, UTF32_BE_BOM);

    public static final List<StreamCharset> RARE = Stream.concat(
                    RARE_NAMED.stream(),
                    Charset.availableCharsets().values().stream()
                            .filter(charset -> !charset.equals(StandardCharsets.UTF_16)
                                    && !charset.equals(Charset.forName("utf-32"))
                                    && !charset.displayName().startsWith("x-")
                                    && !charset.displayName().startsWith("X-")
                                    && !charset.displayName().endsWith("-BOM")
                                    && COMMON.stream()
                                            .noneMatch(c -> c.getCharset().equals(charset))
                                    && RARE_NAMED.stream()
                                            .noneMatch(c -> c.getCharset().equals(charset)))
                            .map(charset -> new StreamCharset(
                                    charset,
                                    null,
                                    Identifiers.get(charset.name().split("-")))))
            .toList();

    public static final List<StreamCharset> ALL =
            Stream.concat(COMMON.stream(), RARE.stream()).toList();

    Charset charset;
    byte[] byteOrderMark;
    List<String> names;

    public Reader reader(InputStream stream) throws Exception {
        if (hasByteOrderMark()) {
            var bom = stream.readNBytes(getByteOrderMark().length);
            if (bom.length != 0 && !Arrays.equals(bom, getByteOrderMark())) {
                throw new IllegalStateException("Charset does not match: " + charset.toString());
            }
        }

        return new InputStreamReader(stream, charset);
    }

    public static StreamCharset get(Charset charset, boolean byteOrderMark) {
        return ALL.stream()
                .filter(streamCharset ->
                        streamCharset.getCharset().equals(charset) && streamCharset.hasByteOrderMark() == byteOrderMark)
                .findFirst()
                .orElseThrow();
    }

    public static StreamCharset get(String s) {
        var found = ALL.stream()
                .filter(streamCharset -> streamCharset.getNames().contains(s.toLowerCase(Locale.ROOT)))
                .findFirst();
        if (found.isEmpty()) {
            throw new IllegalArgumentException("Unknown charset name: " + s);
        }

        return found.get();
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

    @Override
    public int hashCode() {
        int result = Objects.hash(charset);
        result = 31 * result + Arrays.hashCode(byteOrderMark);
        return result;
    }

    public String toString() {
        return getNames().getFirst();
    }

    public boolean hasByteOrderMark() {
        return byteOrderMark != null;
    }
}
