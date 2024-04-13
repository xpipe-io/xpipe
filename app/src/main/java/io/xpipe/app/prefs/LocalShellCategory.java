package io.xpipe.app.prefs;

import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.util.OptionsBuilder;

public class LocalShellCategory extends AppPrefsCategory {

    @Override
    protected String getId() {
        return "localShell";
    }

    @Override
    protected Comp<?> create() {
        var prefs = AppPrefs.get();
        return new OptionsBuilder()
                .addTitle("localShell")
                .sub(new OptionsBuilder()
                        .nameAndDescription("useLocalFallbackShell")
                        .addToggle(prefs.useLocalFallbackShell))
                .buildComp();
    }
}
