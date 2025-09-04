package io.xpipe.app.prefs;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.platform.LabelGraphic;
import io.xpipe.app.platform.OptionsBuilder;

public class UpdatesCategory extends AppPrefsCategory {

    @Override
    protected String getId() {
        return "updates";
    }

    @Override
    protected LabelGraphic getIcon() {
        return new LabelGraphic.IconGraphic("mdi2d-download-box-outline");
    }

    public Comp<?> create() {
        var prefs = AppPrefs.get();
        var builder = new OptionsBuilder();
        builder.addTitle("updates")
                .sub(new OptionsBuilder()
                        .pref(prefs.automaticallyCheckForUpdates)
                        .addToggle(prefs.automaticallyCheckForUpdates)
                        .pref(prefs.checkForSecurityUpdates)
                        .addToggle(prefs.checkForSecurityUpdates));
        return builder.buildComp();
    }
}
