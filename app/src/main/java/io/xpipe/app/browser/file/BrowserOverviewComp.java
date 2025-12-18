package io.xpipe.app.browser.file;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.SimpleComp;
import io.xpipe.app.comp.base.SimpleTitledPaneComp;
import io.xpipe.app.comp.base.VerticalComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.FileEntry;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.platform.DerivedObservableList;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.core.FilePath;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import lombok.SneakyThrows;

import java.util.ArrayList;

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
        recentPane.hide(Bindings.isEmpty(recent));
        list.add(recentPane);

        var commonPlatform = FXCollections.<FileEntry>synchronizedObservableList(FXCollections.observableArrayList());
        ThreadHelper.runFailableAsync(() -> {
            try {
                var all = new ArrayList<FileEntry>();
                for (FilePath cd : model.getFileSystem().listCommonDirectories()) {
                    var entry = FileEntry.ofDirectory(model.getFileSystem(), cd);
                    var fs = model.getFileSystem();
                    if (fs.directoryExists(entry.getPath())) {
                        all.add(entry);
                    }
                }
                Platform.runLater(() -> {
                    commonPlatform.setAll(all);
                });
            } catch (Exception e) {
                // The file system can die
                ErrorEventFactory.fromThrowable(e).expected().omit().handle();
            }
        });
        var commonOverview = new BrowserFileOverviewComp(model, commonPlatform, false);
        var commonPane = new SimpleTitledPaneComp(AppI18n.observable("common"), commonOverview, false)
                .apply(struc -> VBox.setVgrow(struc.get(), Priority.NEVER));
        commonPane.hide(Bindings.isEmpty(commonPlatform));
        list.add(commonPane);

        var rootPlatform = FXCollections.<FileEntry>synchronizedObservableList(FXCollections.observableArrayList());
        ThreadHelper.runFailableAsync(() -> {
            var roots = model.getFileSystem().listRoots().stream()
                    .map(s -> FileEntry.ofDirectory(model.getFileSystem(), s))
                    .toList();
            Platform.runLater(() -> {
                rootPlatform.setAll(roots);
            });
        });
        var rootsOverview = new BrowserFileOverviewComp(model, rootPlatform, false);
        var rootsPane = new SimpleTitledPaneComp(AppI18n.observable("roots"), rootsOverview, false);
        rootsPane.hide(Bindings.isEmpty(rootPlatform));
        list.add(rootsPane);

        var vbox = new VerticalComp(list).styleClass("overview");
        var r = vbox.createRegion();
        return r;
    }
}
