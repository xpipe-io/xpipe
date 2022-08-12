package io.xpipe.extension.comp;

import io.xpipe.fxcomps.Comp;
import io.xpipe.fxcomps.CompStructure;
import io.xpipe.fxcomps.comp.ReplacementComp;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.ComboBox;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;

public class CharsetChoiceComp extends ReplacementComp<CompStructure<ComboBox<Charset>>> {

    private final Property<Charset> charset;

    public CharsetChoiceComp(Property<Charset> charset) {
        this.charset = charset;
    }

    @Override
    protected Comp<CompStructure<ComboBox<Charset>>> createComp() {
        var map = new LinkedHashMap<Charset, ObservableValue<String>>();
        for (var e : List.of(StandardCharsets.UTF_8, StandardCharsets.UTF_16,
                StandardCharsets.UTF_16BE, StandardCharsets.ISO_8859_1, Charset.forName("Windows-1251"), Charset.forName("Windows-1252"), StandardCharsets.US_ASCII)) {
            map.put(e, new SimpleStringProperty(e.displayName()));
        }
        return new ChoiceComp<>(charset, map);
    }
}
