package io.xpipe.app.platform;

import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableValue;

import java.util.*;

public final class ExclusiveValidator<T> implements Validator {

    private final SequencedMap<T, ? extends Validator> validators;
    private final ObservableValue<T> obs;

    public ExclusiveValidator(SequencedMap<T, ? extends Validator> validators, ObservableValue<T> obs) {
        this.validators = validators;
        this.obs = obs;
    }

    private Validator get() {
        return validators.get(obs.getValue());
    }

    @Override
    public Check createCheck() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void add(Check check) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void remove(Check check) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ValidationResult getValidationResult() {
        return get().getValidationResult();
    }

    @Override
    public ObservableValue<ValidationResult> validationResultProperty() {
        var bindingMap = new LinkedHashMap<T, ObservableValue<ValidationResult>>();
        validators.forEach((k, v) -> {
            bindingMap.put(k, v.validationResultProperty());
        });

        var list = new ArrayList<Observable>();
        list.addAll(bindingMap.values());
        list.add(obs);
        Observable[] observables = list.toArray(Observable[]::new);

        return Bindings.createObjectBinding(
                () -> {
                    var v = bindingMap.get(obs.getValue());
                    return v != null ? v.getValue() : null;
                },
                observables);
    }

    @Override
    public ObservableBooleanValue containsErrorsProperty() {
        var bindingMap = new LinkedHashMap<T, ObservableBooleanValue>();
        validators.forEach((k, v) -> {
            bindingMap.put(k, v.containsErrorsProperty());
        });

        var list = new ArrayList<Observable>();
        list.addAll(bindingMap.values());
        list.add(obs);
        Observable[] observables = list.toArray(Observable[]::new);

        return Bindings.createBooleanBinding(
                () -> {
                    var v = bindingMap.get(obs.getValue());
                    return v != null ? v.getValue() : null;
                },
                observables);
    }

    @Override
    public boolean containsErrors() {
        return get().containsErrors();
    }

    @Override
    public boolean validate() {
        return get().validate();
    }

    @Override
    public StringBinding createStringBinding() {
        return createStringBinding("- ", "\n");
    }

    @Override
    public StringBinding createStringBinding(String prefix, String separator) {
        var bindingMap = new LinkedHashMap<T, ObservableValue<String>>();
        validators.forEach((k, v) -> {
            bindingMap.put(k, v.createStringBinding());
        });

        var list = new ArrayList<Observable>();
        list.addAll(bindingMap.values());
        list.add(obs);
        Observable[] observables = list.toArray(Observable[]::new);

        return Bindings.createStringBinding(
                () -> {
                    var v = bindingMap.get(obs.getValue());
                    return v != null ? v.getValue() : null;
                },
                observables);
    }
}
