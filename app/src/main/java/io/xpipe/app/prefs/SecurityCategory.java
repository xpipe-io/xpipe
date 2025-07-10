package io.xpipe.app.prefs;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.util.LabelGraphic;
import io.xpipe.app.util.OptionsBuilder;

public class SecurityCategory extends AppPrefsCategory {

    @Override
    protected String getId() {
        return "security";
    }

    @Override
    protected LabelGraphic getIcon() {
        return new LabelGraphic.IconGraphic("mdi2s-security-network");
    }

    public Comp<?> create() {
        var prefs = AppPrefs.get();
        var builder = new OptionsBuilder();
        builder.addTitle("security")
                .sub(new OptionsBuilder()
                        .pref(prefs.alwaysConfirmElevation)
                        .addToggle(prefs.alwaysConfirmElevation)
                        .pref(prefs.dontCachePasswords)
                        .addToggle(prefs.dontCachePasswords)
                        .pref(prefs.disableCertutilUse)
                        .addToggle(prefs.disableCertutilUse)
                        .pref(prefs.dontAcceptNewHostKeys)
                        .addToggle(prefs.dontAcceptNewHostKeys)
                        .pref(prefs.disableSshPinCaching)
                        .addToggle(prefs.disableSshPinCaching)
                        .pref(prefs.dontAutomaticallyStartVmSshServer)
                        .addToggle(prefs.dontAutomaticallyStartVmSshServer)
                        .pref(prefs.disableTerminalRemotePasswordPreparation)
                        .addToggle(prefs.disableTerminalRemotePasswordPreparation)
                        .pref(prefs.disableApiHttpsTlsCheck)
                        .addToggle(prefs.disableApiHttpsTlsCheck));
        return builder.buildComp();
    }
}
