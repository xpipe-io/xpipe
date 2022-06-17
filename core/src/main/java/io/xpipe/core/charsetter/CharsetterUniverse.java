package io.xpipe.core.charsetter;

import lombok.AllArgsConstructor;
import lombok.Value;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Value
@AllArgsConstructor
public class CharsetterUniverse {

    List<Charset> charsets;

    public static CharsetterUniverse create(CharsetterContext ctx) {
        List<Charset> cs = new ArrayList<>();

        cs.add(StandardCharsets.UTF_8);

        var system = Charset.forName(ctx.getSystemCharsetName());
        cs.add(system);

        // TODO: Locales

        var observed = ctx.getObservedCharsets().stream().map(Charset::forName).toList();
        cs.addAll(observed);

        return new CharsetterUniverse(cs);
    }
}
