package io.xpipe.app.prefs;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.platform.LabelGraphic;
import io.xpipe.app.platform.OptionsBuilder;

public class ConnectionHubCategory extends AppPrefsCategory {

    @Override
    protected String getId() {
        return "connectionHub";
    }

    @Override
    protected LabelGraphic getIcon() {
        return new LabelGraphic.IconGraphic("mdi2c-connection");
    }

    @Override
    protected Comp<?> create() {
        var prefs = AppPrefs.get();
        var connectionsBuilder = new OptionsBuilder()
                .pref(prefs.condenseConnectionDisplay)
                .addToggle(prefs.condenseConnectionDisplay)
                .pref(prefs.showChildCategoriesInParentCategory)
                .addToggle(prefs.showChildCategoriesInParentCategory)
                .pref(prefs.openConnectionSearchWindowOnConnectionCreation)
                .addToggle(prefs.openConnectionSearchWindowOnConnectionCreation)
                .pref(prefs.requireDoubleClickForConnections)
                .addToggle(prefs.requireDoubleClickForConnections);
        var options = new OptionsBuilder().addTitle("connectionHub").sub(connectionsBuilder);
        return options.buildComp();
    }
}
