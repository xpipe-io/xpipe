package io.xpipe.extension.comp;

import io.xpipe.fxcomps.Comp;
import io.xpipe.fxcomps.CompStructure;
import io.xpipe.fxcomps.comp.ReplacementComp;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.ComboBox;
import org.apache.commons.collections4.bidimap.DualLinkedHashBidiMap;

import java.nio.charset.Charset;
import java.util.LinkedHashMap;

public class CharsetChoiceComp extends ReplacementComp<CompStructure<ComboBox<Charset>>> {

    private final Property<Charset> charset;

    public CharsetChoiceComp(Property<Charset> charset) {
        this.charset = charset;
    }

    @Override
    protected Comp<CompStructure<ComboBox<Charset>>> createComp() {
        var map = new LinkedHashMap<Charset, ObservableValue<String>>();
        for (var e : Charset.availableCharsets().entrySet()) {
            map.put(e.getValue(), new SimpleStringProperty(e.getKey()));
        }
        return new ChoiceComp<>(charset, new DualLinkedHashBidiMap<>(map));
    }
}
