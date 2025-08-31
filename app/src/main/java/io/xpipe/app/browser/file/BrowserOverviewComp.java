package io.xpipe.app.browser.file;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.SimpleComp;
import io.xpipe.app.comp.base.SimpleTitledPaneComp;
import io.xpipe.app.comp.base.VerticalComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.FileEntry;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.process.OsFileSystem;
import io.xpipe.app.process.ShellControl;
import io.xpipe.app.platform.DerivedObservableList;
import io.xpipe.app.util.ThreadHelper;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import lombok.SneakyThrows;

import java.util.ArrayList;
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

        var list = new ArrayList<Comp<?>>();

        var recent = DerivedObservableList.wrap(model.getSavedState().getRecentDirectories(), true)
                .mapped(s -> FileEntry.ofDirectory(model.getFileSystem(), s.getDirectory()))
                .getList();
        var recentOverview = new BrowserFileOverviewComp(model, recent, true);
        var recentPane = new SimpleTitledPaneComp(AppI18n.observable("recent"), recentOverview, false);
        list.add(recentPane);

        var sc = model.getFileSystem().getShell();
        if (sc.isPresent()) {
            var commonPlatform = FXCollections.<FileEntry>synchronizedObservableList(FXCollections.observableArrayList());
            ThreadHelper.runFailableAsync(() -> {
                var common = OsFileSystem.of(sc.get().getOsType()).determineInterestingPaths(sc.get()).stream().map(
                        s -> FileEntry.ofDirectory(model.getFileSystem(), s)).filter(entry -> {
                    var fs = model.getFileSystem();

                    try {
                        return fs.directoryExists(entry.getPath());
                    } catch (Exception e) {
                        ErrorEventFactory.fromThrowable(e).handle();
                        return false;
                    }
                }).toList();
                Platform.runLater(() -> {
                    commonPlatform.setAll(common);
                });
            });
            var commonOverview = new BrowserFileOverviewComp(model, commonPlatform, false);
            var commonPane = new SimpleTitledPaneComp(AppI18n.observable("common"), commonOverview, false).apply(
                    struc -> VBox.setVgrow(struc.get(), Priority.NEVER));
            list.add(commonPane);
        }

        var roots = model.getFileSystem().listRoots().stream()
                .map(s -> FileEntry.ofDirectory(model.getFileSystem(), s))
                .toList();
        var rootsOverview = new BrowserFileOverviewComp(model, FXCollections.observableArrayList(roots), false);
        var rootsPane = new SimpleTitledPaneComp(AppI18n.observable("roots"), rootsOverview, false);
        list.add(rootsPane);

        var vbox = new VerticalComp(list).styleClass("overview");
        var r = vbox.createRegion();
        return r;
    }
}
