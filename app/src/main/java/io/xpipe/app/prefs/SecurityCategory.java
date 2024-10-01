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
                        .nameAndDescription("alwaysConfirmElevation")
                        .addToggle(prefs.alwaysConfirmElevation)
                        .nameAndDescription("dontCachePasswords")
                        .addToggle(prefs.dontCachePasswords)
                        .nameAndDescription("denyTempScriptCreation")
                        .addToggle(prefs.denyTempScriptCreation)
                        .nameAndDescription("disableCertutilUse")
                        .addToggle(prefs.disableCertutilUse)
                        .nameAndDescription("dontAcceptNewHostKeys")
                        .addToggle(prefs.dontAcceptNewHostKeys)
                        .nameAndDescription("dontAutomaticallyStartVmSshServer")
                        .addToggle(prefs.dontAutomaticallyStartVmSshServer)
                        .nameAndDescription("disableTerminalRemotePasswordPreparation")
                        .addToggle(prefs.disableTerminalRemotePasswordPreparation)
                        .nameAndDescription("dontAllowTerminalRestart")
                        .addToggle(prefs.dontAllowTerminalRestart)
                );
        return builder.buildComp();
    }
}
