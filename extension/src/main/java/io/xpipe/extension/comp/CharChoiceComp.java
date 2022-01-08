package io.xpipe.extension.comp;

import io.xpipe.fxcomps.CompStructure;
import io.xpipe.fxcomps.store.DefaultValueStoreComp;
import io.xpipe.fxcomps.util.StrongBindings;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualLinkedHashBidiMap;

import java.util.function.Supplier;

public class CharChoiceComp extends DefaultValueStoreComp<CompStructure<HBox>, Character> {

    private final BidiMap<Character, Supplier<String>> range;
    private final Supplier<String> customName;

    public CharChoiceComp(Character defaultVal, BidiMap<Character, Supplier<String>> range, Supplier<String> customName) {
        super(defaultVal);
        this.range = range;
        this.customName = customName;
    }

    @Override
    public CompStructure<HBox> createBase() {
        var charChoice = new CharComp();
        StrongBindings.bind(charChoice.valueProperty(), valueProperty());

        var rangeCopy = new DualLinkedHashBidiMap<>(range);
        rangeCopy.put(null, customName);
        var choice = new ChoiceComp<Character>(value.getValue(), rangeCopy);
        choice.set(getValue());
        choice.valueProperty().addListener((c, o, n) -> {
            set(n);
        });
        valueProperty().addListener((c, o, n) -> {
            if (n != null && !range.containsKey(n)) {
                choice.set(null);
            } else {
                choice.set(n);
            }
        });

        var charChoiceR = charChoice.createRegion();
        var choiceR = choice.createRegion();
        var box = new HBox(charChoiceR, choiceR);
        box.setAlignment(Pos.CENTER);
        choiceR.prefWidthProperty().bind(box.widthProperty().subtract(charChoiceR.widthProperty()));
        box.getStyleClass().add("char-choice-comp");
        return new CompStructure<>(box);
    }
}
