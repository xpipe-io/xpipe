package io.xpipe.app.browser;

import io.xpipe.app.comp.base.SimpleTitledPaneComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.impl.VerticalComp;
import io.xpipe.core.store.FileSystem;
import javafx.collections.FXCollections;
import javafx.scene.layout.Region;

import java.time.Instant;
import java.util.List;

public class BrowserOverviewComp extends SimpleComp {

    private final OpenFileSystemModel model;

    public BrowserOverviewComp(OpenFileSystemModel model) {
        this.model = model;
    }

    @Override
    protected Region createSimple() {
        var commonList = new BrowserSelectionListComp(FXCollections.observableArrayList(
                new FileSystem.FileEntry(model.getFileSystem(), "C:\\", Instant.now(), true, false, false, 0, null)));
        var common = new SimpleTitledPaneComp(AppI18n.observable("a"), commonList);

        var recentList = new BrowserSelectionListComp(FXCollections.observableArrayList(
                new FileSystem.FileEntry(model.getFileSystem(), "C:\\", Instant.now(), true, false, false, 0, null)));
        var recent = new SimpleTitledPaneComp(AppI18n.observable("Recent"), recentList);

        var vbox = new VerticalComp(List.of(common, recent)).styleClass("home");
        return vbox.createRegion();
    }
}
