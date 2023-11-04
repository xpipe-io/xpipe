package io.xpipe.app.comp.base;

import io.xpipe.app.fxcomps.CompStructure;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import lombok.Builder;
import lombok.Value;

public class BigIconButton extends ButtonComp {

    public BigIconButton(ObservableValue<String> name, Node graphic, Runnable listener) {
        super(name, graphic, listener);
    }

    @Override
    public Structure createBase() {
        var vbox = new VBox();
        vbox.getStyleClass().add("vbox");
        vbox.setAlignment(Pos.CENTER);

        var icon = new StackPane(getGraphic());
        icon.setAlignment(Pos.CENTER);
        icon.getStyleClass().add("icon");
        vbox.getChildren().add(icon);

        var label = new Label();
        label.textProperty().bind(getName());
        label.getStyleClass().add("name");
        vbox.getChildren().add(label);

        var b = new Button(null);
        b.accessibleTextProperty().bind(getName());
        b.setGraphic(vbox);
        b.setOnAction(e -> getListener().run());
        b.getStyleClass().add("big-icon-button-comp");
        return Structure.builder().stack(vbox).graphic(getGraphic()).graphicPane(icon).text(label).button(b).build();
    }

    @Value
    @Builder
    public static class Structure implements CompStructure<Button> {
        Button button;
        VBox stack;
        Node graphic;
        StackPane graphicPane;
        Label text;

        @Override
        public Button get() {
            return button;
        }
    }
}
