package io.xpipe.extension.util;

import io.xpipe.core.charsetter.NewLine;
import io.xpipe.core.charsetter.StreamCharset;
import io.xpipe.core.util.SecretValue;
import io.xpipe.extension.I18n;
import io.xpipe.extension.fxcomps.Comp;
import io.xpipe.extension.fxcomps.CompStructure;
import io.xpipe.extension.fxcomps.impl.*;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import net.synedra.validatorfx.Check;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class DynamicOptionsBuilder {

    private final List<DynamicOptionsComp.Entry> entries = new ArrayList<>();
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

    public DynamicOptionsBuilder addTitle(String titleKey) {
        return addTitle(I18n.observable(titleKey));
    }

    public DynamicOptionsBuilder addTitle(ObservableValue<String> title) {
        entries.add(new DynamicOptionsComp.Entry(
                null, Comp.of(() -> new Label(title.getValue())).styleClass("title-header")));
        return this;
    }

    public DynamicOptionsBuilder decorate(Check c) {
        entries.get(entries.size() - 1).comp().apply(s -> c.decorates(s.get()));
        return this;
    }

    public DynamicOptionsBuilder nonNull(Validator v) {
        var e = entries.get(entries.size() - 1);
        var p = props.get(props.size() - 1);
        return decorate(Validator.nonNull(v, e.name(), p));
    }

    public DynamicOptionsBuilder addNewLine(Property<NewLine> prop) {
        var map = new LinkedHashMap<NewLine, ObservableValue<String>>();
        for (var e : NewLine.values()) {
            map.put(e, I18n.observable("extension." + e.getId()));
        }
        var comp = new ChoiceComp<>(prop, map, false);
        entries.add(new DynamicOptionsComp.Entry(I18n.observable("extension.newLine"), comp));
        props.add(prop);
        return this;
    }

    public DynamicOptionsBuilder addCharacter(
            Property<Character> prop, ObservableValue<String> name, Map<Character, ObservableValue<String>> names) {
        var comp = new CharChoiceComp(prop, names, null);
        entries.add(new DynamicOptionsComp.Entry(name, comp));
        props.add(prop);
        return this;
    }

    public DynamicOptionsBuilder addCharacter(
            Property<Character> prop,
            ObservableValue<String> name,
            Map<Character, ObservableValue<String>> names,
            ObservableValue<String> customName) {
        var comp = new CharChoiceComp(prop, names, customName);
        entries.add(new DynamicOptionsComp.Entry(name, comp));
        props.add(prop);
        return this;
    }

    public DynamicOptionsBuilder addToggle(String nameKey,
            Property<Boolean> prop) {
        var comp = new ToggleGroupComp<>(prop, new SimpleObjectProperty<>(Map.of(Boolean.TRUE, I18n.observable("extension.yes"), Boolean.FALSE, I18n.observable("extension.no"))));
        entries.add(new DynamicOptionsComp.Entry(I18n.observable(nameKey), comp));
        props.add(prop);
        return this;
    }

    public <V> DynamicOptionsBuilder addToggle(
            Property<V> prop, ObservableValue<String> name, Map<V, ObservableValue<String>> names) {
        var comp = new ToggleGroupComp<>(prop, new SimpleObjectProperty<>(names));
        entries.add(new DynamicOptionsComp.Entry(name, comp));
        props.add(prop);
        return this;
    }

    public <V> DynamicOptionsBuilder addChoice(
            Property<V> prop, ObservableValue<String> name, Map<V, ObservableValue<String>> names, boolean includeNone
    ) {
        var comp = new ChoiceComp<>(prop, names, includeNone);
        entries.add(new DynamicOptionsComp.Entry(name, comp));
        props.add(prop);
        return this;
    }

    public <V> DynamicOptionsBuilder addChoice(
            Property<V> prop, ObservableValue<String> name, ObservableValue<Map<V, ObservableValue<String>>> names, boolean includeNone
    ) {
        var comp = new ChoiceComp<>(prop, names, includeNone);
        entries.add(new DynamicOptionsComp.Entry(name, comp));
        props.add(prop);
        return this;
    }

    public DynamicOptionsBuilder addCharset(Property<StreamCharset> prop) {
        var comp = new CharsetChoiceComp(prop);
        entries.add(new DynamicOptionsComp.Entry(I18n.observable("extension.charset"), comp));
        props.add(prop);
        return this;
    }

    public DynamicOptionsBuilder addStringArea(String nameKey, Property<String> prop, boolean lazy) {
        var comp = new TextAreaComp(prop, lazy);
        entries.add(new DynamicOptionsComp.Entry(I18n.observable(nameKey), comp));
        props.add(prop);
        return this;
    }

    public DynamicOptionsBuilder addString(String nameKey, Property<String> prop) {
        var comp = new TextFieldComp(prop);
        entries.add(new DynamicOptionsComp.Entry(I18n.observable(nameKey), comp));
        props.add(prop);
        return this;
    }

    public DynamicOptionsBuilder addString(String nameKey, Property<String> prop, boolean lazy) {
        var comp = new TextFieldComp(prop, lazy);
        entries.add(new DynamicOptionsComp.Entry(I18n.observable(nameKey), comp));
        props.add(prop);
        return this;
    }

    public DynamicOptionsBuilder addString(ObservableValue<String> name, Property<String> prop) {
        var comp = new TextFieldComp(prop);
        entries.add(new DynamicOptionsComp.Entry(name, comp));
        props.add(prop);
        return this;
    }

    public DynamicOptionsBuilder addString(ObservableValue<String> name, Property<String> prop, boolean lazy) {
        var comp = new TextFieldComp(prop, lazy);
        entries.add(new DynamicOptionsComp.Entry(name, comp));
        props.add(prop);
        return this;
    }

    public DynamicOptionsBuilder addComp(Comp<?> comp) {
        return addComp((ObservableValue<String>) null, comp, null);
    }

    public DynamicOptionsBuilder addComp(Comp<?> comp, Property<?> prop) {
        return addComp((ObservableValue<String>) null, comp, prop);
    }

    public DynamicOptionsBuilder addComp(String nameKey, Comp<?> comp, Property<?> prop) {
        return addComp(I18n.observable(nameKey), comp, prop);
    }

    public DynamicOptionsBuilder addComp(ObservableValue<String> name, Comp<?> comp, Property<?> prop) {
        entries.add(new DynamicOptionsComp.Entry(name, comp));
        if (prop != null) props.add(prop);
        return this;
    }

    public DynamicOptionsBuilder addSecret(String nameKey, Property<SecretValue> prop) {
        return addSecret(I18n.observable(nameKey), prop);
    }

    public DynamicOptionsBuilder addSecret(ObservableValue<String> name, Property<SecretValue> prop) {
        var comp = new SecretFieldComp(prop);
        entries.add(new DynamicOptionsComp.Entry(name, comp));
        props.add(prop);
        return this;
    }

    public DynamicOptionsBuilder addInteger(ObservableValue<String> name, Property<Integer> prop) {
        var comp = new IntFieldComp(prop);
        entries.add(new DynamicOptionsComp.Entry(name, comp));
        props.add(prop);
        return this;
    }

    public DynamicOptionsBuilder addInteger(String nameKey, Property<Integer> prop) {
        var comp = new IntFieldComp(prop);
        entries.add(new DynamicOptionsComp.Entry(I18n.observable(nameKey), comp));
        props.add(prop);
        return this;
    }

    @SafeVarargs
    public final <T, V extends T> DynamicOptionsBuilder bind(Supplier<V> creator, Property<T>... toSet) {
        props.forEach(prop -> {
            prop.addListener((c, o, n) -> {
                for (Property<T> p : toSet) {
                    p.setValue(creator.get());
                }
            });
        });
        for (Property<T> p : toSet) {
            p.setValue(creator.get());
        }
        return this;
    }

    public final <T, V extends T> DynamicOptionsBuilder bindChoice(
            Supplier<Property<? extends V>> creator, Property<T> toSet) {
        props.forEach(prop -> {
            prop.addListener((c, o, n) -> {
                toSet.unbind();
                toSet.bind(creator.get());
            });
        });
        toSet.bind(creator.get());
        return this;
    }

    public Comp<? extends CompStructure<?>> buildComp() {
        if (title != null) {
            entries.add(
                    0,
                    new DynamicOptionsComp.Entry(
                            null, Comp.of(() -> new Label(title.getValue())).styleClass("title-header")));
        }
        return new DynamicOptionsComp(entries, wrap);
    }

    public Region build() {
        return buildComp().createRegion();
    }
}
