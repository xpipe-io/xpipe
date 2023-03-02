package io.xpipe.app.util;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.impl.*;
import io.xpipe.core.util.SecretValue;
import javafx.beans.property.Property;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import net.synedra.validatorfx.Check;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class OptionsBuilder {

    private final List<OptionsComp.Entry> entries = new ArrayList<>();
    private final List<Property<?>> props = new ArrayList<>();

    private ObservableValue<String> name;
    private ObservableValue<String> description;
    private ObservableValue<String> longDescription;
    private Comp<?> comp;

    private void finishCurrent() {
        if (comp == null) {
            return;
        }

        var entry = new OptionsComp.Entry(null, description, longDescription, name, comp);
        description = null;
        longDescription = null;
        name = null;
        comp = null;
        entries.add(entry);
    }

    public OptionsBuilder addTitle(String titleKey) {
        finishCurrent();
        entries.add(new OptionsComp.Entry(
                titleKey, null, null, null, new LabelComp(AppI18n.observable(titleKey)).styleClass("title-header")));
        return this;
    }

    public OptionsBuilder addTitle(ObservableValue<String> title) {
        finishCurrent();
        entries.add(new OptionsComp.Entry(
                null, null, null, null, Comp.of(() -> new Label(title.getValue())).styleClass("title-header")));
        return this;
    }

    public OptionsBuilder decorate(Check c) {
        comp.apply(s -> c.decorates(s.get()));
        return this;
    }

    public OptionsBuilder nonNull(Validator v) {
        var e = name;
        var p = props.get(props.size() - 1);
        return decorate(Validator.nonNull(v, e, p));
    }

    private void pushComp(Comp<?> comp) {
        finishCurrent();
        this.comp = comp;
    }

    public OptionsBuilder stringArea(Property<String> prop, boolean lazy) {
        var comp = new TextAreaComp(prop, lazy);
        pushComp(comp);
        props.add(prop);
        return this;
    }

    public OptionsBuilder addInteger(Property<Integer> prop) {
        var comp = new IntFieldComp(prop);
        pushComp(comp);
        props.add(prop);
        return this;
    }

    public OptionsBuilder addString(Property<String> prop) {
        return addString(prop, false);
    }
    public OptionsBuilder addString(Property<String> prop, boolean lazy) {
        var comp = new TextFieldComp(prop, lazy);
        pushComp(comp);
        props.add(prop);
        return this;
    }


    public OptionsBuilder name(String nameKey) {
        finishCurrent();
        name = AppI18n.observable(nameKey);
        return this;
    }

    public OptionsBuilder description(String descriptionKey) {
        finishCurrent();
        description = AppI18n.observable(descriptionKey);
        return this;
    }

    public OptionsBuilder longDescription(String descriptionKey) {
        finishCurrent();
        longDescription = AppI18n.observable(descriptionKey);
        return this;
    }

    public OptionsBuilder addComp(Comp<?> comp) {
        pushComp(comp);
        return this;
    }

    public OptionsBuilder addComp(Comp<?> comp, Property<?> prop) {
        pushComp(comp);
        props.add(prop);
        return this;
    }

    public OptionsBuilder addSecret(Property<SecretValue> prop) {
        var comp = new SecretFieldComp(prop);
        pushComp(comp);
        props.add(prop);
        return this;
    }

    @SafeVarargs
    public final <T, V extends T> OptionsBuilder bind(Supplier<V> creator, Property<T>... toSet) {
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

    public final <T, V extends T> OptionsBuilder bindChoice(
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

    public OptionsComp buildComp() {
        finishCurrent();
        return new OptionsComp(entries);
    }

    public Region build() {
        return buildComp().createRegion();
    }
}
