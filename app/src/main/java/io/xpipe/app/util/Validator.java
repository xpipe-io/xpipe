package io.xpipe.app.util;

import io.xpipe.app.core.AppI18n;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.value.ObservableValue;
import net.synedra.validatorfx.Check;
import net.synedra.validatorfx.ValidationResult;
import org.apache.commons.lang3.function.FailableRunnable;

public interface Validator {

    static Check nonNull(Validator v, ObservableValue<String> name, ObservableValue<?> s) {
        return v.createCheck().dependsOn("val", s).withMethod(c -> {            if (c.get("val") == null) {
                c.error(AppI18n.get("app.mustNotBeEmpty", name.getValue()));
            }
        });
    }

    static Check exceptionWrapper(Validator v, ObservableValue<?> s, FailableRunnable<Exception> ex) {
        return v.createCheck().dependsOn("val", s).withMethod(c -> {
            if (c.get("val") == null) {
                try {
                    ex.run();
                } catch (Exception e) {
                    c.error(e.getMessage());
                }
            }
        });
    }

    public Check createCheck();

    /**
     * Add another check to the checker. Changes in the check's validationResultProperty will be reflected in the checker.
     *
     * @param check The check to add.
     */
    public void add(Check check);

    /**
     * Removes a check from this validator.
     *
     * @param check The check to remove from this validator.
     */
    public void remove(Check check);

    /**
     * Retrieves current validation result
     *
     * @return validation result
     */
    public ValidationResult getValidationResult();

    /**
     * Can be used to track validation result changes
     *
     * @return The Validation result property.
     */
    public ReadOnlyObjectProperty<ValidationResult> validationResultProperty();

    /**
     * A read-only boolean property indicating whether any of the checks of this validator emitted an error.
     */
    public ReadOnlyBooleanProperty containsErrorsProperty();

    public boolean containsErrors();

    /**
     * Run all checks (decorating nodes if appropriate)
     *
     * @return true if no errors were found, false otherwise
     */
    public boolean validate();

    /**
     * Create a string property that depends on the validation result.
     * Each error message will be displayed on a separate line prefixed with a bullet.
     *
     * @return
     */
    public StringBinding createStringBinding();

    /**
     * Create a string property that depends on the validation result.
     *
     * @param prefix    The string to prefix each validation message with
     * @param separator The string to separate consecutive validation messages with
     * @return
     */
    public StringBinding createStringBinding(String prefix, String separator);
}
