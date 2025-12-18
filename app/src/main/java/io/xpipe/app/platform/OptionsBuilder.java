package io.xpipe.app.platform;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.base.*;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.GuiDialog;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.util.BooleanScope;
import io.xpipe.app.util.DocumentationLink;
import io.xpipe.app.util.LicenseProvider;
import io.xpipe.core.InPlaceSecretValue;

import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.Region;

import atlantafx.base.controls.Spacer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;

public class OptionsBuilder {

    public <V, T> ObjectProperty<T> map(Property<V> prop, Function<V, T> function) {
        var mapped = new SimpleObjectProperty<T>();
        prop.subscribe(v -> {
            if (mappingUpdate.get()) {
                return;
            }

            try (var ignored = new BooleanScope(mappingUpdate).start()) {
                mapped.setValue(function.apply(v));
            }
        });
        return mapped;
    }

    public <V, T, R> ObjectProperty<R> map(Property<V> prop, Function<V, T> function, Function<T, R> subFunction) {
        var mapped = new SimpleObjectProperty<R>();
        prop.subscribe(v -> {
            if (mappingUpdate.get()) {
                return;
            }

            T t = function.apply(v);
            R r = t != null ? subFunction.apply(t) : null;
            try (var ignored = new BooleanScope(mappingUpdate).start()) {
                mapped.setValue(r);
            }
        });
        return mapped;
    }

    private final Validator ownValidator;
    private final List<Validator> allValidators = new ArrayList<>();
    private final List<Check> allChecks = new ArrayList<>();
    private final List<OptionsComp.Entry> entries = new ArrayList<>();
    private final List<Property<?>> props = new ArrayList<>();

    private ObservableValue<String> name;
    private ObservableValue<String> description;
    private String documentationLink;
    private Comp<?> comp;
    private Comp<?> lastCompHeadReference;
    private ObservableValue<String> lastNameReference;
    private boolean focusFirstIncomplete = true;

    private final BooleanProperty mappingUpdate = new SimpleBooleanProperty();

    public OptionsBuilder disableFirstIncompleteFocus() {
        focusFirstIncomplete = false;
        return this;
    }

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

    public OptionsBuilder choice(IntegerProperty selectedIndex, Map<ObservableValue<String>, OptionsBuilder> options) {
        return choice(selectedIndex, options, null);
    }

    public OptionsBuilder choice(
            IntegerProperty selectedIndex,
            Map<ObservableValue<String>, OptionsBuilder> options,
            Function<ComboBox<ChoicePaneComp.Entry>, Region> transformer) {
        var list = options.entrySet().stream()
                .map(e -> new ChoicePaneComp.Entry(
                        e.getKey(), e.getValue() != null ? e.getValue().buildComp() : Comp.empty()))
                .toList();
        var validatorList = options.values().stream()
                .map(builder -> builder != null ? builder.buildEffectiveValidator() : new SimpleValidator())
                .toList();
        var selected =
                new SimpleObjectProperty<>(selectedIndex.getValue() != -1 ? list.get(selectedIndex.getValue()) : null);
        selected.addListener((observable, oldValue, newValue) -> {
            selectedIndex.setValue(newValue != null ? list.indexOf(newValue) : null);
            if (newValue != null) {
                validatorList.get(list.indexOf(newValue)).validate();
            }
        });
        selectedIndex.addListener((observable, oldValue, newValue) -> {
            selected.setValue(list.get(newValue.intValue()));
        });
        var pane = new ChoicePaneComp(list, selected);
        if (transformer != null) {
            pane.setTransformer(transformer);
        }

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

        var entry = new OptionsComp.Entry(description, documentationLink, name, comp);
        description = null;
        documentationLink = null;
        name = null;
        lastNameReference = null;
        comp = null;
        lastCompHeadReference = null;
        entries.add(entry);
    }

    public OptionsBuilder nameAndDescription(String key) {
        return name(key).description(key + "Description");
    }

    public OptionsBuilder nameAndDescription(ObservableValue<String> key) {
        return name(AppI18n.observable(key)).description(AppI18n.observable(BindingsHelper.map(key, k -> k + "Description")));
    }

    public OptionsBuilder subAdvanced(OptionsBuilder builder) {
        name("advanced");
        subExpandable("showAdvancedOptions", builder);
        return this;
    }

    public OptionsBuilder subExpandable(String key, OptionsBuilder builder) {
        sub(builder, null);
        var subComp = this.comp;
        var pane = new SimpleTitledPaneComp(AppI18n.observable(key), subComp, true);
        pane.apply(struc -> struc.get().setExpanded(false));
        this.comp = pane;
        return this;
    }

    public OptionsBuilder sub(OptionsBuilder builder) {
        return sub(builder, null);
    }

