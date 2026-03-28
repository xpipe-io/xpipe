package io.xpipe.app.prefs;

import io.xpipe.app.comp.SimpleRegionBuilder;
import io.xpipe.app.comp.base.HorizontalComp;
import io.xpipe.app.comp.base.IconButtonComp;
import io.xpipe.app.comp.base.TileButtonComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.platform.LabelGraphic;
import io.xpipe.app.util.DesktopHelper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.layout.Region;

import java.util.List;

public class WorkspaceEntryComp extends SimpleRegionBuilder {

    private final WorkspaceEntry workspace;

    public WorkspaceEntryComp(WorkspaceEntry workspace) {
        this.workspace = workspace;
    }

    @Override
    protected Region createSimple() {
        var view = new IconButtonComp(new LabelGraphic.IconGraphic("mdi2f-folder-open-outline"), () -> {
            DesktopHelper.browseFile(workspace.getDir());
        });
        view.describe(d -> d.nameKey("browse"));
        var buttons = new HorizontalComp(List.of(view));
        var header = workspace.getName() + (workspace.equals(WorkspaceManager.get().getCurrent()) ? " (" + AppI18n.get("active") + ")" : "");
        var tile = new TileButtonComp(
                new ReadOnlyStringWrapper(header),
                new ReadOnlyStringWrapper(workspace.getDir().toString()),
                new ReadOnlyStringWrapper("mdal-home_work"),
                e -> {
                    WorkspaceManager.get().open(workspace);
                    e.consume();
                });
        tile.setRight(buttons);
        tile.setIconSize(1.0);
        tile.maxWidth(2000);
        return tile.build();
    }
}
