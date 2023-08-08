package io.xpipe.app.browser;

import io.xpipe.app.browser.icon.FileIconManager;
import io.xpipe.app.comp.base.ListBoxViewComp;
import io.xpipe.app.core.AppStyle;
import io.xpipe.app.core.AppWindowHelper;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.impl.SvgCacheComp;
import io.xpipe.app.fxcomps.util.PlatformThread;
import io.xpipe.core.impl.FileNames;
import io.xpipe.core.store.FileSystem;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.function.Function;

@Value
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
public class BrowserSelectionListComp extends SimpleComp {

    public static Image snapshot(ObservableList<FileSystem.FileEntry> list) {
        var r = new BrowserSelectionListComp(list).styleClass("drag").createRegion();
        var scene = new Scene(r);
        AppWindowHelper.setupStylesheets(scene);
        AppStyle.addStylesheets(scene);
        SnapshotParameters parameters = new SnapshotParameters();
        parameters.setFill(Color.TRANSPARENT);
        return r.snapshot(parameters, null);
    }

    ObservableList<FileSystem.FileEntry> list;
    Function<FileSystem.FileEntry, ObservableValue<String>> nameTransformation;

    public BrowserSelectionListComp(ObservableList<FileSystem.FileEntry> list) {
        this(list, entry -> new SimpleStringProperty(FileNames.getFileName(entry.getPath())));
    }

    @Override
    protected Region createSimple() {
        var c = new ListBoxViewComp<>(list, list, entry -> {
                    return Comp.of(() -> {
                        var wv = new SvgCacheComp(
                                        new SimpleDoubleProperty(20),
                                        new SimpleDoubleProperty(20),
                                        new SimpleStringProperty(FileIconManager.getFileIcon(entry, false)),
                                        FileIconManager.getSvgCache())
                                .createRegion();
                        var l = new Label(null, wv);
                        l.setTextOverrun(OverrunStyle.CENTER_ELLIPSIS);
                        l.textProperty().bind(PlatformThread.sync(nameTransformation.apply(entry)));
                        return l;
                    });
                })
                .styleClass("selected-file-list");
        return c.createRegion();
    }
}
