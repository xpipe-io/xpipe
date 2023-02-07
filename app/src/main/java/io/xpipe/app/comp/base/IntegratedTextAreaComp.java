package io.xpipe.app.comp.base;

import io.xpipe.app.editor.EditorState;
import io.xpipe.extension.fxcomps.Comp;
import io.xpipe.extension.fxcomps.SimpleComp;
import io.xpipe.extension.fxcomps.impl.IconButtonComp;
import io.xpipe.extension.fxcomps.impl.TextAreaComp;
import javafx.application.Platform;
import javafx.beans.property.Property;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

import java.nio.file.Files;

public class IntegratedTextAreaComp extends SimpleComp {

    private final Property<String> value;
    private final boolean lazy;
    private final String identifier;
    private final String fileType;

    public IntegratedTextAreaComp(Property<String> value, boolean lazy, String identifier, String fileType) {
        this.value = value;
        this.lazy = lazy;
        this.identifier = identifier;
        this.fileType = fileType;
    }

    @Override
    protected Region createSimple() {
        var fileDrop = new FileDropOverlayComp<>(Comp.of(() -> {
            var textArea = new TextAreaComp(value, lazy).createRegion();
            var copyButton = createOpenButton(textArea);
            var pane = new AnchorPane(copyButton);
            pane.setPickOnBounds(false);
            AnchorPane.setTopAnchor(copyButton, 10.0);
            AnchorPane.setRightAnchor(copyButton, 10.0);

            var c = new StackPane();
            c.getChildren().addAll(textArea, pane);
            return c;
        }), paths -> value.setValue(Files.readString(paths.get(0))));
        return fileDrop.createRegion();
    }

    private Region createOpenButton(Region container) {
        var button = new IconButtonComp("mdal-edit", () -> EditorState.get()
                .startEditing(identifier, fileType, this, value.getValue(), (s) -> {
                    Platform.runLater(() -> value.setValue(s));
                })).createRegion();
        return button;
    }
}
