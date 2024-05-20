package io.xpipe.app.comp.base;

import atlantafx.base.theme.Styles;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.CompStructure;
import io.xpipe.app.fxcomps.impl.IconButtonComp;
import io.xpipe.app.util.FileOpener;
import javafx.application.Platform;
import javafx.beans.property.Property;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import lombok.Builder;
import lombok.Value;

public class MarkdownEditorComp extends Comp<MarkdownEditorComp.Structure> {

    private final Property<String> value;
    private final String identifier;

    public MarkdownEditorComp(
            Property<String> value, String identifier) {
        this.value = value;
        this.identifier = identifier;
    }

    private Button createOpenButton() {
        return new IconButtonComp(
                "mdal-edit",
                () -> FileOpener.openString(
                        identifier + ".md",
                        this,
                        value.getValue(),
                        (s) -> {
                            Platform.runLater(() -> value.setValue(s));
                        }))
                .styleClass("edit-button")
                .apply(struc -> struc.get().getStyleClass().remove(Styles.FLAT))
                .createStructure()
                .get();
    }

    @Override
    public Structure createBase() {
        var markdown = new MarkdownComp(value, s -> s).createRegion();
        var editButton = createOpenButton();
        var pane = new AnchorPane(markdown, editButton);
        pane.setPickOnBounds(false);
        AnchorPane.setTopAnchor(editButton, 10.0);
        AnchorPane.setRightAnchor(editButton, 10.0);
        return new Structure(pane, markdown, editButton);
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
    public static class Structure implements CompStructure<AnchorPane> {
        AnchorPane pane;
        Region markdown;
        Button editButton;

        @Override
        public AnchorPane get() {
            return pane;
        }
    }
}
