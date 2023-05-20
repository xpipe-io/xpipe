package io.xpipe.app.browser;

import io.xpipe.app.browser.icon.FileIconManager;
import io.xpipe.app.comp.base.ListBoxViewComp;
import io.xpipe.app.core.AppStyle;
import io.xpipe.app.core.AppWindowHelper;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.core.impl.FileNames;
import io.xpipe.core.store.FileSystem;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@EqualsAndHashCode(callSuper = true)
public class BrowserSelectionListComp extends SimpleComp {

    public static Image snapshot(ObservableList<FileSystem.FileEntry> list) {
        var r = new BrowserSelectionListComp(list).styleClass("drag").createRegion();
        var scene = new Scene(r);
        AppWindowHelper.setupStylesheets(scene);
        AppStyle.addStylesheets(scene);
        SnapshotParameters parameters = new SnapshotParameters();
        parameters.setFill(Color.TRANSPARENT);
        WritableImage image = r.snapshot(parameters, null);
        return image;
    }

    ObservableList<FileSystem.FileEntry> list;

    @Override
    protected Region createSimple() {
        var c = new ListBoxViewComp<>(list, list, entry -> {
                    return Comp.of(() -> {
                        var icon = new ImageView(FileIconManager.getSvgCache()
                                .getCached(FileIconManager.getFileIcon(entry, false))
                                .orElse(null));
                        icon.setFitWidth(20);
                        icon.setFitHeight(20);
                        var l = new Label(FileNames.getFileName(entry.getPath()), icon);
                        return l;
                    });
                })
                .styleClass("selected-file-list");
        return c.createRegion();
    }
}
