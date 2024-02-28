package io.xpipe.app.util;

import atlantafx.base.controls.Spacer;
import io.xpipe.app.comp.base.ToggleSwitchComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.GuiDialog;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.impl.*;
import io.xpipe.core.util.InPlaceSecretValue;
import javafx.beans.property.*;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Orientation;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import net.synedra.validatorfx.Check;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class OptionsBuilder {

    private final Validator ownValidator;
    private final List<Validator> allValidators = new ArrayList<>();
    private final List<OptionsComp.Entry> entries = new ArrayList<>();
    private final List<Property<?>> props = new ArrayList<>();

    private ObservableValue<String> name;
    private ObservableValue<String> description;
    private String longDescription;
    private Comp<?> comp;
    private Comp<?> lastCompHeadReference;
    private ObservableValue<String> lastNameReference;

    public OptionsBuilder() {
        this.ownValidator = new SimpleValidator();
        this.allValidators.add(ownValidator);
    }

    public OptionsBuilder(Validator validator) {
        this.ownValidator = validator;
        this.allValidators.add(ownValidator);
    }

    public Validator buildEffectiveValidator() {
        return new ChainedValidator(allValidators);
    }

    public OptionsBuilder choice(IntegerProperty selectedIndex, Map<String, OptionsBuilder> options) {
        var list = options.entrySet().stream()
                .map(e -> new ChoicePaneComp.Entry(
                        AppI18n.observable(e.getKey()),
                        e.getValue() != null ? e.getValue().buildComp() : Comp.empty()))
                .toList();
        var validatorList = options.values().stream()
                .map(builder -> builder != null ? builder.buildEffectiveValidator() : new SimpleValidator())
                .toList();
        var selected =
                new SimpleObjectProperty<>(selectedIndex.getValue() != -1 ? list.get(selectedIndex.getValue()) : null);
        selected.addListener((observable, oldValue, newValue) -> {
            selectedIndex.setValue(newValue != null ? list.indexOf(newValue) : null);
        });
        var pane = new ChoicePaneComp(list, selected);

        var validatorMap = new LinkedHashMap<ChoicePaneComp.Entry, Validator>();
        for (int i = 0; i < list.size(); i++) {
            validatorMap.put(list.get(i), validatorList.get(i));
        }
        validatorMap.put(null, new SimpleValidator());
        var orVal = new ExclusiveValidator<>(validatorMap, selected);

        options.values().forEach(builder -> {
            if (builder != null) {
                props.addAll(builder.props);
            }
        });
        props.add(selectedIndex);
        allValidators.add(orVal);
        pushComp(pane);
        return this;
    }

    private void finishCurrent() {
        if (comp == null) {
            return;
        }

        var entry = new OptionsComp.Entry(null, description, longDescription, name, comp);
        description = null;
        longDescription = null;
        name = null;
        lastNameReference = null;
        comp = null;
        lastCompHeadReference = null;
        entries.add(entry);
    }

    public OptionsBuilder nameAndDescription(String key) {
        return name(key).description(key + "Description");
    }

    public OptionsBuilder sub(OptionsBuilder builder) {
        return sub(builder, null);
    }

    public OptionsBuilder sub(OptionsBuilder builder, Property<?> prop) {
        props.addAll(builder.props);
        allValidators.add(builder.buildEffectiveValidator());
        if (prop != null) {
            props.add(prop);
        }
        var c = builder.lastCompHeadReference;
        var n = builder.lastNameReference;
        pushComp(builder.buildComp());
        if (c != null) {
            lastCompHeadReference = c;
        }
        if (n != null) {
            lastNameReference = n;
        }
        return this;
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
                null,
                null,
                null,
                null,
                Comp.of(() -> new Label(title.getValue())).styleClass("title-header")));
        return this;
    }

    public OptionsBuilder check(Check c) {
        lastCompHeadReference.apply(s -> c.decorates(s.get()));
        return this;
    }

    public OptionsBuilder disable() {
        lastCompHeadReference.disable(new SimpleBooleanProperty(true));
        return this;
    }

    public OptionsBuilder disable(ObservableValue<Boolean> b) {
        lastCompHeadReference.disable(b);
        return this;
    }

    public OptionsBuilder hide(ObservableValue<Boolean> b) {
        lastCompHeadReference.hide(b);
        return this;
    }

    public OptionsBuilder disable(boolean b) {
        lastCompHeadReference.disable(new SimpleBooleanProperty(b));
        return this;
    }

    public OptionsBuilder nonNull() {
        var e = lastNameReference;
        var p = props.getLast();
        return check(Validator.nonNull(ownValidator, e, p));
    }

    public OptionsBuilder withValidator(Consumer<Validator> val) {
        val.accept(ownValidator);
        return this;
    }

    public OptionsBuilder nonEmpty() {
        var e = lastNameReference;
        var p = props.getLast();
        return check(Validator.nonEmpty(ownValidator, e, (ReadOnlyListProperty<?>) p));
    }

    public OptionsBuilder validate() {
        var e = lastNameReference;
        var p = props.getLast();
        return check(Validator.nonNull(ownValidator, e, p));
    }

    public OptionsBuilder nonNull(Validator v) {
        var e = lastNameReference;
        var p = props.getLast();
        return check(Validator.nonNull(v, e, p));
    }

    private void pushComp(Comp<?> comp) {
        finishCurrent();
        this.comp = comp;
        this.lastCompHeadReference = comp;
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

    public OptionsBuilder addToggle(Property<Boolean> prop) {
        var comp = new ToggleSwitchComp(prop, null);
        pushComp(comp);
        props.add(prop);
        return this;
    }

    public OptionsBuilder addYesNoToggle(Property<Boolean> prop) {
        var comp = new ToggleGroupComp<>(
                prop,
                new SimpleObjectProperty<>(Map.of(
                        Boolean.TRUE, AppI18n.observable("app.yes"), Boolean.FALSE, AppI18n.observable("app.no"))));
        pushComp(comp);
        props.add(prop);
        return this;
    }

    public OptionsBuilder addString(Property<String> prop) {
        return addString(prop, false);
    }

    public OptionsBuilder addPath(Property<Path> prop) {
        var string = new SimpleStringProperty(
                prop.getValue() != null ? prop.getValue().toString() : null);
        var comp = new TextFieldComp(string, true);
        string.addListener((observable, oldValue, newValue) -> {
            if (newValue == null) {
                prop.setValue(null);
                return;
            }

            try {
                var p = Path.of(newValue);
                prop.setValue(p);
            } catch (InvalidPathException ignored) {

            }
        });
        pushComp(comp);
        props.add(prop);
        return this;
    }

    public OptionsBuilder addString(Property<String> prop, boolean lazy) {
        var comp = new TextFieldComp(prop, lazy);
        pushComp(comp);
        props.add(prop);
        return this;
    }

    public OptionsBuilder spacer(double size) {
        return addComp(Comp.of(() -> new Spacer(size, Orientation.VERTICAL)));
    }

    public OptionsBuilder separator() {
        return addComp(Comp.separator());
    }

    public OptionsBuilder name(String nameKey) {
        finishCurrent();
        name = AppI18n.observable(nameKey);
        lastNameReference = name;
        return this;
    }

    public OptionsBuilder description(String descriptionKey) {
        finishCurrent();
        description = AppI18n.observable(descriptionKey);
        return this;
    }

    public OptionsBuilder description(ObservableValue<String> description) {
        finishCurrent();
        this.description = description;
        return this;
    }

    public OptionsBuilder longDescription(String descriptionKey) {
        finishCurrent();
        longDescription = AppI18n.getInstance().getMarkdownDocumentation(descriptionKey);
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

    public OptionsBuilder addProperty(Property<?> prop) {
        props.add(prop);
        return this;
    }

    public OptionsBuilder addSecret(Property<InPlaceSecretValue> prop) {
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

    public GuiDialog buildDialog() {
        return new GuiDialog(buildComp(), buildEffectiveValidator());
    }
}
