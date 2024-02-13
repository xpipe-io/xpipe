package io.xpipe.app.fxcomps.impl;

import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.CompStructure;
import io.xpipe.app.fxcomps.SimpleCompStructure;
import io.xpipe.app.fxcomps.util.PlatformThread;
import io.xpipe.app.util.ClipboardHelper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import org.kordamp.ikonli.javafx.FontIcon;

public class CodeSnippetComp extends Comp<CompStructure<?>> {

    private final ObservableValue<Boolean> showLineNumbers;
    private final ObservableValue<CodeSnippet> value;

    public CodeSnippetComp(boolean showLineNumbers, ObservableValue<CodeSnippet> value) {
        this.showLineNumbers = new SimpleBooleanProperty(showLineNumbers);
        this.value = PlatformThread.sync(value);
    }

    public CodeSnippetComp(ObservableValue<Boolean> showLineNumbers, ObservableValue<CodeSnippet> value) {
        this.showLineNumbers = PlatformThread.sync(showLineNumbers);
        this.value = PlatformThread.sync(value);
    }

    private static String toRGBCode(Color color) {
        return String.format(
                "#%02X%02X%02X",
                (int) (color.getRed() * 255), (int) (color.getGreen() * 255), (int) (color.getBlue() * 255));
    }

    private void fillArea(VBox lineNumbers, TextArea s) {
        lineNumbers.getChildren().clear();
        s.clear();

        int number = 1;
        for (CodeSnippet.Line line : value.getValue().lines()) {
            var numberLabel = new Label(String.valueOf(number));
            numberLabel.getStyleClass().add("line-number");
            lineNumbers.getChildren().add(numberLabel);

            for (var el : line.elements()) {
                s.appendText(el.text());
            }

            boolean last = number == value.getValue().lines().size();
            if (!last) {
                s.appendText("\n");
            }

            number++;
        }
    }

    private Region createCopyButton(Region container) {
        var button = new Button();
        button.setGraphic(new FontIcon("mdoal-content_copy"));
        button.setOnAction(e -> {
            ClipboardHelper.copyText(value.getValue().getRawString());
        });
        button.getStyleClass().add("copy");
        button.getStyleClass().add("button-comp");
        button.visibleProperty().bind(container.hoverProperty());
        return button;
    }

    @Override
    public CompStructure<?> createBase() {
        var s = new javafx.scene.control.TextArea();
        s.setEditable(false);
        s.setBackground(null);
        s.getStyleClass().add("code-snippet");
        s.addEventFilter(ScrollEvent.ANY, e -> {
            s.getParent().fireEvent(e);
            e.consume();
        });
        s.prefHeightProperty().setValue(20 * this.value.getValue().lines().size());

        var lineNumbers = new VBox();
        lineNumbers.getStyleClass().add("line-numbers");
        fillArea(lineNumbers, s);
        value.addListener((c, o, n) -> {
            fillArea(lineNumbers, s);
        });

        var spacer = new Region();
        spacer.getStyleClass().add("spacer");

        var content = new HBox(s);
        spacer.prefHeightProperty().bind(content.heightProperty());
        if (showLineNumbers.getValue()) {
            content.getChildren().add(0, lineNumbers);
            content.getChildren().add(1, spacer);
        }
        showLineNumbers.addListener((c, o, n) -> {
            if (n) {
                content.getChildren().add(0, lineNumbers);
                content.getChildren().add(1, spacer);
            } else {
                content.getChildren().remove(lineNumbers);
                content.getChildren().remove(spacer);
            }
        });
        HBox.setHgrow(s, Priority.ALWAYS);

        var c = new StackPane(content);
        c.getStyleClass().add("code-snippet-container");
        c.prefHeightProperty().bind(content.prefHeightProperty());

        var copyButton = createCopyButton(c);
        var pane = new AnchorPane(copyButton);
        pane.setPickOnBounds(false);
        AnchorPane.setTopAnchor(copyButton, 10.0);
        AnchorPane.setRightAnchor(copyButton, 10.0);
        c.getChildren().add(pane);

        return new SimpleCompStructure<>(c);
    }
}
