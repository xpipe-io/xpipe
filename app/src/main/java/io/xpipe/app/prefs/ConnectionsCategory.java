package io.xpipe.app.prefs;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.util.OptionsBuilder;
import io.xpipe.core.process.OsType;

public class ConnectionsCategory extends AppPrefsCategory {

    @Override
    protected String getId() {
        return "connections";
    }

    @Override
    protected Comp<?> create() {
        var prefs = AppPrefs.get();
        var options = new OptionsBuilder().addTitle("localShell").sub(
                new OptionsBuilder().pref(prefs.useLocalFallbackShell).addToggle(prefs.useLocalFallbackShell)).addTitle("connections").sub(
                new OptionsBuilder().pref(prefs.condenseConnectionDisplay)
                        .addToggle(prefs.condenseConnectionDisplay)
                        .pref(prefs.showChildCategoriesInParentCategory)
                        .addToggle(prefs.showChildCategoriesInParentCategory)
                        .pref(prefs.openConnectionSearchWindowOnConnectionCreation)
                        .addToggle(prefs.openConnectionSearchWindowOnConnectionCreation)
                        .pref(prefs.requireDoubleClickForConnections)
                        .addToggle(prefs.requireDoubleClickForConnections));
        if (OsType.getLocal() == OsType.WINDOWS) {
            options
                    .addTitle("sshConfiguration")
                    .sub(new OptionsBuilder()
                            .pref(prefs.useBundledTools)
                            .addToggle(prefs.useBundledTools)
                            .addComp(prefs.getCustomComp("x11WslInstance")));
        }
        return options
                .buildComp();
    }
}
