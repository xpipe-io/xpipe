package io.xpipe.extension.comp;

import io.xpipe.fxcomps.Comp;
import io.xpipe.fxcomps.CompStructure;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualLinkedHashBidiMap;

public class CharChoiceComp extends Comp<CompStructure<HBox>> {

    private final Property<Character> value;
    private final Property<Character> charChoiceValue;
    private final BidiMap<Character, ObservableValue<String>> range;
    private final ObservableValue<String> customName;

    public CharChoiceComp(Property<Character> value, BidiMap<Character, ObservableValue<String>> range, ObservableValue<String> customName) {
        this.value = value;
        this.range = range;
        this.customName = customName;
        this.charChoiceValue = new SimpleObjectProperty<>(range.containsKey(value.getValue()) ? value.getValue() : null);
        value.addListener((c, o, n) -> {
            if (!range.containsKey(n)) {
                charChoiceValue.setValue(null);
            } else {
                charChoiceValue.setValue(n);
            }
        });
    }

    @Override
    public CompStructure<HBox> createBase() {
        var charChoice = new CharComp(value);
        var rangeCopy = new DualLinkedHashBidiMap<>(range);
        if (customName != null) {
            rangeCopy.put(null, customName);
        }
        var choice = new ChoiceComp<Character>(charChoiceValue, rangeCopy);
        var charChoiceR = charChoice.createRegion();
        var choiceR = choice.createRegion();
        var box = new HBox(charChoiceR, choiceR);
        box.setAlignment(Pos.CENTER);
        choiceR.prefWidthProperty().bind(box.widthProperty().subtract(charChoiceR.widthProperty()));
        box.getStyleClass().add("char-choice-comp");
        return new CompStructure<>(box);
    }
}
