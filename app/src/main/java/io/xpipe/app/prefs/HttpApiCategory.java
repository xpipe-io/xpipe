package io.xpipe.app.prefs;

import io.xpipe.app.beacon.AppBeaconServer;
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
                        .nameAndDescription("httpServerPort")
                        .addInteger(prefs.httpServerPort)
                        .disable(AppBeaconServer.get().isPropertyPort())
                        .nameAndDescription("apiKey")
                        .addString(prefs.apiKey)
                        .nameAndDescription("disableApiAuthentication")
                        .addToggle(prefs.disableApiAuthentication)
                )
                .buildComp();
    }
}
