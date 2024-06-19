package io.xpipe.app.browser;

import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.comp.base.ListBoxViewComp;
import io.xpipe.app.core.AppStyle;
import io.xpipe.app.core.window.AppWindowHelper;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.impl.PrettyImageHelper;
import io.xpipe.app.fxcomps.util.PlatformThread;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.image.Image;
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

    ObservableList<BrowserEntry> list;
    Function<BrowserEntry, ObservableValue<String>> nameTransformation;

    public BrowserSelectionListComp(ObservableList<BrowserEntry> list) {
        this(list, entry -> new SimpleStringProperty(entry.getFileName()));
    }

    public static Image snapshot(ObservableList<BrowserEntry> list) {
        var r = new BrowserSelectionListComp(list).styleClass("drag").createRegion();
        var scene = new Scene(r);
        AppWindowHelper.setupStylesheets(scene);
        AppStyle.addStylesheets(scene);
        SnapshotParameters parameters = new SnapshotParameters();
        parameters.setFill(Color.TRANSPARENT);
        return r.snapshot(parameters, null);
    }

    @Override
    protected Region createSimple() {
        var c = new ListBoxViewComp<>(list, list, entry -> {
                    return Comp.of(() -> {
                        var image = PrettyImageHelper.ofFixedSizeSquare(entry.getIcon(), 24)
                                .createRegion();
                        var l = new Label(null, image);
                        l.setTextOverrun(OverrunStyle.CENTER_ELLIPSIS);
                        l.textProperty().bind(PlatformThread.sync(nameTransformation.apply(entry)));
                        return l;
                    });
                })
                .styleClass("selected-file-list");
        return c.createRegion();
    }
}
