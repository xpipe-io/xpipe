package io.xpipe.app.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import lombok.Getter;
import net.synedra.validatorfx.Decoration;
import net.synedra.validatorfx.DefaultDecoration;
import net.synedra.validatorfx.ValidationMessage;
import net.synedra.validatorfx.ValidationResult;

/** A check represents a check for validity in a form.
 * @author r.lichtenberger@synedra.com
 */
public class Check {

    private Map<String, ObservableValue<? extends Object>> dependencies = new HashMap<>(1);
    private Consumer<Context> checkMethod;
    private ReadOnlyObjectWrapper<ValidationResult> validationResultProperty = new ReadOnlyObjectWrapper<>();
    private ValidationResult nextValidationResult = new ValidationResult();
    @Getter
    private List<Node> targets = new ArrayList<>(1);
    private List<Decoration> decorations = new ArrayList<>();
    private Function<ValidationMessage, Decoration> decorationFactory;
    private ChangeListener<? super Object> dependencyListener;

    public class Context {

        private Context() { }

        /** Get the current value of a dependency.
         * @param <T> The type the value should be casted into
         * @param key The key the dependency has been given
         * @return The current value of the given depency
         */
        @SuppressWarnings("unchecked")
        public <T> T get(String key) {
            return (T) dependencies.get(key).getValue();
        }

        public Iterable<String> keys() {
            return dependencies.keySet();
        }

        /** Emit a warning.
         * @param message The text to be presented to the user as warning message.
         */
        public void warn(String message) {
            nextValidationResult.addWarning(message);
        }

        /** Emit an error.
         * @param message The text to be presented to the user as error message.
         */
        public void error(String message) {
            nextValidationResult.addError(message);
        }
    }

    public Check() {
        validationResultProperty.set(new ValidationResult());
        decorationFactory = DefaultDecoration.getFactory();
        dependencyListener = (obs, oldv, newv) -> recheck();
    }

    public Check withMethod(Consumer<Context> checkMethod) {
        this.checkMethod = checkMethod;
        return this;
    }

    public Check dependsOn(String key, ObservableValue<? extends Object> dependency) {
        dependencies.put(key, dependency);
        return this;
    }

    public Check decorates(Node target) {
        targets.add(target);
        return this;
    }

    public Check decoratingWith(Function<ValidationMessage, Decoration> decorationFactory) {
        this.decorationFactory = decorationFactory;
        return this;
    }

    /** Sets this check to be immediately evaluated if one of its dependencies changes.
     * This method must be called last.
     */
    public Check immediate() {
        for (ObservableValue<? extends Object> dependency : dependencies.values()) {
            dependency.addListener(dependencyListener);
        }
        Platform.runLater(this::recheck);	// to circumvent problems with decoration pane vs. dialog
        return this;
    }

    /** Evaluate all dependencies and apply decorations of this check. You should not normally need to call this method directly. */
    public void recheck() {
        nextValidationResult = new ValidationResult();
        checkMethod.accept(new Context());
        for (Node target : targets) {
            for (Decoration decoration : decorations) {
                decoration.remove(target);
            }
        }
        decorations.clear();
        for (Node target : targets) {
            for (ValidationMessage validationMessage : nextValidationResult.getMessages()) {
                Decoration decoration = decorationFactory.apply(validationMessage);
                decorations.add(decoration);
                decoration.add(target);
            }
        }
        if (!nextValidationResult.getMessages().equals(getValidationResult().getMessages())) {
            validationResultProperty.set(nextValidationResult);
        }
    }

    /** Retrieves current validation result
     * @return validation result
     */
    public ValidationResult getValidationResult() {
        return validationResultProperty.get();
    }

    /** Can be used to track validation result changes
     * @return The Validation result property.
     */
    public ReadOnlyObjectProperty<ValidationResult> validationResultProperty() {
        return validationResultProperty.getReadOnlyProperty();
    }

}
