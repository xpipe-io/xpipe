package io.xpipe.app.browser.file;

import io.xpipe.app.comp.SimpleComp;
import io.xpipe.app.comp.base.SimpleTitledPaneComp;
import io.xpipe.app.comp.base.VerticalComp;
import io.xpipe.app.core.AppFont;
import io.xpipe.app.core.AppFontSizes;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.util.DerivedObservableList;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.core.process.ShellControl;
import io.xpipe.core.store.FileEntry;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import lombok.SneakyThrows;

import java.util.List;

public class BrowserOverviewComp extends SimpleComp {

    private final BrowserFileSystemTabModel model;

    public BrowserOverviewComp(BrowserFileSystemTabModel model) {
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

        var commonPlatform = FXCollections.<FileEntry>observableArrayList();
        ThreadHelper.runFailableAsync(() -> {
            var common = sc.getOsType().determineInterestingPaths(sc).stream()
                    .filter(s -> !s.isBlank())
                    .map(s -> FileEntry.ofDirectory(model.getFileSystem(), s))
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
        var commonPane = new SimpleTitledPaneComp(AppI18n.observable("common"), commonOverview, false)
                .apply(struc -> VBox.setVgrow(struc.get(), Priority.NEVER));

        var roots = model.getFileSystem().listRoots().stream()
                .map(s -> FileEntry.ofDirectory(model.getFileSystem(), s))
                .toList();
        var rootsOverview = new BrowserFileOverviewComp(model, FXCollections.observableArrayList(roots), false);
        var rootsPane = new SimpleTitledPaneComp(AppI18n.observable("roots"), rootsOverview, false);

        var recent = new DerivedObservableList<>(model.getSavedState().getRecentDirectories(), true)
                .mapped(s -> FileEntry.ofDirectory(model.getFileSystem(), s.getDirectory()))
                .getList();
        var recentOverview = new BrowserFileOverviewComp(model, recent, true);
        var recentPane = new SimpleTitledPaneComp(AppI18n.observable("recent"), recentOverview, false);

        var vbox = new VerticalComp(List.of(recentPane, commonPane, rootsPane)).styleClass("overview");
        var r = vbox.createRegion();
        AppFontSizes.sm(r);
        return r;
    }
}
