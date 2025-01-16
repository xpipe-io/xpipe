package io.xpipe.app.prefs;

import io.xpipe.app.beacon.AppBeaconServer;
import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.util.Hyperlinks;
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
                        .pref(prefs.enableHttpApi)
                        .addToggle(prefs.enableHttpApi)
                        .nameAndDescription("openApiDocs")
                        .addComp(new ButtonComp(AppI18n.observable("openApiDocsButton"), () -> {
                            Hyperlinks.open(
                                    "http://localhost:" + AppBeaconServer.get().getPort());
                        }))
                        .pref(prefs.apiKey)
                        .addString(prefs.apiKey)
                        .pref(prefs.disableApiAuthentication)
                        .addToggle(prefs.disableApiAuthentication))
                .buildComp();
    }
}
