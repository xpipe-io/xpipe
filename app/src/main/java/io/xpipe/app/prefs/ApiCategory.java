package io.xpipe.app.prefs;

import io.xpipe.app.comp.BaseRegionBuilder;
import io.xpipe.app.comp.base.TextFieldComp;
import io.xpipe.app.platform.LabelGraphic;
import io.xpipe.app.platform.OptionsBuilder;

public class ApiCategory extends AppPrefsCategory {

    @Override
    protected String getId() {
        return "api";
    }

    @Override
    protected LabelGraphic getIcon() {
        return new LabelGraphic.IconGraphic("mdi2c-code-json");
    }

    @Override
    protected BaseRegionBuilder<?, ?> create() {
        var prefs = AppPrefs.get();

        return new OptionsBuilder()
                .addTitle("httpServer")
                .sub(new OptionsBuilder()
                        .pref(prefs.enableHttpApi)
                        .addToggle(prefs.enableHttpApi)
                        .pref(prefs.apiKey)
                        .addComp(new TextFieldComp(prefs.apiKey).maxWidth(getCompWidth()), prefs.apiKey)
                        .pref(prefs.disableApiAuthentication)
                        .addToggle(prefs.disableApiAuthentication))
                .buildComp();
    }
}