    public OptionsBuilder sub(OptionsBuilder builder, Property<?> prop) {
        props.addAll(builder.props);
        allValidators.add(builder.buildEffectiveValidator());
        if (builder.focusFirstIncomplete) {
            allChecks.addAll(builder.allChecks);
        }
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
                null, null, null, new LabelComp(AppI18n.observable(titleKey)).styleClass("title-header")));
        return this;
    }

    public OptionsBuilder addTitle(ObservableValue<String> title) {
        finishCurrent();
        entries.add(new OptionsComp.Entry(
                null, null, null, new LabelComp(title).styleClass("title-header")));
        return this;
    }

    public OptionsBuilder pref(Object property) {
        var mapping = AppPrefs.get().getMapping(property);
        pref(
                mapping.getKey(),
                mapping.isRequiresRestart(),
                mapping.getLicenseFeatureId(),
                mapping.getDocumentationLink());
        return this;
    }

    public OptionsBuilder pref(
            String key, boolean requiresRestart, String licenseFeatureId, DocumentationLink documentationLink) {
        var name = key;
        name(name);
        if (requiresRestart) {
            description(AppI18n.observable(name + "Description").map(s -> s + "\n\n" + AppI18n.get("requiresRestart")));
        } else {
            description(AppI18n.observable(name + "Description"));
        }
        if (documentationLink != null) {
            documentationLink(documentationLink);
        }
        if (licenseFeatureId != null) {
            licenseRequirement(licenseFeatureId);
        }
        return this;
    }

    public OptionsBuilder licenseRequirement(String featureId) {
        var f = LicenseProvider.get().getFeature(featureId);
        name = f.suffixObservable(name);
        lastNameReference = name;
        return this;
    }

    public OptionsBuilder check(Function<Validator, Check> c) {
        var check = c.apply(ownValidator);
        lastCompHeadReference.apply(s -> {
            check.decorates(s.get());
        });
        allChecks.add(check);
        return this;
    }

    public OptionsBuilder check(Check c) {
        lastCompHeadReference.apply(s -> c.decorates(s.get()));
        allChecks.add(c);
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

    public OptionsBuilder hide(boolean b) {
        return hide(new SimpleBooleanProperty(b));
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

    public OptionsBuilder nonNullIf(ObservableValue<Boolean> b) {
        var e = lastNameReference;
        var p = props.getLast();
        return check(Validator.nonNullIf(ownValidator, e, p, b));
    }

    private void pushComp(Comp<?> comp) {
        finishCurrent();
        this.comp = comp;
        this.lastCompHeadReference = comp;
    }

    public OptionsBuilder addInteger(Property<Integer> prop) {
        var comp = new IntFieldComp(prop);
        pushComp(comp);
        props.add(prop);
        return this;
    }

    public OptionsBuilder addToggle(Property<Boolean> prop) {
        var comp = new ToggleSwitchComp(prop, null, null);
        pushComp(comp);
        props.add(prop);
        return this;
    }

    public OptionsBuilder addYesNoToggle(Property<Boolean> prop) {
        var map = new LinkedHashMap<Boolean, ObservableValue<String>>();
        map.put(Boolean.FALSE, AppI18n.observable("no"));
        map.put(null, AppI18n.observable("inherit"));
        map.put(Boolean.TRUE, AppI18n.observable("yes"));
        var comp = new ToggleGroupComp<>(prop, new SimpleObjectProperty<>(map));
        pushComp(comp);
        props.add(prop);
        return this;
    }

    public OptionsBuilder addStaticString(Object o) {
        return addStaticString(new SimpleStringProperty(o != null ? o.toString() : null));
    }

    public OptionsBuilder addStaticString(ObservableValue<String> s) {
        var prop = new SimpleStringProperty();
        s.subscribe(prop::set);
        var comp = new TextFieldComp(prop, false);
        BindingsHelper.preserve(comp, s);
        comp.apply(struc -> {
            struc.get().setEditable(false);
            struc.get().setOpacity(0.9);
            struc.get().setFocusTraversable(false);
        });
        pushComp(comp);
        return this;
    }

    public OptionsBuilder addString(Property<String> prop) {
        var comp = new TextFieldComp(prop, false);
        pushComp(comp);
        props.add(prop);
        return this;
    }

    public OptionsBuilder spacer(double size) {
        return addComp(Comp.of(() -> new Spacer(size, Orientation.VERTICAL)));
    }

    public OptionsBuilder name(String nameKey) {
        finishCurrent();
        name = AppI18n.observable(nameKey);
        lastNameReference = name;
        return this;
    }

    public OptionsBuilder name(ObservableValue<String> name) {
        finishCurrent();
        this.name = name;
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

    public OptionsBuilder documentationLink(String link) {
        finishCurrent();
        documentationLink = link;
        return this;
    }

    public OptionsBuilder documentationLink(DocumentationLink link) {
        finishCurrent();
        documentationLink = link != null ? link.getLink() : null;
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

    public <T> OptionsBuilder addProperty(ObservableList<T> prop) {
        // For updating the options builder binding on list change, it doesn't support observable lists
        var listHashProp = new SimpleIntegerProperty(0);
        prop.addListener((ListChangeListener<T>) c -> {
            listHashProp.set(c.getList().hashCode());
        });
        addProperty(listHashProp);
        return this;
    }

    public OptionsBuilder addSecret(Property<InPlaceSecretValue> prop, boolean copy) {
        var comp = new SecretFieldComp(prop, copy);
        pushComp(comp);
        props.add(prop);
        return this;
    }

    @SafeVarargs
    public final <T, V extends T> OptionsBuilder bind(Supplier<V> creator, Property<T>... toSet) {
        props.forEach(prop -> {
            prop.addListener((c, o, n) -> {
                if (mappingUpdate.get()) {
                    return;
                }

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
        var current = new AtomicReference<Property<? extends V>>(creator.get());
        var listener = new ChangeListener<V>() {
            @Override
            public void changed(ObservableValue<? extends V> observable, V oldValue, V newValue) {
                if (mappingUpdate.get()) {
                    return;
                }

                toSet.setValue(newValue);
            }
        };
        current.get().addListener(listener);
        props.forEach(prop -> {
            prop.addListener((c, o, n) -> {
                if (mappingUpdate.get()) {
                    return;
                }

                current.get().removeListener(listener);
                current.set(creator.get());
                toSet.setValue(current.get().getValue());
                current.get().addListener(listener);
            });
        });
        return this;
    }

    public OptionsComp buildComp() {
        finishCurrent();
        var comp = new OptionsComp(entries, focusFirstIncomplete ? allChecks : List.of());
        return comp;
    }

    public Region build() {
        return buildComp().createRegion();
    }

    public GuiDialog buildDialog() {
        return new GuiDialog(this);
    }
}
