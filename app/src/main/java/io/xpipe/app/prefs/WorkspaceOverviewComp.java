package io.xpipe.app.prefs;

import io.xpipe.app.comp.RegionBuilder;
import io.xpipe.app.comp.SimpleRegionBuilder;
import io.xpipe.app.comp.base.ListBoxViewComp;
import io.xpipe.app.comp.base.TileButtonComp;
import io.xpipe.app.comp.base.VerticalComp;
import io.xpipe.app.core.AppI18n;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.scene.layout.Region;

import java.util.List;

public class WorkspaceOverviewComp extends SimpleRegionBuilder {

    @Override
    protected Region createSimple() {
        var list = WorkspaceManager.get().getWorkspaces();
        var box = new ListBoxViewComp<>(
                list,
                list,
                s -> {
                    var comp = new WorkspaceEntryComp(s);
                    return comp;
                },
                false);
        box.apply(struc -> {
            struc.minHeightProperty().bind(((Region) struc.getContent()).heightProperty());
        });
        var addButton = new TileButtonComp(
                AppI18n.observable("addWorkspace"),
                AppI18n.observable("addWorkspaceDescription"),
                new ReadOnlyStringWrapper("mdrmz-playlist_add"),
                e -> {
                    WorkspaceManager.get().show();
                    e.consume();
                });
        addButton.setIconSize(1.0);
        addButton.maxWidth(2000);
        var vbox = new VerticalComp(List.of(box, RegionBuilder.hseparator(), addButton));
        vbox.spacing(10);
        return vbox.build();
    }
}
