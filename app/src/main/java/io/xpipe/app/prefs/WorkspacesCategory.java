package io.xpipe.app.prefs;

import io.xpipe.app.comp.BaseRegionBuilder;
import io.xpipe.app.comp.RegionBuilder;
import io.xpipe.app.core.AppProperties;
import io.xpipe.app.platform.LabelGraphic;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.util.DocumentationLink;

public class WorkspacesCategory extends AppPrefsCategory {

    @Override
    protected String getId() {
        return "workspaces";
    }

    @Override
    protected LabelGraphic getIcon() {
        return new LabelGraphic.IconGraphic("mdal-corporate_fare");
    }

    @Override
    protected BaseRegionBuilder<?, ?> create() {
        return new OptionsBuilder()
                .title("manageWorkspaces")
                .sub(new OptionsBuilder()
                        .nameAndDescription("workspaceManagement")
                        .documentationLink(DocumentationLink.WORKSPACES)
                        .licenseRequirement("workspaces")
                        // For some reason, creating this comp in AOT train mode causes freezes
                        // at least when the AOT cache is generated
                        .addComp(AppProperties.get().isAotTrainMode() ? RegionBuilder.empty() : new WorkspaceOverviewComp().maxWidth(getCompWidth())))
                .buildComp();
    }
}
