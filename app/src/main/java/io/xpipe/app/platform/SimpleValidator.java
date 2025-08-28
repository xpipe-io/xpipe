package io.xpipe.app.platform;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ChangeListener;

import net.synedra.validatorfx.Severity;
import net.synedra.validatorfx.ValidationMessage;
import net.synedra.validatorfx.ValidationResult;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class SimpleValidator implements Validator {

    private final Map<Check, ChangeListener<ValidationResult>> checks = new LinkedHashMap<>();
    private final ReadOnlyObjectWrapper<ValidationResult> validationResultProperty =
            new ReadOnlyObjectWrapper<>(new ValidationResult());
    private final ReadOnlyBooleanWrapper containsErrorsProperty = new ReadOnlyBooleanWrapper();

    /**
     * Create a check that lives within this checker's domain.
     *
     * @return A check object whose dependsOn, decorates, etc. methods can be called
     */
    public Check createCheck() {
        Check check = new Check();
        add(check);
        return check;
    }

    /**
     * Add another check to the checker. Changes in the check's validationResultProperty will be reflected in the checker.
     *
     * @param check The check to add.
     */
    public void add(Check check) {
        ChangeListener<ValidationResult> listener = (obs, oldv, newv) -> refreshProperties();
        checks.put(check, listener);
        check.validationResultProperty().addListener(listener);
    }

    /**
     * Removes a check from this validator.
     *
     * @param check The check to remove from this validator.
     */
    public void remove(Check check) {
        ChangeListener<ValidationResult> listener = checks.remove(check);
        if (listener != null) {
            check.validationResultProperty().removeListener(listener);
        }
        refreshProperties();
    }

    /**
     * Retrieves current validation result
     *
     * @return validation result
     */
    public ValidationResult getValidationResult() {
        return validationResultProperty.get();
    }

    /**
     * Can be used to track validation result changes
     *
     * @return The Validation result property.
     */
    public ReadOnlyObjectProperty<ValidationResult> validationResultProperty() {
        return validationResultProperty.getReadOnlyProperty();
    }

    /**
     * A read-only boolean property indicating whether any of the checks of this validator emitted an error.
     */
    public ReadOnlyBooleanProperty containsErrorsProperty() {
        return containsErrorsProperty.getReadOnlyProperty();
    }

    public boolean containsErrors() {
        return containsErrorsProperty().get();
    }

    /**
     * Run all checks (decorating nodes if appropriate)
     *
     * @return true if no errors were found, false otherwise
     */
    public boolean validate() {
        for (Check check : checks.keySet()) {
            check.recheck();
        }
        return !containsErrors();
    }

    /**
     * Create a string property that depends on the validation result.
     * Each error message will be displayed on a separate line prefixed with a bullet.
     */
    public StringBinding createStringBinding() {
        return createStringBinding("- ", "\n");
    }

    @Override
    public StringBinding createStringBinding(String prefix, String separator) {
        return Bindings.createStringBinding(
                () -> {
                    StringBuilder str = new StringBuilder();
                    for (ValidationMessage msg : validationResultProperty.get().getMessages()) {
                        if (str.length() > 0) {
                            str.append(separator);
                        }
                        str.append(prefix).append(msg.getText());
                    }
                    return str.toString();
                },
                validationResultProperty);
    }

    @Override
    public Collection<Check> getActiveChecks() {
        return checks.keySet();
    }

    private void refreshProperties() {
        ValidationResult nextResult = new ValidationResult();
        for (Check check : checks.keySet()) {
            nextResult.addAll(check.getValidationResult().getMessages());
        }
        validationResultProperty.set(nextResult);
        boolean hasErrors = false;
        for (ValidationMessage msg : nextResult.getMessages()) {
            hasErrors = hasErrors || msg.getSeverity() == Severity.ERROR;
        }
        containsErrorsProperty.set(hasErrors);
    }
}
