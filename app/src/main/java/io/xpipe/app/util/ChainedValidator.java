package io.xpipe.app.util;

import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;

import net.synedra.validatorfx.ValidationMessage;
import net.synedra.validatorfx.ValidationResult;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class ChainedValidator implements Validator {

    private final List<Validator> validators;
    private final ReadOnlyObjectWrapper<ValidationResult> validationResultProperty =
            new ReadOnlyObjectWrapper<>(new ValidationResult());
    private final ReadOnlyBooleanWrapper containsErrorsProperty = new ReadOnlyBooleanWrapper();

    public ChainedValidator(List<Validator> validators) {
        this.validators = validators;
        validators.forEach(v -> {
            v.containsErrorsProperty().addListener((c, o, n) -> {
                containsErrorsProperty.set(containsErrors());
            });

            v.validationResultProperty().addListener((c, o, n) -> {
                validationResultProperty.set(getValidationResult());
            });
        });
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
        r.addAll(list);
        return r;
    }

    @Override
    public ReadOnlyObjectProperty<ValidationResult> validationResultProperty() {
        return validationResultProperty;
    }

    @Override
    public ReadOnlyBooleanProperty containsErrorsProperty() {
        return containsErrorsProperty;
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

    @Override
    public Collection<Check> getActiveChecks() {
        var all = new ArrayList<Check>();
        for (var val : validators) {
            all.addAll(val.getActiveChecks());
        }
        return all;
    }
}
