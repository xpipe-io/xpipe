package io.xpipe.core.charsetter;

import lombok.AllArgsConstructor;
import lombok.Value;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Locale;

@Value
@AllArgsConstructor
public class CharsetterContext {

    String systemCharsetName;
    Locale systemLocale;
    Locale appLocale;
    List<String> observedCharsets;

    public static CharsetterContext empty() {
        return new CharsetterContext(Charset.defaultCharset().name(), Locale.getDefault(), Locale.getDefault(), List.of());
    }
}
