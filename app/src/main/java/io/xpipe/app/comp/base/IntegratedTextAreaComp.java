package io.xpipe.app.comp.base;

import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.CompStructure;
import io.xpipe.app.fxcomps.impl.IconButtonComp;
import io.xpipe.app.fxcomps.impl.TextAreaComp;
import io.xpipe.app.util.FileOpener;

import javafx.application.Platform;
import javafx.beans.property.Property;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TextArea;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

import atlantafx.base.theme.Styles;
import lombok.Builder;
import lombok.Value;

import java.nio.file.Files;
import java.nio.file.Path;

public class IntegratedTextAreaComp extends Comp<IntegratedTextAreaComp.Structure> {

    private final Property<String> value;
    private final boolean lazy;
    private final String identifier;
    private final ObservableValue<String> fileType;

    public IntegratedTextAreaComp(
            Property<String> value, boolean lazy, String identifier, ObservableValue<String> fileType) {
        this.value = value;
        this.lazy = lazy;
        this.identifier = identifier;
        this.fileType = fileType;
    }

    private Region createOpenButton() {
        return new IconButtonComp(
                        "mdal-edit",
                        () -> FileOpener.openString(
                                identifier + (fileType.getValue() != null ? "." + fileType.getValue() : ""),
                                this,
                                value.getValue(),
                                (s) -> {
                                    Platform.runLater(() -> value.setValue(s));
                                }))
                .styleClass("edit-button")
                .apply(struc -> struc.get().getStyleClass().remove(Styles.FLAT))
                .createRegion();
    }

    @Override
    public Structure createBase() {
        var fileDrop = new FileDropOverlayComp<>(
                new Comp<TextAreaStructure>() {
                    @Override
                    public TextAreaStructure createBase() {
                        var textArea = new TextAreaComp(value, lazy).createStructure();
                        var copyButton = createOpenButton();
                        var pane = new AnchorPane(copyButton);
                        pane.setPickOnBounds(false);
                        AnchorPane.setTopAnchor(copyButton, 10.0);
                        AnchorPane.setRightAnchor(copyButton, 10.0);

                        var c = new StackPane();
                        c.getChildren().addAll(textArea.get(), pane);
                        return new TextAreaStructure(c, textArea.getTextArea());
                    }
                },
                paths -> {
                    var first = paths.getFirst();
                    if (Files.size(first) > 1_000_000) {
                        return;
                    }

                    value.setValue(Files.readString(first));
                });
        var struc = fileDrop.createStructure();
        return new Structure(struc.get(), struc.getCompStructure().getTextArea());
    }

    @Value
    @Builder
    public static class TextAreaStructure implements CompStructure<StackPane> {
        StackPane pane;
        TextArea textArea;

        @Override
        public StackPane get() {
            return pane;
        }
    }

    @Value
    @Builder
    public static class Structure implements CompStructure<StackPane> {
        StackPane pane;
        TextArea textArea;

        @Override
        public StackPane get() {
            return pane;
        }
    }
}
