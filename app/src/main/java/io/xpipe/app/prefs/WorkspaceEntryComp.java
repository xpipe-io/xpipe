package io.xpipe.app.prefs;

import io.xpipe.app.comp.SimpleRegionBuilder;
import io.xpipe.app.comp.base.HorizontalComp;
import io.xpipe.app.comp.base.IconButtonComp;
import io.xpipe.app.comp.base.TileButtonComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.window.AppDialog;
import io.xpipe.app.platform.LabelGraphic;
import io.xpipe.app.util.DesktopHelper;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleBooleanProperty;
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
        var available = workspace.isAvailable();
        view.disable(!available);
        var current = workspace.equals(WorkspaceManager.get().getCurrent());
        var delete = new IconButtonComp(new LabelGraphic.IconGraphic("mdi2t-trash-can-outline"), () -> {
            if (AppDialog.confirm("workspaceDeletion")) {
                WorkspaceManager.get().removeWorkspace(workspace);
            }
        });
        delete.describe(d -> d.nameKey("delete"));
        delete.disable(!available);
        delete.hide(new SimpleBooleanProperty(current));
        var buttons = new HorizontalComp(List.of(view, delete));
        buttons.spacing(7);
        var header = workspace.getName()
                + (current ? " (" + AppI18n.get("active") + ")" : "")
                + (!available ? " (" + AppI18n.get("unavailable") + ")" : "");
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
        // Don't disable the whole tile, as that would also disable the delete button
        tile.applyStructure(struc -> {
            struc.getGraphic().setDisable(!available);
            struc.getName().setDisable(!available);
            struc.getDescription().setDisable(!available);
        });
        return tile.build();
    }
}
