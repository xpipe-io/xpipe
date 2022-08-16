package io.xpipe.extension.comp;

import io.xpipe.core.charsetter.NewLine;
import io.xpipe.core.util.Secret;
import io.xpipe.extension.I18n;
import io.xpipe.extension.Validator;
import io.xpipe.extension.Validators;
import io.xpipe.fxcomps.Comp;
import javafx.beans.property.Property;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import net.synedra.validatorfx.Check;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class DynamicOptionsBuilder<T> {

    private final  List<DynamicOptionsComp.Entry> entries = new ArrayList<>();
    private final List<Property<?>> props = new ArrayList<>();
    private final List<Property<?>> lazyProperties = new ArrayList<>();

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

    public DynamicOptionsBuilder<T> makeLazy() {
        var p = props.get(props.size() - 1);
        props.remove(p);
        lazyProperties.add(p);
        return this;
    }

    public DynamicOptionsBuilder<T> decorate(Check c) {

        entries.get(entries.size() - 1).comp().apply(s -> c.decorates(s.get()));
        return this;
    }

    public DynamicOptionsBuilder<T> nonNull(Validator v) {
        var e = entries.get(entries.size() - 1);
        var p = props.get(props.size() - 1);
        return decorate(Validators.nonNull(v, e.name(), p));
    }

    public DynamicOptionsBuilder<T> addNewLine(Property<NewLine> prop) {
        var map = new LinkedHashMap<NewLine, ObservableValue<String>>();
        for (var e : NewLine.values()) {
            map.put(e, I18n.observable("extension." + e.getId()));
        }
        var comp = new ChoiceComp<>(prop,map);
        entries.add(new DynamicOptionsComp.Entry(I18n.observable("extension.newLine"), comp));
        props.add(prop);
        return this;
    }

    public DynamicOptionsBuilder<T> addCharacter(Property<Character> prop, ObservableValue<String> name, Map<Character, ObservableValue<String>> names) {
        var comp = new CharChoiceComp(prop, names, null);
        entries.add(new DynamicOptionsComp.Entry(name, comp));
        props.add(prop);
        return this;
    }

    public DynamicOptionsBuilder<T> addCharacter(Property<Character> prop, ObservableValue<String> name, Map<Character, ObservableValue<String>> names, ObservableValue<String> customName) {
        var comp = new CharChoiceComp(prop, names, customName);
        entries.add(new DynamicOptionsComp.Entry(name, comp));
        props.add(prop);
        return this;
    }

    public <V> DynamicOptionsBuilder<T> addToggle(Property<V> prop, ObservableValue<String> name, Map<V, ObservableValue<String>> names) {
        var comp = new ToggleGroupComp<>(prop, names);
        entries.add(new DynamicOptionsComp.Entry(name, comp));
        props.add(prop);
        return this;
    }

    public <V> DynamicOptionsBuilder<T> addChoice(Property<V> prop, ObservableValue<String> name, Map<V, ObservableValue<String>> names) {
        var comp = new ChoiceComp<>(prop, names);
        entries.add(new DynamicOptionsComp.Entry(name, comp));
        props.add(prop);
        return this;
    }

    public <V> DynamicOptionsBuilder<T> addChoice(Property<V> prop, ObservableValue<String> name, ObservableValue<Map<V, ObservableValue<String>>> names) {
        var comp = new ChoiceComp<>(prop, names);
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

    public DynamicOptionsBuilder<T> addStringArea(String nameKey, Property<String> prop) {
        var comp = new TextAreaComp(prop);
        entries.add(new DynamicOptionsComp.Entry(I18n.observable(nameKey), comp));
        props.add(prop);
        return this;
    }

    public DynamicOptionsBuilder<T> addString(String nameKey, Property<String> prop) {
        var comp = new TextFieldComp(prop);
        entries.add(new DynamicOptionsComp.Entry(I18n.observable(nameKey), comp));
        props.add(prop);
        return this;
    }

    public DynamicOptionsBuilder<T> addLazyString(String nameKey, Property<String> prop, Property<String> lazy) {
        var comp = new TextFieldComp(prop, lazy);
        entries.add(new DynamicOptionsComp.Entry(I18n.observable(nameKey), comp));
        props.add(prop);
        lazyProperties.add(lazy);
        return this;
    }

    public DynamicOptionsBuilder<T> addString(ObservableValue<String> name, Property<String> prop) {
        var comp = new TextFieldComp(prop);
        entries.add(new DynamicOptionsComp.Entry(name, comp));
        props.add(prop);
        return this;
    }

    public DynamicOptionsBuilder<T> addComp(ObservableValue<String> name, Comp<?> comp, Property<?> prop) {
        entries.add(new DynamicOptionsComp.Entry(name, comp));
        props.add(prop);
        return this;
    }

    public DynamicOptionsBuilder<T> addSecret(String nameKey, Property<Secret> prop) {
        return addSecret(I18n.observable(nameKey), prop);
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

    public DynamicOptionsBuilder<T> addInteger(String nameKey, Property<Integer> prop) {
        var comp = new IntFieldComp(prop);
        entries.add(new DynamicOptionsComp.Entry(I18n.observable(nameKey), comp));
        props.add(prop);
        return this;
    }

    @SafeVarargs
    public final <V extends T> DynamicOptionsBuilder<T> bind(Supplier<V> creator, Property<T>... toSet) {
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

    public <V extends T> DynamicOptionsBuilder<T> bindLazy(Supplier<V> creator, Property<T> toLazySet) {
        lazyProperties.forEach(prop -> {
            prop.addListener((c,o,n) -> {
                toLazySet.setValue(creator.get());
            });
        });
        toLazySet.setValue(creator.get());

        return this;
    }

    public final <V extends T> DynamicOptionsBuilder<T> bindChoice(Supplier<Property<? extends V>> creator,  Property<T> toSet) {
        props.forEach(prop -> {
            prop.addListener((c,o,n) -> {
                toSet.unbind();
                toSet.bind(creator.get());
            });
        });
        toSet.bind(creator.get());
        return this;
    }

    public Comp<?> buildComp() {
        if (title != null) {
            entries.add(0, new DynamicOptionsComp.Entry(null, Comp.of(() -> new Label(title.getValue())).styleClass("title")));
        }
        return new DynamicOptionsComp(entries, wrap);
    }


    public <V extends T> Region build() {
        return buildComp().createRegion();
    }
}
