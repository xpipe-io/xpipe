package io.xpipe.app.prefs;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.util.OptionsBuilder;

public class WorkflowCategory extends AppPrefsCategory {

    @Override
    protected String getId() {
        return "workflow";
    }

    @Override
    protected Comp<?> create() {
        var prefs = AppPrefs.get();
        return new OptionsBuilder()
                .addTitle("connections")
                .sub(new OptionsBuilder()
                        .pref(prefs.condenseConnectionDisplay)
                        .addToggle(prefs.condenseConnectionDisplay)
                        .pref(prefs.showChildCategoriesInParentCategory)
                        .addToggle(prefs.showChildCategoriesInParentCategory)
                        .pref(prefs.openConnectionSearchWindowOnConnectionCreation)
                        .addToggle(prefs.openConnectionSearchWindowOnConnectionCreation)
                        .pref(prefs.requireDoubleClickForConnections)
                        .addToggle(prefs.requireDoubleClickForConnections))
                .addTitle("fileBrowser")
                .sub(new OptionsBuilder().pref(prefs.confirmAllDeletions).addToggle(prefs.confirmAllDeletions))
                .buildComp();
    }
}
