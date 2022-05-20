package io.xpipe.charsetter;

import lombok.AllArgsConstructor;
import lombok.Value;

import java.util.List;
import java.util.Locale;

@Value
@AllArgsConstructor
public class CharsetterContext {

    String systemCharsetName;

    Locale systemLocale;

    Locale appLocale;

    List<String> observedCharsets;
}
