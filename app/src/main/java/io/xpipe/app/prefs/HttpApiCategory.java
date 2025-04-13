package io.xpipe.app.prefs;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.comp.base.TextFieldComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.util.DocumentationLink;
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
                            DocumentationLink.API.open();
                        }))
                        .pref(prefs.apiKey)
                        .addComp(new TextFieldComp(prefs.apiKey).maxWidth(getCompWidth()), prefs.apiKey)
                        .pref(prefs.disableApiAuthentication)
                        .addToggle(prefs.disableApiAuthentication))
                .buildComp();
    }
}
