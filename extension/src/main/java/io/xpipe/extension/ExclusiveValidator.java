package io.xpipe.extension;

import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.value.ObservableValue;
import net.synedra.validatorfx.Check;
import net.synedra.validatorfx.ValidationResult;

import java.util.ArrayList;
import java.util.Map;

public final class ExclusiveValidator<T> implements io.xpipe.extension.Validator {

    private final Map<T, ? extends Validator> validators;
    private final ObservableValue<T> obs;

    public ExclusiveValidator(Map<T, ? extends Validator> validators, ObservableValue<T> obs) {
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
    public ReadOnlyObjectProperty<ValidationResult> validationResultProperty() {
        return get().validationResultProperty();
    }

    @Override
    public ReadOnlyBooleanProperty containsErrorsProperty() {
        return get().containsErrorsProperty();
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
        var list = new ArrayList<Observable>(validators.values().stream().map(Validator::createStringBinding).toList());
        list.add(obs);
        Observable[] observables = list.toArray(Observable[]::new);
        return Bindings.createStringBinding(() -> {
            return get().createStringBinding(prefix, separator).get();
        }, observables);
    }
}
