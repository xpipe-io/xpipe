package io.xpipe.app.comp.base;

import io.xpipe.app.comp.RegionStructure;
import io.xpipe.app.comp.RegionStructureBuilder;
import io.xpipe.app.util.FileOpener;

import javafx.application.Platform;
import javafx.beans.property.Property;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

import atlantafx.base.theme.Styles;
import lombok.Builder;
import lombok.Value;

public class MarkdownEditorComp extends RegionStructureBuilder<AnchorPane, MarkdownEditorComp.Structure> {

    private final Property<String> value;
    private final String identifier;

    public MarkdownEditorComp(Property<String> value, String identifier) {
        this.value = value;
        this.identifier = identifier;
    }

    private Button createOpenButton() {
        return new IconButtonComp(
                        "mdal-edit",
                        () -> FileOpener.openString(identifier + ".md", this, value.getValue(), (s) -> {
                            Platform.runLater(() -> value.setValue(s));
                        }))
                .style("edit-button")
                .apply(struc -> struc.getStyleClass().remove(Styles.FLAT))
                .build();
    }

    @Override
    public Structure createBase() {
        var markdown = new MarkdownComp(value, s -> s, true).build();
        var editButton = createOpenButton();
        var pane = new AnchorPane(markdown, editButton);
        pane.setPickOnBounds(false);
        AnchorPane.setTopAnchor(editButton, 10.0);
        AnchorPane.setRightAnchor(editButton, 10.0);
        markdown.prefWidthProperty().bind(pane.prefWidthProperty());
        markdown.prefHeightProperty().bind(pane.prefHeightProperty());
        return new Structure(pane, markdown, editButton);
    }

    @Value
    @Builder
    public static class TextAreaStructure implements RegionStructure<StackPane> {
        StackPane pane;
        TextArea textArea;

        @Override
        public StackPane get() {
            return pane;
        }
    }

    @Value
    @Builder
    public static class Structure implements RegionStructure<AnchorPane> {
        AnchorPane pane;
        Region markdown;
        Button editButton;

        @Override
        public AnchorPane get() {
            return pane;
        }
    }
}
