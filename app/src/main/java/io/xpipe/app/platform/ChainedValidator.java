package io.xpipe.app.platform;

import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableValue;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ChainedValidator implements Validator {

    private final List<Validator> validators;

    public ChainedValidator(List<Validator> validators) {
        this.validators = validators;
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
        var list = new ArrayList<ValidationMessage>();
        for (var val : validators) {
            list.addAll(val.getValidationResult().getMessages());
        }

        var r = new ValidationResult();
        r.add(list);
        return r;
    }

    @Override
    public ObservableValue<ValidationResult> validationResultProperty() {
        var list = new ArrayList<ObservableValue<ValidationResult>>();
        for (var val : validators) {
            list.add(val.validationResultProperty());
        }
        return Bindings.createObjectBinding(
                () -> {
                    for (var r : list) {
                        var val = r.getValue();
                        if (!val.getMessages().isEmpty()) {
                            return val;
                        }
                    }
                    return new ValidationResult();
                },
                list.toArray(Observable[]::new));
    }

    @Override
    public ObservableBooleanValue containsErrorsProperty() {
        var list = new ArrayList<ObservableBooleanValue>();
        for (var val : validators) {
            list.add(val.containsErrorsProperty());
        }
        return Bindings.createBooleanBinding(
                () -> {
                    for (var r : list) {
                        var val = r.getValue();
                        if (val) {
                            return true;
                        }
                    }
                    return false;
                },
                list.toArray(Observable[]::new));
    }

    @Override
    public boolean containsErrors() {
        return validators.stream().anyMatch(Validator::containsErrors);
    }

    @Override
    public boolean validate() {
        var valid = true;
        for (var val : validators) {
            if (!val.validate()) {
                valid = false;
            }
        }

        return valid;
    }

    @Override
    public StringBinding createStringBinding() {
        return createStringBinding("- ", "\n");
    }

    @Override
    public StringBinding createStringBinding(String prefix, String separator) {
        var list = new ArrayList<Observable>(
                validators.stream().map(Validator::createStringBinding).toList());
        Observable[] observables = list.toArray(Observable[]::new);
        return Bindings.createStringBinding(
                () -> {
                    return validators.stream()
                            .map(v -> v.createStringBinding(prefix, separator).get())
                            .collect(Collectors.joining("\n"));
                },
                observables);
    }
}
