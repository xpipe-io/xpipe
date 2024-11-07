package io.xpipe.app.prefs;

import io.xpipe.app.ext.PrefsChoiceValue;
import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.base.ChoiceComp;
import io.xpipe.app.util.OptionsBuilder;

public class SystemCategory extends AppPrefsCategory {

    @Override
    protected String getId() {
        return "system";
    }

    public Comp<?> create() {
        var prefs = AppPrefs.get();
        var builder = new OptionsBuilder();
        builder.addTitle("appBehaviour")
                .sub(new OptionsBuilder()
                        .pref(prefs.startupBehaviour)
                        .addComp(ChoiceComp.ofTranslatable(
                                        prefs.startupBehaviour,
                                        PrefsChoiceValue.getSupported(StartupBehaviour.class),
                                        false)
                                .minWidth(300))
                        .pref(prefs.closeBehaviour)
                        .addComp(ChoiceComp.ofTranslatable(
                                        prefs.closeBehaviour,
                                        PrefsChoiceValue.getSupported(CloseBehaviour.class),
                                        false)
                                .minWidth(300)))
                .addTitle("advanced")
                .sub(new OptionsBuilder().pref(prefs.developerMode).addToggle(prefs.developerMode))
                .addTitle("updates")
                .sub(new OptionsBuilder()
                        .pref(prefs.automaticallyCheckForUpdates)
                        .addToggle(prefs.automaticallyCheckForUpdates));
        return builder.buildComp();
    }
}
