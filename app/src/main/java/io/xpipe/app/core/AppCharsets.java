package io.xpipe.app.core;

import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.core.charsetter.Charsetter;
import io.xpipe.core.charsetter.CharsetterContext;
import io.xpipe.core.charsetter.StreamCharset;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AppCharsets {

    private static final List<String> observedCharsets = new ArrayList<>();

    public static void init() {
        var system = System.getProperty("file.encoding");
        var systemLocale = Locale.getDefault();
        var appLocale = AppPrefs.get().language.getValue().getLocale();
        var used = AppCache.get("observedCharsets", List.class, () -> new ArrayList<String>());
        var ctx = new CharsetterContext(system, systemLocale, appLocale, used);
        Charsetter.init(ctx);
    }

    public static void observe(StreamCharset c) {
        if (c == null) {
            return;
        }

        var used = AppCache.get("observedCharsets", List.class, () -> new ArrayList<String>());
        used.add(c.getCharset().name());
        AppCache.update("observedCharsets", used);

        init();
    }
}
