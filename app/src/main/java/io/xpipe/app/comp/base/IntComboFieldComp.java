package io.xpipe.app.comp.base;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.CompStructure;
import io.xpipe.app.comp.SimpleCompStructure;
import io.xpipe.app.platform.MenuHelper;
import io.xpipe.app.platform.PlatformThread;

import javafx.beans.property.Property;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.scene.control.skin.ComboBoxListViewSkin;
import javafx.scene.input.KeyEvent;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import java.util.List;

@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class IntComboFieldComp extends Comp<CompStructure<ComboBox<String>>> {

    Property<Integer> value;
    List<Integer> predefined;
    boolean allowNegative;

    public IntComboFieldComp(Property<Integer> value, List<Integer> predefined, boolean allowNegative) {
        this.value = value;
        this.predefined = predefined;
        this.allowNegative = allowNegative;
    }

    @Override
    public CompStructure<ComboBox<String>> createBase() {
        var text = MenuHelper.<String>createComboBox();
        text.setEditable(true);
        text.setValue(value.getValue() != null ? value.getValue().toString() : null);
        text.setItems(FXCollections.observableList(
                predefined.stream().map(integer -> "" + integer).toList()));
        text.setMaxWidth(20000);
        text.getStyleClass().add("int-combo-field-comp");
        text.setVisibleRowCount(Math.min(10, predefined.size()));

        value.addListener((ChangeListener<Number>) (observableValue, oldValue, newValue) -> {
            PlatformThread.runLaterIfNeeded(() -> {
                if (newValue == null) {
                    text.setValue("");
                } else {
                    text.setValue(newValue.toString());
                }
            });
        });

        text.addEventFilter(KeyEvent.KEY_TYPED, keyEvent -> {
            if (allowNegative) {
                if (!"-0123456789".contains(keyEvent.getCharacter())) {
                    keyEvent.consume();
                }
            } else {
                if (!"0123456789".contains(keyEvent.getCharacter())) {
                    keyEvent.consume();
                }
            }
        });

        text.valueProperty().addListener((observableValue, oldValue, newValue) -> {
            if (newValue == null
                    || newValue.isEmpty()
                    || (allowNegative && "-".equals(newValue))
                    || !newValue.matches("-?\\d+")) {
                value.setValue(null);
                return;
            }

            int intValue = Integer.parseInt(newValue);
            value.setValue(intValue);
        });

        return new SimpleCompStructure<>(text);
    }
}
