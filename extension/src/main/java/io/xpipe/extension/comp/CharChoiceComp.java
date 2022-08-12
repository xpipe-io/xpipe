package io.xpipe.extension.comp;

import io.xpipe.fxcomps.Comp;
import io.xpipe.fxcomps.CompStructure;
import io.xpipe.fxcomps.SimpleCompStructure;
import javafx.beans.property.Property;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;

import java.util.HashMap;
import java.util.Map;

public class CharChoiceComp extends Comp<CompStructure<HBox>> {

    private final Property<Character> value;
    private final Map<Character, ObservableValue<String>> range;
    private final ObservableValue<String> customName;

    public CharChoiceComp(Property<Character> value, Map<Character, ObservableValue<String>> range, ObservableValue<String> customName) {
        this.value = value;
        this.range = range;
        this.customName = customName;
    }

    @Override
    public CompStructure<HBox> createBase() {
        var charChoice = new CharComp(value);
        var rangeCopy = new HashMap<>(range);
        if (customName != null) {
            rangeCopy.put(null, customName);
        }
        var choice = new ChoiceComp<Character>(value, rangeCopy);
        var charChoiceR = charChoice.createRegion();
        var choiceR = choice.createRegion();
        var box = new HBox(charChoiceR, choiceR);
        box.setAlignment(Pos.CENTER);
        choiceR.prefWidthProperty().bind(box.widthProperty().subtract(charChoiceR.widthProperty()));
        box.getStyleClass().add("char-choice-comp");
        return new SimpleCompStructure<>(box);
    }
}
