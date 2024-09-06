package io.xpipe.app.prefs;

import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.util.OptionsBuilder;

public class HttpApiCategory extends AppPrefsCategory {

    @Override
    protected String getId() {
        return "httpApi";
    }

    @Override
    protected Comp<?> create() {
        var prefs = AppPrefs.get();
        return new OptionsBuilder()
                .addTitle("httpServerConfiguration")
                .sub(new OptionsBuilder()
                        .nameAndDescription("enableHttpApi")
                        .addToggle(prefs.enableHttpApi)
                        .nameAndDescription("apiKey")
                        .addString(prefs.apiKey)
                        .nameAndDescription("disableApiAuthentication")
                        .addToggle(prefs.disableApiAuthentication))
                .buildComp();
    }
}
