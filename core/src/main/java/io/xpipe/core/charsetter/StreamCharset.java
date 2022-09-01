package io.xpipe.core.charsetter;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Value;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

@Value
public class StreamCharset {

    public static StreamCharset get(Charset charset, boolean byteOrderMark) {
        return Stream.concat(COMMON.stream(), RARE.stream())
                .filter(streamCharset -> streamCharset.getCharset()
                        .equals(charset) && streamCharset.hasByteOrderMark() == byteOrderMark)
                .findFirst()
                .orElseThrow();
    }

    @JsonCreator
    public static StreamCharset get(String s) {
        var byteOrderMark = s.endsWith("-bom");
        var charset = Charset.forName(s.substring(
                0, s.length() - (byteOrderMark ?
                        4 :
                        0)));
        return StreamCharset.get(charset, byteOrderMark);
    }

    Charset charset;
    byte[] byteOrderMark;

    @JsonValue
    public String toString() {
        return getCharset()
                .name().toLowerCase(Locale.ROOT) + (hasByteOrderMark() ?
                "-bom" :
                "");
    }

    public static final StreamCharset UTF8 = new StreamCharset(StandardCharsets.UTF_8, null);
    public static final StreamCharset UTF8_BOM = new StreamCharset(StandardCharsets.UTF_8, new byte[]{
            (byte) 0xEF,
            (byte) 0xBB,
            (byte) 0xBF
    });

    public static final StreamCharset UTF16 = new StreamCharset(StandardCharsets.UTF_16, null);
    public static final StreamCharset UTF16_BOM = new StreamCharset(StandardCharsets.UTF_16, new byte[]{
            (byte) 0xFE,
            (byte) 0xFF
    });

    public static final StreamCharset UTF16_LE = new StreamCharset(StandardCharsets.UTF_16LE, null);
    public static final StreamCharset UTF16_LE_BOM = new StreamCharset(StandardCharsets.UTF_16LE, new byte[]{
            (byte) 0xFF,
            (byte) 0xFE
    });

    public static final StreamCharset UTF32 = new StreamCharset(Charset.forName("utf-32"), null);
    public static final StreamCharset UTF32_BOM = new StreamCharset(Charset.forName("utf-32"), new byte[]{
            0x00,
            0x00,
            (byte) 0xFE,
            (byte) 0xFF
    });

    public static final List<StreamCharset> COMMON = List.of(
            UTF8, UTF8_BOM, UTF16, UTF16_BOM, UTF16_LE, UTF16_LE_BOM, UTF32, UTF32_BOM, new StreamCharset(StandardCharsets.US_ASCII, null),
            new StreamCharset(StandardCharsets.ISO_8859_1, null),
            new StreamCharset(Charset.forName("Windows-1251"), null), new StreamCharset(Charset.forName("Windows-1252"), null)
    );

    public static final List<StreamCharset> RARE = Charset.availableCharsets()
            .values()
            .stream()
            .filter(charset -> COMMON.stream()
                    .noneMatch(c -> c.getCharset()
                            .equals(charset)))
            .map(charset -> new StreamCharset(charset, null))
            .toList();

    public boolean hasByteOrderMark() {
        return byteOrderMark != null;
    }
}
