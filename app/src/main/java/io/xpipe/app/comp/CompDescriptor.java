package io.xpipe.app.comp;

import io.xpipe.app.comp.base.TooltipHelper;
import io.xpipe.app.core.AppI18n;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.Region;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CompDescriptor {

    ObservableValue<String> name;
    ObservableValue<String> description;
    KeyCombination shortcut;
    FocusTraversal focusTraversal;

    public static enum FocusTraversal {

        DISABLED,
        ENABLED_FOR_ACCESSIBILITY,
        ENABLED;
    }
    
    public void apply(Region r) {
        var accessibleText = getName() != null ? Bindings.createStringBinding(() -> {
            var s = getName().getValue() + "\n\n";
            if (getShortcut() != null) {
                s += AppI18n.get("shortcut") + ": " + getShortcut().getDisplayText();
            }
            return s;
        }, AppI18n.activeLanguage(), getName()) : null;

        var tooltipText = Bindings.createStringBinding(() -> {
            var s = "";
            if (getName() != null) {
                s += getName().getValue() + "\n\n";
            }
            if (getDescription() != null) {
                var desc = getDescription().getValue();
                if (desc != null) {
                    s += desc + "\n\n";
                }
            }
            if (getShortcut() != null) {
                s += AppI18n.get("shortcut") + ": " + getShortcut().getDisplayText();
            }
            return s.strip();
        }, AppI18n.activeLanguage(), getName() != null ? getName() : new ReadOnlyObjectWrapper<>(), getDescription() != null ? getDescription() : new ReadOnlyObjectWrapper<>());

        var tt = TooltipHelper.create(tooltipText);
        Tooltip.install(r, tt);
        if (accessibleText != null) {
            r.accessibleTextProperty().bind(getName());
        }
        if (getDescription() != null) {
            r.accessibleHelpProperty().bind(getDescription());
        }
        if (getFocusTraversal() != null) {
            switch (getFocusTraversal()) {
                case DISABLED -> {
                    r.setFocusTraversable(false);
                }
                case ENABLED_FOR_ACCESSIBILITY -> {
                    r.focusTraversableProperty().bind(Platform.accessibilityActiveProperty());
                }
                case ENABLED -> {
                    r.setFocusTraversable(true);
                }
            }
        }
    }

    public static class CompDescriptorBuilder {

        public CompDescriptorBuilder nameKey(String key) {
            return name(AppI18n.observable(key));
        }
    }
}
