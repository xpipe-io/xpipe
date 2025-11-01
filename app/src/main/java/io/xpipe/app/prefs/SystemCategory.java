package io.xpipe.app.prefs;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.base.ChoiceComp;
import io.xpipe.app.ext.PrefsChoiceValue;
import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.ext.ShellDialectChoiceComp;
import io.xpipe.app.platform.LabelGraphic;
import io.xpipe.app.platform.OptionsBuilder;

public class SystemCategory extends AppPrefsCategory {

    @Override
    protected String getId() {
        return "system";
    }

    @Override
    protected LabelGraphic getIcon() {
        return new LabelGraphic.IconGraphic("mdi2d-desktop-classic");
    }

    public Comp<?> create() {
        var prefs = AppPrefs.get();
        var builder = new OptionsBuilder();
        builder.addTitle("system")
                .sub(new OptionsBuilder()
                        .pref(prefs.startupBehaviour)
                        .addComp(ChoiceComp.ofTranslatable(
                                        prefs.startupBehaviour,
                                        PrefsChoiceValue.getSupported(StartupBehaviour.class),
                                        false)
                                .maxWidth(getCompWidth()))
                        .pref(prefs.closeBehaviour)
                        .addComp(ChoiceComp.ofTranslatable(
                                        prefs.closeBehaviour,
                                        PrefsChoiceValue.getSupported(CloseBehaviour.class),
                                        false)
                                .maxWidth(getCompWidth()))
                        .pref(prefs.hibernateBehaviour)
                        .addComp(ChoiceComp.ofTranslatable(
                                        prefs.hibernateBehaviour,
                                        PrefsChoiceValue.getSupported(HibernateBehaviour.class),
                                        true)
                                .maxWidth(getCompWidth()))
                        .pref(prefs.localShellDialect)
                        .addComp(
                                new ShellDialectChoiceComp(
                                                ProcessControlProvider.get().getAvailableLocalDialects(),
                                                prefs.localShellDialect,
                                                ShellDialectChoiceComp.NullHandling.NULL_DISABLED)
                                        .maxWidth(getCompWidth()),
                                prefs.localShellDialect)
                        .pref(prefs.developerMode)
                        .addToggle(prefs.developerMode));
        return builder.buildComp();
    }
}
