package io.xpipe.app.prefs;

import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.util.ElevationAccessChoiceComp;
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
                        .nameAndDescription("elevationPolicy")
                        .addComp(new ElevationAccessChoiceComp(prefs.elevationPolicy).minWidth(250), prefs.elevationPolicy)
                        .nameAndDescription("dontCachePasswords")
                        .addToggle(prefs.dontCachePasswords)
                        .nameAndDescription("denyTempScriptCreation")
                        .addToggle(prefs.denyTempScriptCreation)
                        .nameAndDescription("disableCertutilUse")
                        .addToggle(prefs.disableCertutilUse)
                        .nameAndDescription("disableTerminalRemotePasswordPreparation")
                        .addToggle(prefs.disableTerminalRemotePasswordPreparation)
                        .nameAndDescription("dontAcceptNewHostKeys")
                        .addToggle(prefs.dontAcceptNewHostKeys)
                );
        return builder.buildComp();
    }
}
