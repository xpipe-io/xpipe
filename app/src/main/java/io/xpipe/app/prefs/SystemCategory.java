package io.xpipe.app.prefs;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.base.ChoiceComp;
import io.xpipe.app.ext.PrefsChoiceValue;
import io.xpipe.app.util.OptionsBuilder;

public class SystemCategory extends AppPrefsCategory {

    @Override
    protected String getId() {
        return "system";
    }

    public Comp<?> create() {
        var prefs = AppPrefs.get();
        var builder = new OptionsBuilder();
        var localShellBuilder =
                new OptionsBuilder().pref(prefs.useLocalFallbackShell).addToggle(prefs.useLocalFallbackShell);
        builder.addTitle("system")
                .sub(new OptionsBuilder()
                        .pref(prefs.startupBehaviour)
                        .addComp(ChoiceComp.ofTranslatable(
                                        prefs.startupBehaviour,
                                        PrefsChoiceValue.getSupported(StartupBehaviour.class),
                                        false)
                                .minWidth(getCompWidth() / 2.0))
                        .pref(prefs.closeBehaviour)
                        .addComp(ChoiceComp.ofTranslatable(
                                        prefs.closeBehaviour,
                                        PrefsChoiceValue.getSupported(CloseBehaviour.class),
                                        false)
                                .minWidth(getCompWidth() / 2.0)));
        builder.sub(localShellBuilder);
        builder.sub(new OptionsBuilder().pref(prefs.developerMode).addToggle(prefs.developerMode));
        return builder.buildComp();
    }
}
