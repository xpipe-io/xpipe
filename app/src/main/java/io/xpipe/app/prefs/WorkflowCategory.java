package io.xpipe.app.prefs;

import io.xpipe.app.fxcomps.Comp;
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
                .addTitle("workflow")
                .sub(new OptionsBuilder()
                        .nameAndDescription("condenseConnectionDisplay")
                        .addToggle(prefs.condenseConnectionDisplay)
                        .nameAndDescription("showChildCategoriesInParentCategory")
                        .addToggle(prefs.showChildCategoriesInParentCategory)
                        .nameAndDescription("openConnectionSearchWindowOnConnectionCreation")
                        .addToggle(prefs.openConnectionSearchWindowOnConnectionCreation)
                        .nameAndDescription("requireDoubleClickForConnections")
                        .addToggle(prefs.requireDoubleClickForConnections))
                .buildComp();
    }
}
