package io.xpipe.app.util;

import io.xpipe.app.core.AppI18n;

import javafx.beans.binding.StringBinding;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyListProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;

import net.synedra.validatorfx.ValidationResult;

import java.util.function.Predicate;

public interface Validator {

    static Check nonNull(Validator v, ObservableValue<String> name, ObservableValue<?> s) {
        return v.createCheck()
                .dependsOn("val", s)
                .withMethod(c -> {
                    if (c.get("val") == null) {
                        c.error(AppI18n.get(
                                "app.mustNotBeEmpty", name != null ? name.getValue() : AppI18n.get("value")));
                    }
                })
                .immediate();
    }

    static Check nonNullIf(
            Validator v, ObservableValue<String> name, ObservableValue<?> s, ObservableValue<Boolean> checkIf) {
        return v.createCheck()
                .dependsOn("val", s)
                .dependsOn("if", checkIf)
                .withMethod(c -> {
                    if (Boolean.TRUE.equals(c.get("if")) && c.get("val") == null) {
                        c.error(AppI18n.get(
                                "app.mustNotBeEmpty", name != null ? name.getValue() : AppI18n.get("value")));
                    }
                })
                .immediate();
    }

    static Check nonEmpty(Validator v, ObservableValue<String> name, ReadOnlyListProperty<?> s) {
        return v.createCheck()
                .dependsOn("val", s)
                .withMethod(c -> {
                    if (((ObservableList<?>) c.get("val")).size() == 0) {
                        c.error(AppI18n.get(
                                "app.mustNotBeEmpty", name != null ? name.getValue() : AppI18n.get("value")));
                    }
                })
                .immediate();
    }

    static <T> Check create(Validator v, ObservableValue<String> message, ReadOnlyProperty<T> s, Predicate<T> p) {
        return v.createCheck()
                .dependsOn("val", s)
                .withMethod(c -> {
                    if (!p.test(c.get("val"))) {
                        c.error(message.getValue());
                    }
                })
                .immediate();
    }

    Check createCheck();

    /**
     * Add another check to the checker. Changes in the check's validationResultProperty will be reflected in the checker.
     *
     * @param check The check to add.
     */
    void add(Check check);

    /**
     * Removes a check from this validator.
     *
     * @param check The check to remove from this validator.
     */
    void remove(Check check);

    /**
     * Retrieves current validation result
     *
     * @return validation result
     */
    ValidationResult getValidationResult();

    /**
     * Can be used to track validation result changes
     *
     * @return The Validation result property.
     */
    ReadOnlyObjectProperty<ValidationResult> validationResultProperty();

    /**
     * A read-only boolean property indicating whether any of the checks of this validator emitted an error.
     */
    ReadOnlyBooleanProperty containsErrorsProperty();

    boolean containsErrors();

    /**
     * Run all checks (decorating nodes if appropriate)
     *
     * @return true if no errors were found, false otherwise
     */
    boolean validate();

    /**
     * Create a string property that depends on the validation result.
     * Each error message will be displayed on a separate line prefixed with a bullet.
     */
    StringBinding createStringBinding();

    /**
     * Create a string property that depends on the validation result.
     *
     * @param prefix    The string to prefix each validation message with
     * @param separator The string to separate consecutive validation messages with
     */
    StringBinding createStringBinding(String prefix, String separator);

}
