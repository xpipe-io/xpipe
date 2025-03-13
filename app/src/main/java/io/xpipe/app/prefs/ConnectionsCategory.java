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
        var connectionsBuilder = new OptionsBuilder().pref(prefs.condenseConnectionDisplay).addToggle(prefs.condenseConnectionDisplay).pref(
                prefs.showChildCategoriesInParentCategory).addToggle(prefs.showChildCategoriesInParentCategory).pref(
                prefs.openConnectionSearchWindowOnConnectionCreation).addToggle(prefs.openConnectionSearchWindowOnConnectionCreation).pref(
                prefs.requireDoubleClickForConnections).addToggle(prefs.requireDoubleClickForConnections);
        var localShellBuilder = new OptionsBuilder().pref(prefs.useLocalFallbackShell).addToggle(prefs.useLocalFallbackShell);
        // Change order to prioritize fallback shell on macOS
        var options = OsType.getLocal() == OsType.MACOS ?  new OptionsBuilder()
                .addTitle("localShell")
                .sub(localShellBuilder)
                .addTitle("connections")
                .sub(connectionsBuilder) :
                new OptionsBuilder()
                .addTitle("connections")
                .sub(connectionsBuilder)
                .addTitle("localShell")
                .sub(localShellBuilder);
        if (OsType.getLocal() == OsType.WINDOWS) {
            options.addTitle("sshConfiguration")
                    .sub(new OptionsBuilder()
                            .addComp(prefs.getCustomComp("x11WslInstance")));
        }
        return options.buildComp();
    }
}
