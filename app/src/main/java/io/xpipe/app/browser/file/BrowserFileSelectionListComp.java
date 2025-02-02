package io.xpipe.app.browser.file;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.SimpleComp;
import io.xpipe.app.comp.base.ListBoxViewComp;
import io.xpipe.app.comp.base.PrettyImageHelper;
import io.xpipe.app.core.AppStyle;
import io.xpipe.app.core.window.AppWindowHelper;
import io.xpipe.app.util.BindingsHelper;
import io.xpipe.app.util.PlatformThread;

import javafx.beans.binding.Bindings;
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
public class BrowserFileSelectionListComp extends SimpleComp {

    ObservableList<BrowserEntry> list;
    Function<BrowserEntry, ObservableValue<String>> nameTransformation;

    public BrowserFileSelectionListComp(ObservableList<BrowserEntry> list) {
        this(list, entry -> new SimpleStringProperty(entry.getFileName()));
    }

    public static Image snapshot(ObservableList<BrowserEntry> list) {
        var r = new BrowserFileSelectionListComp(list).styleClass("drag").createRegion();
        var scene = new Scene(r);
        AppWindowHelper.setupStylesheets(scene);
        AppStyle.addStylesheets(scene);
        SnapshotParameters parameters = new SnapshotParameters();
        parameters.setFill(Color.TRANSPARENT);
        return r.snapshot(parameters, null);
    }

    @Override
    protected Region createSimple() {
        var c = new ListBoxViewComp<>(
                        list,
                        list,
                        entry -> {
                            return Comp.of(() -> {
                                var image = PrettyImageHelper.ofFixedSizeSquare(entry.getIcon(), 24)
                                        .createRegion();
                                var t = nameTransformation.apply(entry);
                                var l = new Label(t.getValue(), image);
                                l.setGraphicTextGap(6);
                                l.setTextOverrun(OverrunStyle.CENTER_ELLIPSIS);
                                t.addListener((observable, oldValue, newValue) -> {
                                    PlatformThread.runLaterIfNeeded(() -> {
                                        l.setText(newValue);
                                    });
                                });
                                BindingsHelper.preserve(l, t);
                                return l;
                            });
                        },
                        true)
                .styleClass("selected-file-list")
                .hide(Bindings.isEmpty(list));
        return c.createRegion();
    }
}
