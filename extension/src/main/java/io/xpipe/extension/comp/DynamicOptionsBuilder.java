package io.xpipe.extension.comp;

import io.xpipe.charsetter.NewLine;
import io.xpipe.core.source.DataSource;
import io.xpipe.extension.I18n;
import io.xpipe.fxcomps.Comp;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TextField;
import javafx.scene.layout.Region;
import org.apache.commons.collections4.bidimap.DualLinkedHashBidiMap;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class DynamicOptionsBuilder<T extends DataSource<?>> {

    private final  List<DynamicOptionsComp.Entry> entries = new ArrayList<>();
    private final List<Property<?>> props = new ArrayList<>();

    public DynamicOptionsBuilder<T> addText(ObservableValue<String> name, Property<String> prop) {
        var comp = new TextField();
        comp.textProperty().bindBidirectional(prop);
        entries.add(new DynamicOptionsComp.Entry(name, Comp.of(() -> comp)));
        props.add(prop);
        return this;
    }

    public DynamicOptionsBuilder<T> addNewLine(Property<NewLine> prop) {
        var map = new LinkedHashMap<NewLine, ObservableValue<String>>();
        for (var e : NewLine.values()) {
            map.put(e, new SimpleStringProperty(e.getId()));
        }
        var comp = new ChoiceComp<>(prop, new DualLinkedHashBidiMap<>(map));
        entries.add(new DynamicOptionsComp.Entry(I18n.observable("newLine"), comp));
        props.add(prop);
        return this;
    }

    public DynamicOptionsBuilder<T> addCharacter(Property<Character> prop, ObservableValue<String> name, Map<Character, ObservableValue<String>> names) {
        var comp = new CharChoiceComp(prop, new DualLinkedHashBidiMap<>(names), null);
        entries.add(new DynamicOptionsComp.Entry(name, comp));
        props.add(prop);
        return this;
    }

    public DynamicOptionsBuilder<T> addCharacter(Property<Character> prop, ObservableValue<String> name, Map<Character, ObservableValue<String>> names, ObservableValue<String> customName) {
        var comp = new CharChoiceComp(prop, new DualLinkedHashBidiMap<>(names), customName);
        entries.add(new DynamicOptionsComp.Entry(name, comp));
        props.add(prop);
        return this;
    }

    public <V> DynamicOptionsBuilder<T> addToggle(Property<V> prop, ObservableValue<String> name, Map<V, ObservableValue<String>> names) {
        var comp = new ToggleGroupComp<>(prop, new DualLinkedHashBidiMap<>(names));
        entries.add(new DynamicOptionsComp.Entry(name, comp));
        props.add(prop);
        return this;
    }

    public DynamicOptionsBuilder<T> addCharset(Property<Charset> prop) {
        var comp = new CharsetChoiceComp(prop);
        entries.add(new DynamicOptionsComp.Entry(I18n.observable("charset"), comp));
        props.add(prop);
        return this;
    }

    public Region build(Function<T, T> creator, Property<T> toBind) {
        var bind = Bindings.createObjectBinding(() -> creator.apply(toBind.getValue()), props.toArray(Observable[]::new));
        bind.addListener((c,o, n) -> {
            toBind.setValue(n);
        });
        return new DynamicOptionsComp(entries).createRegion();
    }
}
