package io.xpipe.app.browser;

import atlantafx.base.theme.Styles;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.CompStructure;
import io.xpipe.app.fxcomps.impl.TextFieldComp;
import io.xpipe.app.fxcomps.util.SimpleChangeListener;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import org.kordamp.ikonli.javafx.FontIcon;

public class BrowserFilterComp extends Comp<BrowserFilterComp.Structure> {

    @Override
    public Structure createBase() {
        var expanded = new SimpleBooleanProperty();
        var text = new TextFieldComp(filterString, false).createRegion();
        var button = new Button();
        text.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue && filterString.getValue() == null) {
                if (button.isFocused()) {
                    return;
                }

                expanded.set(false);
            }
        });
        filterString.addListener((observable, oldValue, newValue) -> {
            if (newValue == null && !text.isFocused()) {
                expanded.set(false);
            }
        });
        text.setMinWidth(0);
        Styles.toggleStyleClass(text, Styles.LEFT_PILL);

        SimpleChangeListener.apply(filterString, val -> {
            if (val == null) {
                text.getStyleClass().remove(Styles.SUCCESS);
            } else {
                text.getStyleClass().add(Styles.SUCCESS);
            }
        });

        var fi = new FontIcon("mdi2m-magnify");
        button.setGraphic(fi);
        button.setOnAction(event -> {
            if (expanded.get()) {
                if (filterString.getValue() == null) {
                    expanded.set(false);
                }
                event.consume();
            } else {
                expanded.set(true);
                text.requestFocus();
                event.consume();
            }
        });

        text.setPrefWidth(0);
        button.getStyleClass().add(Styles.FLAT);
        expanded.addListener((observable, oldValue, val) -> {
            if (val) {
                text.setPrefWidth(250);
                button.getStyleClass().add(Styles.RIGHT_PILL);
                button.getStyleClass().remove(Styles.FLAT);
            } else {
                text.setPrefWidth(0);
                button.getStyleClass().remove(Styles.RIGHT_PILL);
                button.getStyleClass().add(Styles.FLAT);
            }
        });
        button.prefHeightProperty().bind(text.heightProperty());

        var box = new HBox(text, button);
        box.setAlignment(Pos.CENTER);
        return new Structure(box, (TextField) text, button);
    }

    public record Structure(HBox box, TextField textField, Button toggleButton) implements CompStructure<HBox> {

        @Override
        public HBox get() {
            return box;
        }
    }

    private final Property<String> filterString;

    public BrowserFilterComp(Property<String> filterString) {
        this.filterString = filterString;
    }
}
