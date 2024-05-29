package io.xpipe.app.browser;

import io.xpipe.app.browser.file.BrowserFileOverviewComp;
import io.xpipe.app.browser.fs.OpenFileSystemModel;
import io.xpipe.app.comp.base.SimpleTitledPaneComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.impl.VerticalComp;
import io.xpipe.app.fxcomps.util.DerivedObservableList;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.core.process.ShellControl;
import io.xpipe.core.store.FileSystem;
import javafx.application.Platform;
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
        // The open file system might have already been closed
        if (model.getFileSystem() == null) {
            return new Region();
        }

        ShellControl sc = model.getFileSystem().getShell().orElseThrow();

        var commonPlatform = FXCollections.<FileSystem.FileEntry>observableArrayList();
        ThreadHelper.runFailableAsync(() -> {
            var common = sc.getOsType().determineInterestingPaths(sc).stream()
                    .map(s -> FileSystem.FileEntry.ofDirectory(model.getFileSystem(), s))
                    .filter(entry -> {
                        try {
                            return sc.getShellDialect()
                                    .directoryExists(sc, entry.getPath())
                                    .executeAndCheck();
                        } catch (Exception e) {
                            ErrorEvent.fromThrowable(e).handle();
                            return false;
                        }
                    })
                    .toList();
            Platform.runLater(() -> {
                commonPlatform.setAll(common);
            });
        });
        var commonOverview = new BrowserFileOverviewComp(model, commonPlatform, false);
        var commonPane = new SimpleTitledPaneComp(AppI18n.observable("common"), commonOverview)
                .apply(struc -> VBox.setVgrow(struc.get(), Priority.NEVER));

        var roots = sc.getShellDialect()
                .listRoots(sc)
                .map(s -> FileSystem.FileEntry.ofDirectory(model.getFileSystem(), s))
                .toList();
        var rootsOverview = new BrowserFileOverviewComp(model, FXCollections.observableArrayList(roots), false);
        var rootsPane = new SimpleTitledPaneComp(AppI18n.observable("roots"), rootsOverview);

        var recent = new DerivedObservableList<>(model.getSavedState().getRecentDirectories(), true).mapped(
                s -> FileSystem.FileEntry.ofDirectory(model.getFileSystem(), s.getDirectory())).getList();
        var recentOverview = new BrowserFileOverviewComp(model, recent, true);
        var recentPane = new SimpleTitledPaneComp(AppI18n.observable("recent"), recentOverview);

        var vbox = new VerticalComp(List.of(recentPane, commonPane, rootsPane)).styleClass("overview");
        return vbox.createRegion();
    }
}
