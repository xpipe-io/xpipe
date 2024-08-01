package io.xpipe.app.prefs;

import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.util.OptionsBuilder;

public class ProfilesCategory extends AppPrefsCategory {

    @Override
    protected String getId() {
        return "profiles";
    }

    @Override
    protected Comp<?> create() {
        return new OptionsBuilder()
                .addTitle("manageProfiles")
                .sub(new OptionsBuilder()
                        .nameAndDescription("profileAdd")
                        .addComp(
                                new ButtonComp(AppI18n.observable("addProfile"),
                                        ProfileCreationAlert::showAsync)))
                .buildComp();
    }
}
