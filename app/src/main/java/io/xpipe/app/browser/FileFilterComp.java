package io.xpipe.app.browser;

import atlantafx.base.theme.Styles;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.SimpleCompStructure;
import io.xpipe.app.fxcomps.augment.GrowAugment;
import io.xpipe.app.fxcomps.impl.TextFieldComp;
import io.xpipe.app.fxcomps.util.Shortcuts;
import io.xpipe.app.fxcomps.util.SimpleChangeListener;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import org.kordamp.ikonli.javafx.FontIcon;

public class FileFilterComp extends SimpleComp {

    private final Property<String> filterString;

    public FileFilterComp(Property<String> filterString) {
        this.filterString = filterString;
    }

    @Override
    protected Region createSimple() {
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
        GrowAugment.create(false, true).augment(new SimpleCompStructure<>(button));
        Shortcuts.addShortcut(button, new KeyCodeCombination(KeyCode.F, KeyCombination.SHORTCUT_DOWN));
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
            System.out.println(val);
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

        var box = new HBox(text, button);
        box.setFillHeight(true);
        return box;
    }
}
