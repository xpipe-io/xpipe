package io.xpipe.app.prefs;

import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.util.OptionsBuilder;

public class SecurityCategory extends AppPrefsCategory {

    @Override
    protected String getId() {
        return "security";
    }

    public Comp<?> create() {
        var prefs = AppPrefs.get();
        var builder = new OptionsBuilder();
        builder.addTitle("securityPolicy")
                .sub(new OptionsBuilder()
                        .pref(prefs.checkForSecurityUpdates)
                        .addToggle(prefs.checkForSecurityUpdates)
                        .pref(prefs.alwaysConfirmElevation)
                        .addToggle(prefs.alwaysConfirmElevation)
                        .pref(prefs.dontCachePasswords)
                        .addToggle(prefs.dontCachePasswords)
                        .pref(prefs.denyTempScriptCreation)
                        .addToggle(prefs.denyTempScriptCreation)
                        .pref(prefs.disableCertutilUse)
                        .addToggle(prefs.disableCertutilUse)
                        .pref(prefs.dontAcceptNewHostKeys)
                        .addToggle(prefs.dontAcceptNewHostKeys)
                        .pref(prefs.dontAutomaticallyStartVmSshServer)
                        .addToggle(prefs.dontAutomaticallyStartVmSshServer)
                        .pref(prefs.disableTerminalRemotePasswordPreparation)
                        .addToggle(prefs.disableTerminalRemotePasswordPreparation)
                        .pref(prefs.dontAllowTerminalRestart)
                        .addToggle(prefs.dontAllowTerminalRestart));
        return builder.buildComp();
    }
}
