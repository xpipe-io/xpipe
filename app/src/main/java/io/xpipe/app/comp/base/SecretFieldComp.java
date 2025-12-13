package io.xpipe.app.comp.base;

import atlantafx.base.controls.Popover;
import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.CompStructure;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.platform.ClipboardHelper;
import io.xpipe.app.platform.PlatformThread;
import io.xpipe.core.InPlaceSecretValue;

import javafx.application.Platform;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

import atlantafx.base.layout.InputGroup;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SecretFieldComp extends Comp<SecretFieldComp.Structure> {

    private final Property<InPlaceSecretValue> value;
    private final boolean allowCopy;
    private final List<Comp<?>> additionalButtons = new ArrayList<>();

    public SecretFieldComp(Property<InPlaceSecretValue> value, boolean allowCopy) {
        this.value = value;
        this.allowCopy = allowCopy;
    }

    public static SecretFieldComp ofString(Property<String> s) {
        var prop = new SimpleObjectProperty<>(s.getValue() != null ? InPlaceSecretValue.of(s.getValue()) : null);
        prop.addListener((observable, oldValue, newValue) -> {
            s.setValue(newValue != null ? new String(newValue.getSecret()) : null);
        });
        s.addListener((observableValue, s1, t1) -> {
            prop.set(t1 != null ? InPlaceSecretValue.of(t1) : null);
        });
        return new SecretFieldComp(prop, false);
    }

    public void addButton(Comp<?> button) {
        this.additionalButtons.add(button);
    }

    protected InPlaceSecretValue encrypt(char[] c) {
        return InPlaceSecretValue.of(c);
    }

    @Override
    public Structure createBase() {
        var field = new PasswordField();
        field.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.isControlDown() && e.getCode() == KeyCode.BACK_SPACE) {
                var sel = field.getSelection();
                if (sel.getEnd() > 0) {
                    field.setText(field.getText().substring(sel.getEnd()));
                    e.consume();
                }
            }
        });
        field.setText(value.getValue() != null ? value.getValue().getSecretValue() : null);
        field.textProperty().addListener((c, o, n) -> {
            value.setValue(n != null && n.length() > 0 ? encrypt(n.toCharArray()) : null);
        });

        var capsPopover = new Popover();
        var label = new Label();
        label.textProperty().bind(AppI18n.observable("capslockWarning"));
        label.setGraphic(new FontIcon("mdi2i-information-outline"));
        label.setPadding(new Insets(0, 10, 0, 10));
        capsPopover.setContentNode(label);
        capsPopover.setArrowLocation(Popover.ArrowLocation.BOTTOM_CENTER);
        capsPopover.setDetachable(false);

        value.addListener((c, o, n) -> {
            PlatformThread.runLaterIfNeeded(() -> {
                // Check if control value is the same. Then don't set it as that might cause bugs
                if ((n == null && field.getText().isEmpty())
                        || Objects.equals(field.getText(), n != null ? n.getSecretValue() : null)) {
                    return;
                }

                field.setText(n != null ? n.getSecretValue() : null);

                var capslock = Platform.isKeyLocked(KeyCode.CAPS);
                if (!capslock.orElse(false)) {
                    capsPopover.hide();
                    return;
                }
                if (!capsPopover.isShowing()) {
                    capsPopover.show(field);
                }
            });
        });
        HBox.setHgrow(field, Priority.ALWAYS);

        var copyButton = new ButtonComp(null, new FontIcon("mdi2c-clipboard-multiple-outline"), () -> {
                    ClipboardHelper.copyPassword(value.getValue());
                })
                .grow(false, true)
                .tooltipKey("copyPassword");

        var list = new ArrayList<Comp<?>>();
        list.add(Comp.of(() -> field));
        if (allowCopy) {
            list.add(copyButton);
        }
        list.addAll(additionalButtons);

        var ig = new InputGroupComp(list);
        ig.styleClass("secret-field-comp");
        ig.apply(struc -> {
            struc.get().focusedProperty().addListener((c, o, n) -> {
                if (n) {
                    field.requestFocus();
                }
            });
        });

        return new Structure(ig.createBase().get(), field);
    }

    @AllArgsConstructor
    public static class Structure implements CompStructure<InputGroup> {

        private final InputGroup inputGroup;

        @Getter
        private final TextField field;

        @Override
        public InputGroup get() {
            return inputGroup;
        }
    }
}
