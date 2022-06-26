package io.xpipe.extension.comp;

import io.xpipe.core.charsetter.NewLine;
import io.xpipe.core.util.Secret;
import io.xpipe.extension.I18n;
import io.xpipe.fxcomps.Comp;
import javafx.beans.property.Property;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import org.apache.commons.collections4.bidimap.DualLinkedHashBidiMap;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class DynamicOptionsBuilder<T> {

    private final  List<DynamicOptionsComp.Entry> entries = new ArrayList<>();
    private final List<Property<?>> props = new ArrayList<>();

    private final ObservableValue<String> title;
    private final boolean wrap;

    public DynamicOptionsBuilder() {
        this.wrap = true;
        this.title = null;
    }

    public DynamicOptionsBuilder(boolean wrap) {
        this.wrap = wrap;
        this.title = null;
    }

    public DynamicOptionsBuilder(ObservableValue<String> title) {
        this.wrap = false;
        this.title = title;
    }

    public DynamicOptionsBuilder<T> addNewLine(Property<NewLine> prop) {
        var map = new LinkedHashMap<NewLine, ObservableValue<String>>();
        for (var e : NewLine.values()) {
            map.put(e, I18n.observable("extension." + e.getId()));
        }
        var comp = new ChoiceComp<>(prop, new DualLinkedHashBidiMap<>(map));
        entries.add(new DynamicOptionsComp.Entry(I18n.observable("extension.newLine"), comp));
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
        entries.add(new DynamicOptionsComp.Entry(I18n.observable("extension.charset"), comp));
        props.add(prop);
        return this;
    }

    public DynamicOptionsBuilder<T> addString(ObservableValue<String> name, Property<String> prop) {
        var comp = new TextFieldComp(prop);
        entries.add(new DynamicOptionsComp.Entry(name, comp));
        props.add(prop);
        return this;
    }

    public DynamicOptionsBuilder<T> addComp(Comp<?> comp) {
        entries.add(new DynamicOptionsComp.Entry(null, comp));
        return this;
    }

    public DynamicOptionsBuilder<T> addSecret(ObservableValue<String> name, Property<Secret> prop) {
        var comp = new SecretFieldComp(prop);
        entries.add(new DynamicOptionsComp.Entry(name, comp));
        props.add(prop);
        return this;
    }

    public DynamicOptionsBuilder<T> addInteger(ObservableValue<String> name, Property<Integer> prop) {
        var comp = new IntFieldComp(prop);
        entries.add(new DynamicOptionsComp.Entry(name, comp));
        props.add(prop);
        return this;
    }

    public <V extends T> Comp<?> buildComp(Function<T, V> creator, Property<T> toSet) {
        props.forEach(prop -> {
            prop.addListener((c,o,n) -> {
                toSet.setValue(creator.apply(toSet.getValue()));
            });
        });
        if (title != null) {
            entries.add(0, new DynamicOptionsComp.Entry(null, Comp.of(() -> new Label(title.getValue())).styleClass("title")));
        }
        return new DynamicOptionsComp(entries, wrap);
    }

    public <V extends T> Region build(Function<T, V> creator, Property<T> toSet) {
        return buildComp(creator, toSet).createRegion();
    }
}
