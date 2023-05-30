package io.xpipe.app.browser;

import io.xpipe.app.comp.base.SimpleTitledPaneComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.impl.VerticalComp;
import io.xpipe.app.fxcomps.util.BindingsHelper;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.core.process.ShellControl;
import io.xpipe.core.store.FileSystem;
import javafx.collections.FXCollections;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import lombok.SneakyThrows;

import java.util.List;

public class BrowserOverviewComp extends SimpleComp {

    private final OpenFileSystemModel model;

    public BrowserOverviewComp(OpenFileSystemModel model) {
        this.model = model;
    }

    @Override
    @SneakyThrows
    protected Region createSimple() {
        ShellControl sc = model.getFileSystem().getShell().orElseThrow();
        // TODO: May be move this into another thread
        var common = sc.getOsType().determineInterestingPaths(sc).stream()
                .map(s -> FileSystem.FileEntry.ofDirectory(model.getFileSystem(), s))
                .filter(entry -> {
                    try {
                        var b = sc.getShellDialect().directoryExists(sc, entry.getPath()).executeAndCheck();
                        return b;
                    } catch (Exception e) {
                        ErrorEvent.fromThrowable(e).handle();
                        return false;
                    }
                })
                .toList();
        var commonOverview = new BrowserFileOverviewComp(model, FXCollections.observableArrayList(common), false);
        var commonPane = new SimpleTitledPaneComp(AppI18n.observable("common"), commonOverview).apply(struc -> VBox.setVgrow(struc.get(), Priority.NEVER));

        var roots = sc.getShellDialect()
                .listRoots(sc)
                .map(s -> FileSystem.FileEntry.ofDirectory(model.getFileSystem(), s))
                .toList();
        var rootsOverview = new BrowserFileOverviewComp(model, FXCollections.observableArrayList(roots), false);
        var rootsPane = new SimpleTitledPaneComp(AppI18n.observable("roots"), rootsOverview);

        var recent = BindingsHelper.mappedContentBinding(
                model.getSavedState().getRecentDirectories(),
                s -> FileSystem.FileEntry.ofDirectory(model.getFileSystem(), s.getDirectory()));
        var recentOverview = new BrowserFileOverviewComp(model, recent, true);
        var recentPane = new SimpleTitledPaneComp(AppI18n.observable("recent"), recentOverview);

        var vbox = new VerticalComp(List.of(commonPane, rootsPane, recentPane)).styleClass("overview");
        return vbox.createRegion();
    }
}
